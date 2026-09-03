package com.admoseley.quietforaminute.scheduler

import java.time.DayOfWeek
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

/**
 * Pure date/id math used by [AlarmScheduler], pulled out so it's testable without an
 * `AlarmManager`/`Context` in the loop.
 */
object AlarmTiming {

    /**
     * Next wall-clock occurrence of [dayOfWeek] at [hour]:[minute], strictly after [from].
     * `withHour`/`withMinute` on a ZonedDateTime resolve DST gaps/overlaps for us.
     */
    fun nextOccurrence(
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
    fun requestCode(scheduleId: Long, day: DayOfWeek): Int =
        (scheduleId * 7 + day.value).toInt()
}
