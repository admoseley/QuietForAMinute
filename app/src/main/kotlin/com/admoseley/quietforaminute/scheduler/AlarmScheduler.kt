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
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import javax.inject.Singleton

const val EXTRA_SCHEDULE_ID = "extra_schedule_id"
const val EXTRA_DURATION_MINUTES = "extra_duration_minutes"
const val EXTRA_SOURCE = "extra_source"
const val SOURCE_MANUAL = "manual"
const val SOURCE_ALARM = "alarm"

@Singleton
class AlarmScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    /**
     * Schedule exact alarms for each day in the schedule.
     * Returns false if exact alarms are not permitted.
     */
    fun schedule(schedule: Schedule): Boolean {
        if (!schedule.isEnabled) return true
        if (!canScheduleExactAlarms()) {
            Log.w("AlarmScheduler", "Cannot schedule exact alarms — permission not granted")
            return false
        }

        val now = ZonedDateTime.now()
        schedule.days.forEach { day ->
            val triggerTime = nextOccurrence(now, day, schedule.triggerHour, schedule.triggerMinute)
            val pi = buildPendingIntent(schedule.id, day, schedule.durationMinutes)
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime.toInstant().toEpochMilli(),
                pi
            )
        }
        return true
    }

    fun cancel(schedule: Schedule) {
        cancelAllDaysForSchedule(schedule.id)
    }

    /** Clears every weekday slot for this schedule id (covers removed days and DB/UI drift). */
    fun cancelAllDaysForSchedule(scheduleId: Long) {
        DayOfWeek.entries.forEach { day ->
            val pi = PendingIntent.getBroadcast(
                context,
                requestCode(scheduleId, day),
                Intent(context, AlarmReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            ) ?: return@forEach
            alarmManager.cancel(pi)
        }
    }

    private fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true // API < 31 does not require explicit permission
        }
    }

    private fun buildPendingIntent(
        scheduleId: Long,
        day: DayOfWeek,
        durationMinutes: Int
    ): PendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode(scheduleId, day),
        Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_SCHEDULE_ID, scheduleId)
            putExtra(EXTRA_DURATION_MINUTES, durationMinutes)
            putExtra(EXTRA_SOURCE, SOURCE_ALARM)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun nextOccurrence(
        from: ZonedDateTime,
        dayOfWeek: DayOfWeek,
        hour: Int,
        minute: Int
    ): ZonedDateTime {
        var candidate = from.with(TemporalAdjusters.nextOrSame(dayOfWeek))
            .withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!candidate.isAfter(from)) {
            candidate = candidate.plusWeeks(1)
        }
        return candidate
    }

    // Unique requestCode per (scheduleId x dayOfWeek)
    private fun requestCode(scheduleId: Long, day: DayOfWeek): Int =
        (scheduleId * 7 + day.value).toInt()
}
