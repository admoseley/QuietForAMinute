package com.admoseley.quietforaminute.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.admoseley.quietforaminute.data.repository.ScheduleRepository
import com.admoseley.quietforaminute.scheduler.EXTRA_DND_ENABLED
import com.admoseley.quietforaminute.scheduler.EXTRA_DURATION_MINUTES
import com.admoseley.quietforaminute.scheduler.EXTRA_SCHEDULE_ID
import com.admoseley.quietforaminute.scheduler.EXTRA_SOURCE
import com.admoseley.quietforaminute.scheduler.EXTRA_STREAM_TYPE
import com.admoseley.quietforaminute.scheduler.SOURCE_ALARM
import com.admoseley.quietforaminute.service.MuteTimerService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Receives the exact alarm for one (schedule × weekday) slot.
 *
 * Two jobs, in this order:
 *  1. Re-arm the schedule for its next occurrence *immediately*. Alarms set with
 *     `setExactAndAllowWhileIdle` are one-shot. Previously the re-arm happened only when the
 *     countdown finished, so a killed process, a reboot mid-countdown, or a manual mute replacing
 *     the scheduled one meant the schedule silently never fired again.
 *  2. Start [MuteTimerService] to mute and count down.
 *
 * Delivery of an exact alarm grants a short foreground-service start exemption on Android 12+,
 * which is what allows the `startForegroundService` call from this background context.
 */
@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var scheduleRepository: ScheduleRepository

    override fun onReceive(context: Context, intent: Intent) {
        val scheduleId = intent.getLongExtra(EXTRA_SCHEDULE_ID, -1L)
        val durationMinutes = intent.getIntExtra(EXTRA_DURATION_MINUTES, 0)
        val dndEnabled = intent.getBooleanExtra(EXTRA_DND_ENABLED, false)
        Log.d(TAG, "Alarm fired: schedule=$scheduleId duration=$durationMinutes dnd=$dndEnabled")

        if (scheduleId >= 0) {
            // goAsync() keeps the receiver (and process) alive until finish() is called, giving
            // the database read + AlarmManager calls time to complete (limit is ~10 s).
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                try {
                    val schedule = scheduleRepository.getById(scheduleId)
                    if (schedule != null && schedule.isEnabled) {
                        // save() cancels every weekday slot and recomputes the next occurrence of
                        // each, so the slot that just fired lands on next week.
                        scheduleRepository.save(schedule)
                    }
                } catch (t: Throwable) {
                    Log.e(TAG, "Failed to re-arm schedule $scheduleId", t)
                } finally {
                    pendingResult.finish()
                }
            }
        }

        val serviceIntent = Intent(context, MuteTimerService::class.java).apply {
            putExtra(EXTRA_SCHEDULE_ID, scheduleId)
            putExtra(EXTRA_DURATION_MINUTES, durationMinutes)
            putExtra(EXTRA_SOURCE, SOURCE_ALARM)
            putExtra(EXTRA_STREAM_TYPE, AudioManager.STREAM_MUSIC)
            putExtra(EXTRA_DND_ENABLED, dndEnabled)
        }
        try {
            ContextCompat.startForegroundService(context, serviceIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to start MuteTimerService from alarm", e)
        }
    }

    private companion object {
        const val TAG = "AlarmReceiver"
    }
}
