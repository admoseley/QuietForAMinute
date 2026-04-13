package com.admoseley.quietforaminute.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.app.NotificationManagerCompat
import android.util.Log
import android.widget.Toast
import com.admoseley.quietforaminute.MainActivity
import com.admoseley.quietforaminute.QuietApplication.Companion.CHANNEL_TIMER
import com.admoseley.quietforaminute.R
import com.admoseley.quietforaminute.audio.ChimePlayer
import com.admoseley.quietforaminute.data.datastore.PreferencesRepository
import com.admoseley.quietforaminute.data.repository.ScheduleRepository
import com.admoseley.quietforaminute.scheduler.EXTRA_DURATION_MINUTES
import com.admoseley.quietforaminute.scheduler.EXTRA_SCHEDULE_ID
import com.admoseley.quietforaminute.scheduler.EXTRA_SOURCE
import com.admoseley.quietforaminute.scheduler.SOURCE_ALARM
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
class MuteTimerService : Service() {

    @Inject lateinit var prefsRepository: PreferencesRepository
    @Inject lateinit var scheduleRepository: ScheduleRepository
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
        val scheduleId = intent?.getLongExtra(EXTRA_SCHEDULE_ID, -1L) ?: -1L
        val source = intent?.getStringExtra(EXTRA_SOURCE) ?: ""

        if (durationMinutes <= 0) {
            stopSelf()
            return START_NOT_STICKY
        }

        ServiceCompat.startForeground(
            this,
            NOTIF_ID,
            buildTimerNotification(durationMinutes, durationMinutes),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )

        // Cancel any existing countdown (e.g. if a new scheduled mute fires while one is active)
        countdownJob?.cancel()
        countdownJob = serviceScope.launch {
            runCountdown(durationMinutes, scheduleId, source)
        }

        return START_NOT_STICKY
    }

    private suspend fun runCountdown(totalMinutes: Int, scheduleId: Long, source: String) {
        Log.d("MuteTimerService", "runCountdown started: total=$totalMinutes, source=$source")
        
        if (source == SOURCE_ALARM) {
            muteMusicForScheduledAlarm()
        }

        val durationMs = totalMinutes.toLong() * 60_000L
        val startTime = SystemClock.elapsedRealtime()
        val endTime = startTime + durationMs

        while (SystemClock.elapsedRealtime() < endTime) {
            val now = SystemClock.elapsedRealtime()
            val remainingMs = endTime - now
            val remainingMinutes = ((remainingMs + 59_999L) / 60_000L).toInt().coerceAtLeast(1)
            
            Log.d("MuteTimerService", "Timer running: remainingMs=$remainingMs")
            updateTimerNotification(totalMinutes, remainingMinutes)
            
            // Wait up to 10 seconds or until finished
            delay(minOf(remainingMs, 10_000L))
        }

        Log.d("MuteTimerService", "Countdown complete. Restoring volume...")
        restoreVolume()

        // Re-schedule next occurrence for alarms triggered by a schedule
        if (source == SOURCE_ALARM && scheduleId >= 0) {
            val schedule = scheduleRepository.getById(scheduleId)
            if (schedule != null && schedule.isEnabled) {
                scheduleRepository.save(schedule)
            }
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * Scheduled alarms only adjust [AudioManager.STREAM_MUSIC], matching [restoreVolume].
     * [VolumeReceiver] also reacts to [AudioManager.STREAM_RING]; manual ring mutes still use
     * the same timer but we only restore music from preferences (see Settings).
     */
    private fun muteMusicForScheduledAlarm() {
        OverlayServiceBridge.suppressCount.set(1)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
    }

    private suspend fun restoreVolume() {
        Log.d("MuteTimerService", "Executing restoreVolume()")
        val defaultVolume = prefsRepository.defaultVolume.first()
        val chimeEnabled = prefsRepository.chimeOnRestore.first()

        // Suppress the VolumeReceiver
        OverlayServiceBridge.suppressCount.set(1)

        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val safeVolume = defaultVolume.coerceIn(0, maxVolume)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, safeVolume, 0)
        Log.d("MuteTimerService", "Volume restored to $safeVolume")

        // Show Toast notification on main thread
        withContext(Dispatchers.Main) {
            Toast.makeText(this@MuteTimerService, "System volume has been restored", Toast.LENGTH_SHORT).show()
        }

        if (chimeEnabled) {
            val chimeUri = prefsRepository.restoreChimeUri.first()
            delay(200)
            chimePlayer.playChime(chimeUri)
            // Keep service alive slightly longer to ensure chime starts
            delay(1000)
        }
    }

    private fun buildTimerNotification(totalMinutes: Int, remainingMinutes: Int): Notification {
        val tap = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_TIMER)
            .setSmallIcon(R.drawable.ic_volume_monitor)
            .setContentTitle("Phone muted")
            .setContentText(formatRemaining(remainingMinutes))
            .setOngoing(true)
            .setContentIntent(tap)
            .build()
    }

    private fun updateTimerNotification(totalMinutes: Int, remainingMinutes: Int) {
        val notification = buildTimerNotification(totalMinutes, remainingMinutes)
        try {
            NotificationManagerCompat.from(this).notify(NOTIF_ID, notification)
        } catch (_: SecurityException) { /* no permission */ }
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
        const val NOTIF_ID = 1002
    }
}

/** In-process counter so MuteTimerService can suppress N volume-change broadcasts after restoring. */
object OverlayServiceBridge {
    val suppressCount = java.util.concurrent.atomic.AtomicInteger(0)
}
