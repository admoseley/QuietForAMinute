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

// Intent extras shared by AlarmScheduler, AlarmReceiver, OverlayService and MuteTimerService.
const val EXTRA_SCHEDULE_ID = "extra_schedule_id"
const val EXTRA_DURATION_MINUTES = "extra_duration_minutes"
const val EXTRA_SOURCE = "extra_source"
const val EXTRA_RESTORE_VOLUME = "extra_restore_volume"
/** Which AudioManager stream was muted (and therefore which one to restore). */
const val EXTRA_STREAM_TYPE = "extra_stream_type"
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
            val triggerTime = nextOccurrence(now, day, schedule.triggerHour, schedule.triggerMinute)
            val pi = buildPendingIntent(schedule.id, day, schedule.durationMinutes)
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
                requestCode(scheduleId, day),
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
        durationMinutes: Int
    ): PendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode(scheduleId, day),
        Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_SCHEDULE_ID, scheduleId)
            putExtra(EXTRA_DURATION_MINUTES, durationMinutes)
            putExtra(EXTRA_SOURCE, SOURCE_ALARM)
        },
        // UPDATE_CURRENT so an edited duration replaces the extras of the existing PendingIntent.
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    /**
     * Next wall-clock occurrence of [dayOfWeek] at [hour]:[minute], strictly after [from].
     * `withHour`/`withMinute` on a ZonedDateTime resolve DST gaps/overlaps for us.
     */
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

    /**
     * Unique requestCode per (scheduleId × dayOfWeek). `day.value` is 1..7, so schedule N owns
     * codes 7N+1..7N+7 and schedule N+1 starts at 7N+8 — no overlap. Room ids are small, so the
     * Long→Int truncation is safe in practice.
     */
    private fun requestCode(scheduleId: Long, day: DayOfWeek): Int =
        (scheduleId * 7 + day.value).toInt()

    private companion object {
        const val TAG = "AlarmScheduler"
    }
}
