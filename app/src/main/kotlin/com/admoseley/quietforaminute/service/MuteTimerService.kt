package com.admoseley.quietforaminute.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import com.admoseley.quietforaminute.MainActivity
import com.admoseley.quietforaminute.QuietApplication.Companion.CHANNEL_TIMER
import com.admoseley.quietforaminute.R
import com.admoseley.quietforaminute.audio.ChimePlayer
import com.admoseley.quietforaminute.data.datastore.PreferencesRepository
import com.admoseley.quietforaminute.scheduler.EXTRA_DURATION_MINUTES
import com.admoseley.quietforaminute.scheduler.EXTRA_RESTORE_VOLUME
import com.admoseley.quietforaminute.scheduler.EXTRA_SOURCE
import com.admoseley.quietforaminute.scheduler.EXTRA_STREAM_TYPE
import com.admoseley.quietforaminute.scheduler.SOURCE_ALARM
import com.admoseley.quietforaminute.scheduler.SOURCE_MANUAL
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Foreground service that owns a single mute countdown and restores volume when it ends.
 *
 * Started by:
 *  - [OverlayService] after the user picks a duration in the popup ([SOURCE_MANUAL]). The user
 *    has already muted the stream themselves; we only count down and restore.
 *  - [com.admoseley.quietforaminute.receiver.AlarmReceiver] for a scheduled mute
 *    ([SOURCE_ALARM]). We mute the music stream ourselves, then count down and restore.
 *
 * Only one countdown runs at a time. A new start intent cancels the previous countdown *without*
 * restoring, and the new duration takes over (documented behaviour, see INSTRUCTIONS.md §3.5).
 *
 * Re-arming the next occurrence of a schedule is NOT done here any more — it happens in
 * `AlarmReceiver` the moment the alarm fires, so a killed process or a replaced countdown can no
 * longer silently stop a schedule from repeating.
 *
 * Known limitation: the countdown lives in process memory. If Android kills the process (low
 * memory, aggressive OEM battery management, user force-stop) the volume is never restored. A more
 * robust design would persist the end time and register a backup `AlarmManager` alarm that
 * performs the restore even when this service is gone.
 */
@AndroidEntryPoint
class MuteTimerService : Service() {

    @Inject lateinit var prefsRepository: PreferencesRepository
    @Inject lateinit var chimePlayer: ChimePlayer

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var countdownJob: Job? = null
    private lateinit var audioManager: AudioManager

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(AudioManager::class.java)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val durationMinutes = intent?.getIntExtra(EXTRA_DURATION_MINUTES, 0) ?: 0
        val source = intent?.getStringExtra(EXTRA_SOURCE) ?: SOURCE_MANUAL
        val restoreVolume = intent?.getIntExtra(EXTRA_RESTORE_VOLUME, -1) ?: -1
        val streamType = intent?.getIntExtra(EXTRA_STREAM_TYPE, AudioManager.STREAM_MUSIC)
            ?: AudioManager.STREAM_MUSIC

        // Promote to foreground FIRST. We were started with startForegroundService(), and the
        // system throws ForegroundServiceDidNotStartInTimeException if startForeground() is not
        // called promptly. Calling stopSelf() before startForeground() does not reliably avoid
        // that on every OS version, so always go foreground, then bail out if the input is bad.
        ServiceCompat.startForeground(
            this,
            NOTIF_ID,
            buildTimerNotification(durationMinutes.coerceAtLeast(1)),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )

        if (durationMinutes <= 0) {
            Log.w(TAG, "Ignoring start with duration=$durationMinutes")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        // Cancel any existing countdown (e.g. a scheduled mute fires while a manual one is active).
        countdownJob?.cancel()
        countdownJob = serviceScope.launch {
            runCountdown(durationMinutes, source, restoreVolume, streamType)
        }

        // NOT_STICKY on purpose: if the system restarts us with a null intent we have no idea what
        // to count down, so a restart would only produce a stray notification.
        return START_NOT_STICKY
    }

