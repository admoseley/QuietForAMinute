package com.admoseley.quietforaminute.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.admoseley.quietforaminute.domain.model.Schedule
import com.admoseley.quietforaminute.receiver.AlarmReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.DayOfWeek
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

// Intent extras shared by AlarmScheduler, AlarmReceiver, OverlayService and MuteTimerService.
const val EXTRA_SCHEDULE_ID = "extra_schedule_id"
const val EXTRA_DURATION_MINUTES = "extra_duration_minutes"
const val EXTRA_SOURCE = "extra_source"
const val EXTRA_RESTORE_VOLUME = "extra_restore_volume"
/** Which AudioManager stream was muted (and therefore which one to restore). */
const val EXTRA_STREAM_TYPE = "extra_stream_type"
/** Whether to also turn Do Not Disturb on for the duration of this mute (issue #45). */
const val EXTRA_DND_ENABLED = "extra_dnd_enabled"
const val SOURCE_MANUAL = "manual"
const val SOURCE_ALARM = "alarm"

/**
 * Thin wrapper over [AlarmManager]. One exact, one-shot alarm per (schedule × weekday).
 *
 * Alarms are one-shot on purpose: `setRepeating` is inexact since API 19 and cannot express
 * "every Monday". Each alarm is re-armed for the following week by [AlarmReceiver] when it fires.
 */
@Singleton
class AlarmScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    /**
     * Schedule exact alarms for each day in the schedule.
     * Returns false if exact alarms are not permitted (nothing is armed in that case).
     */
    fun schedule(schedule: Schedule): Boolean {
        if (!schedule.isEnabled) return true
        if (!canScheduleExactAlarms()) {
            Log.w(TAG, "Cannot schedule exact alarms — permission not granted")
            return false
        }

        val now = ZonedDateTime.now()
        schedule.days.forEach { day ->
            val triggerTime = AlarmTiming.nextOccurrence(now, day, schedule.triggerHour, schedule.triggerMinute)
            val pi = buildPendingIntent(schedule.id, day, schedule.durationMinutes, schedule.dndEnabled)
            // RTC_WAKEUP + allowWhileIdle: fires at wall-clock time even in Doze.
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime.toInstant().toEpochMilli(),
                pi
            )
            Log.d(TAG, "Armed schedule ${schedule.id} for $day at $triggerTime")
        }
        return true
    }

    fun cancel(schedule: Schedule) {
        cancelAllDaysForSchedule(schedule.id)
    }

    /** Clears every weekday slot for this schedule id (covers removed days and DB/UI drift). */
    fun cancelAllDaysForSchedule(scheduleId: Long) {
        DayOfWeek.entries.forEach { day ->
            // FLAG_NO_CREATE returns null when no alarm was ever registered for this slot.
            // Extras are ignored by PendingIntent matching (Intent.filterEquals), so a bare
            // Intent with the same component + requestCode is enough to find it.
            val pi = PendingIntent.getBroadcast(
                context,
                AlarmTiming.requestCode(scheduleId, day),
                Intent(context, AlarmReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            ) ?: return@forEach
            alarmManager.cancel(pi)
            pi.cancel()
        }
    }

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true // API < 31 does not require explicit permission
        }
    }

    private fun buildPendingIntent(
        scheduleId: Long,
        day: DayOfWeek,
        durationMinutes: Int,
        dndEnabled: Boolean
    ): PendingIntent = PendingIntent.getBroadcast(
        context,
        AlarmTiming.requestCode(scheduleId, day),
        Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_SCHEDULE_ID, scheduleId)
            putExtra(EXTRA_DURATION_MINUTES, durationMinutes)
            putExtra(EXTRA_SOURCE, SOURCE_ALARM)
            putExtra(EXTRA_DND_ENABLED, dndEnabled)
        },
        // UPDATE_CURRENT so an edited duration or DND choice replaces the extras of the existing
        // PendingIntent (extras are ignored by PendingIntent matching, so the flag is what carries
        // an edit through to an already-armed alarm).
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private companion object {
        const val TAG = "AlarmScheduler"
    }
}
