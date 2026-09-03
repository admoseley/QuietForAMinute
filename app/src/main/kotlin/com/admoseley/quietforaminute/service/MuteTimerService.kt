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
import com.admoseley.quietforaminute.scheduler.BackupRestoreScheduler
import com.admoseley.quietforaminute.scheduler.EXTRA_DND_ENABLED
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
 * Reliability (issue #8): the countdown itself still lives in process memory — if Android kills
 * this process (low memory, an OEM battery manager, force-stop) mid-countdown, the delay loop
 * simply stops. Two things now cover that instead of leaving the user muted indefinitely:
 *  1. [BackupRestoreScheduler] arms an exact `AlarmManager` alarm for shortly after the expected
 *     end time. AlarmManager alarms are a separate OS subsystem from this service's process, so
 *     they fire even if this process is long gone. Normally this alarm is cancelled below the
 *     moment the primary restore succeeds, so it never fires under normal operation.
 *  2. The restore target is persisted to DataStore ([PreferencesRepository.savePendingRestore]).
 *     AlarmManager alarms do **not** survive a reboot, so if the device itself restarts
 *     mid-countdown, [com.admoseley.quietforaminute.receiver.BootReceiver] reads this record back
 *     and either restores immediately (if the end time already passed) or re-arms the backup
 *     alarm for whatever time is left.
 */
@AndroidEntryPoint
class MuteTimerService : Service() {

    @Inject lateinit var prefsRepository: PreferencesRepository
    @Inject lateinit var chimePlayer: ChimePlayer
    @Inject lateinit var volumeRestorer: VolumeRestorer
    @Inject lateinit var backupRestoreScheduler: BackupRestoreScheduler
    @Inject lateinit var dndController: DndController

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var countdownJob: Job? = null
    private lateinit var audioManager: AudioManager

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(AudioManager::class.java)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL_TIMER) {
            cancelTimer()
            return START_NOT_STICKY
        }

        val durationMinutes = intent?.getIntExtra(EXTRA_DURATION_MINUTES, 0) ?: 0
        val source = intent?.getStringExtra(EXTRA_SOURCE) ?: SOURCE_MANUAL
        val restoreVolume = intent?.getIntExtra(EXTRA_RESTORE_VOLUME, -1) ?: -1
        val streamType = intent?.getIntExtra(EXTRA_STREAM_TYPE, AudioManager.STREAM_MUSIC)
            ?: AudioManager.STREAM_MUSIC
        val dndRequested = intent?.getBooleanExtra(EXTRA_DND_ENABLED, false) ?: false

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
        // This also implicitly retires the previous countdown's backup alarm, since the one armed
        // below immediately replaces it (same fixed requestCode — see BackupRestoreScheduler).
        countdownJob?.cancel()
        countdownJob = serviceScope.launch {
            runCountdown(durationMinutes, source, restoreVolume, streamType, dndRequested)
        }

        // NOT_STICKY on purpose: if the system restarts us with a null intent we have no idea what
        // to count down, so a restart would only produce a stray notification.
        return START_NOT_STICKY
    }

    private suspend fun runCountdown(
        totalMinutes: Int,
        source: String,
        manualRestoreVolume: Int,
        streamType: Int,
        dndRequested: Boolean
    ) {
        Log.d(TAG, "runCountdown: total=$totalMinutes source=$source stream=$streamType dnd=$dndRequested")

        if (source == SOURCE_ALARM) muteForScheduledAlarm(streamType)

        // enable() reports whether *we* changed DND, which is not the same as "DND is on": if the
        // user already had it on, it stays on and is left for them to turn off. Only what we
        // switched on do we switch back off.
        val dndEnabledByUs = if (dndRequested) dndController.enable() else false

        // DND silences interruptions but not media playback, so a mute triggered by the ring
        // stream would leave a video playing out loud. Silence media too in that case, and record
        // it so the restore paths know they owe bringing it back.
        val mediaMuted = if (dndRequested && streamType != AudioManager.STREAM_MUSIC) {
            muteMediaAlongsideDnd()
        } else {
            false
        }

        // elapsedRealtime is immune to wall-clock changes (NTP sync, time zone edits, manual
        // adjustment), so the countdown can neither be shortened nor extended by them — used for
        // the in-process loop below. The backup alarm and persisted record need a value that
        // means something after a process death or reboot, so they use a wall-clock epoch instead
        // computed from the same instant; a wall-clock jump mid-countdown could drift the two
        // apart, the same trade-off AlarmScheduler already accepts for scheduled mutes.
        val startElapsed = SystemClock.elapsedRealtime()
        val endTime = startElapsed + totalMinutes.toLong() * 60_000L
        val endEpochMillis = System.currentTimeMillis() + totalMinutes.toLong() * 60_000L

        prefsRepository.savePendingRestore(
            endEpochMillis, streamType, manualRestoreVolume, dndEnabledByUs, mediaMuted
        )
        backupRestoreScheduler.schedule(
            endEpochMillis + BackupRestoreScheduler.TRIGGER_BUFFER_MS,
            streamType,
            manualRestoreVolume,
            clearDnd = dndEnabledByUs,
            restoreMedia = mediaMuted
        )

        while (SystemClock.elapsedRealtime() < endTime) {
            val remainingMs = endTime - SystemClock.elapsedRealtime()
            val remainingMinutes = ((remainingMs + 59_999L) / 60_000L).toInt().coerceAtLeast(1)
            updateTimerNotification(remainingMinutes)
            // Wake up every 10 s to refresh the notification (or sooner if the timer is about to end).
            delay(minOf(remainingMs, 10_000L))
        }

        Log.d(TAG, "Countdown complete, restoring stream $streamType")
        restoreVolume(streamType, manualRestoreVolume, dndEnabledByUs, mediaMuted)

        // The primary restore just ran, so the backup alarm and its persisted record are no
        // longer needed — clearing them here is what keeps the backup alarm from ever actually
        // firing on the normal, nothing-went-wrong path.
        backupRestoreScheduler.cancel()
        prefsRepository.clearPendingRestore()

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * The user restored volume by hand mid-countdown, so the timer is moot (issue #42). Tears
     * everything down without touching the volume — they already set it where they want it —
     * and acknowledges it the same way a normal restore does, with a Toast and the restore chime.
     *
     * Do Not Disturb is the exception to "don't touch anything" (issue #45): the volume key the
     * user pressed cannot clear DND, so leaving it on would strand them silenced with no timer
     * left to end it. Same for media if we muted it alongside DND. Both are read from the persisted
     * record *before* it is cleared, since that record is the only memory of what we changed.
     *
     * Deliberately still calls startForeground first: this service may or may not already be in
     * the foreground depending on how it was started, and the "did not start in time" exception
     * doesn't care that we're about to stop. Same defensive pattern as the bad-input path above.
     */
    private fun cancelTimer() {
        Log.d(TAG, "Timer cancelled by a manual volume restore")
        ServiceCompat.startForeground(
            this,
            NOTIF_ID,
            buildTimerNotification(1),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )
        countdownJob?.cancel()

        serviceScope.launch {
            val pending = prefsRepository.pendingRestore.first()
            backupRestoreScheduler.cancel()
            prefsRepository.clearPendingRestore()

            if (pending?.dndEnabled == true) dndController.disable()
            if (pending?.mediaMuted == true) {
                // -1 = the configured default: we silenced media ourselves, so there is no
                // user-chosen per-mute level for it to go back to.
                volumeRestorer.restore(AudioManager.STREAM_MUSIC, manualRestoreVolume = -1)
            }

            Toast.makeText(
                this@MuteTimerService,
                getString(R.string.toast_manual_restore_timer_cancelled),
                Toast.LENGTH_SHORT
            ).show()

            if (prefsRepository.chimeOnRestore.first()) {
                chimePlayer.playChime(prefsRepository.restoreChimeUri.first())
                delay(1_000) // keep the service alive long enough for the chime to play
            }

            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
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
     * Silences media on top of the stream the user already muted, so "Do Not Disturb" really means
     * quiet rather than just uninterrupted. Returns whether media actually needed muting — already
     * being at zero means it's the user's own setting and not ours to raise again later.
     */
    private fun muteMediaAlongsideDnd(): Boolean {
        if (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) {
            Log.d(TAG, "Media already silent — not taking ownership of restoring it")
            return false
        }
        OverlayServiceBridge.markProgrammaticChange()
        return try {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
            Log.d(TAG, "Media muted alongside DND")
            true
        } catch (e: SecurityException) {
            Log.w(TAG, "Unable to mute media alongside DND", e)
            false
        }
    }

    /** Volume-setting itself is shared with the backup/boot restore paths — see [VolumeRestorer]. */
    private suspend fun restoreVolume(
        streamType: Int,
        manualRestoreVolume: Int,
        clearDnd: Boolean,
        restoreMedia: Boolean
    ) {
        Log.d(TAG, "Executing restoreVolume()")
        val chimeEnabled = prefsRepository.chimeOnRestore.first()

        volumeRestorer.restore(streamType, manualRestoreVolume, clearDnd, restoreMedia)

        Toast.makeText(this, getString(R.string.toast_volume_restored), Toast.LENGTH_SHORT).show()

        if (chimeEnabled) {
            val chimeUri = prefsRepository.restoreChimeUri.first()
            delay(200) // let the new volume take effect before the chime starts
            chimePlayer.playChime(chimeUri)
            delay(1_000) // keep the service alive long enough for the chime to play
        }
    }

    private fun buildTimerNotification(remainingMinutes: Int): Notification {
        val tap = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_TIMER)
            .setSmallIcon(R.drawable.ic_volume_monitor)
            .setContentTitle(getString(R.string.notif_timer_title))
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
            h > 0 && m > 0 -> getString(R.string.notif_timer_text_hours_minutes, h, m)
            h > 0 -> getString(R.string.notif_timer_text_hours, h)
            else -> getString(R.string.notif_timer_text_minutes, m)
        }
    }

    override fun onDestroy() {
        // Deliberately does NOT cancel the backup alarm or clear the persisted record here: if
        // onDestroy runs because the countdown finished normally, restoreVolume() already cleared
        // both above. If onDestroy runs because the system is killing this service outright, the
        // backup alarm and DataStore record are exactly what needs to survive that.
        countdownJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIF_ID = 1002
        private const val TAG = "MuteTimerService"

        /** Sent by [OverlayService] when the user restores volume themselves mid-countdown. */
        const val ACTION_CANCEL_TIMER = "com.admoseley.quietforaminute.action.CANCEL_TIMER"
    }
}