    private suspend fun runCountdown(
        totalMinutes: Int,
        source: String,
        manualRestoreVolume: Int,
        streamType: Int
    ) {
        Log.d(TAG, "runCountdown: total=$totalMinutes source=$source stream=$streamType")

        if (source == SOURCE_ALARM) muteForScheduledAlarm(streamType)

        // elapsedRealtime is immune to wall-clock changes (NTP sync, time zone edits, manual
        // adjustment), so the countdown can neither be shortened nor extended by them.
        val endTime = SystemClock.elapsedRealtime() + totalMinutes.toLong() * 60_000L

        while (SystemClock.elapsedRealtime() < endTime) {
            val remainingMs = endTime - SystemClock.elapsedRealtime()
            val remainingMinutes = ((remainingMs + 59_999L) / 60_000L).toInt().coerceAtLeast(1)
            updateTimerNotification(remainingMinutes)
            // Wake up every 10 s to refresh the notification (or sooner if the timer is about to end).
            delay(minOf(remainingMs, 10_000L))
        }

        Log.d(TAG, "Countdown complete, restoring stream $streamType")
        restoreVolume(streamType, manualRestoreVolume)

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * Scheduled mutes silence the stream themselves. The programmatic-change window MUST be
     * opened first, otherwise the resulting VOLUME_CHANGED(0) broadcast would show the popup.
     */
    private fun muteForScheduledAlarm(streamType: Int) {
        OverlayServiceBridge.markProgrammaticChange()
        try {
            audioManager.setStreamVolume(streamType, 0, 0)
        } catch (e: SecurityException) {
            // Ring-stream changes can be refused while Do Not Disturb policy is active.
            Log.w(TAG, "Unable to mute stream $streamType", e)
        }
    }

    /**
     * Restores the stream that was muted.
     *
     * [manualRestoreVolume] comes from the popup slider and is already in the target stream's
     * index units. The default from Settings is stored in STREAM_MUSIC units, so it is scaled
     * proportionally when the muted stream was the ringer (whose max is usually lower).
     */
    private suspend fun restoreVolume(streamType: Int, manualRestoreVolume: Int) {
        val targetMax = audioManager.getStreamMaxVolume(streamType)
        val volume = if (manualRestoreVolume >= 0) {
            manualRestoreVolume
        } else {
            val default = prefsRepository.defaultVolume.first()
            scaleFromMusicUnits(default, streamType, targetMax)
        }.coerceIn(0, targetMax)
        val chimeEnabled = prefsRepository.chimeOnRestore.first()

        // Open the window BEFORE touching the volume so the broadcasts land inside it.
        OverlayServiceBridge.markProgrammaticChange()
        try {
            // If the user muted with the volume-panel icon the stream carries a mute *flag* and
            // the index alone will not bring sound back; clear the flag first (no-op otherwise).
            audioManager.adjustStreamVolume(streamType, AudioManager.ADJUST_UNMUTE, 0)
            audioManager.setStreamVolume(streamType, volume, 0)
            Log.d(TAG, "Stream $streamType restored to $volume/$targetMax")
        } catch (e: SecurityException) {
            // Raising the ringer out of silent can be blocked by Do Not Disturb policy.
            Log.w(TAG, "Unable to restore stream $streamType", e)
        }

        Toast.makeText(this, "System volume has been restored", Toast.LENGTH_SHORT).show()

        if (chimeEnabled) {
            val chimeUri = prefsRepository.restoreChimeUri.first()
            delay(200) // let the new volume take effect before the chime starts
            chimePlayer.playChime(chimeUri)
            delay(1_000) // keep the service alive long enough for the chime to play
        }
    }

    private fun scaleFromMusicUnits(musicUnits: Int, streamType: Int, targetMax: Int): Int {
        if (streamType == AudioManager.STREAM_MUSIC) return musicUnits
        val musicMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        return (musicUnits.toFloat() / musicMax * targetMax).roundToInt()
    }

    private fun buildTimerNotification(remainingMinutes: Int): Notification {
        val tap = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_TIMER)
            .setSmallIcon(R.drawable.ic_volume_monitor)
            .setContentTitle("Phone muted")
            .setContentText(formatRemaining(remainingMinutes))
            .setOngoing(true)
            .setOnlyAlertOnce(true) // refreshing the text every 10 s must not re-alert
            .setContentIntent(tap)
            .build()
    }

    private fun updateTimerNotification(remainingMinutes: Int) {
        try {
            NotificationManagerCompat.from(this).notify(NOTIF_ID, buildTimerNotification(remainingMinutes))
        } catch (_: SecurityException) { /* POST_NOTIFICATIONS not granted */ }
    }

    private fun formatRemaining(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h > 0 && m > 0 -> "Restoring volume in ${h}h ${m}m"
            h > 0 -> "Restoring volume in ${h}h"
            else -> "Restoring volume in ${m}m"
        }
    }

    override fun onDestroy() {
        countdownJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "MuteTimerService"
        const val NOTIF_ID = 1002
    }
}
