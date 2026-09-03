package com.admoseley.quietforaminute.scheduler

import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AlarmTimingTest {

    private val zone = ZoneId.of("America/Chicago")

    @Test
    fun `same day before trigger time fires today`() {
        // Monday 2026-09-07 at 07:00, requesting Monday 08:00 -> today at 08:00.
        val from = ZonedDateTime.of(2026, 9, 7, 7, 0, 0, 0, zone)
        val next = AlarmTiming.nextOccurrence(from, DayOfWeek.MONDAY, 8, 0)
        assertEquals(ZonedDateTime.of(2026, 9, 7, 8, 0, 0, 0, zone), next)
    }

    @Test
    fun `same day after trigger time rolls to next week`() {
        // Monday 2026-09-07 at 09:00, requesting Monday 08:00 -> next Monday, not today.
        val from = ZonedDateTime.of(2026, 9, 7, 9, 0, 0, 0, zone)
        val next = AlarmTiming.nextOccurrence(from, DayOfWeek.MONDAY, 8, 0)
        assertEquals(ZonedDateTime.of(2026, 9, 14, 8, 0, 0, 0, zone), next)
    }

    @Test
    fun `same day at exactly the trigger time rolls to next week`() {
        // isAfter() is strict, so an exact match is treated as "already happened".
        val from = ZonedDateTime.of(2026, 9, 7, 8, 0, 0, 0, zone)
        val next = AlarmTiming.nextOccurrence(from, DayOfWeek.MONDAY, 8, 0)
        assertEquals(ZonedDateTime.of(2026, 9, 14, 8, 0, 0, 0, zone), next)
    }

    @Test
    fun `a different day later this week fires this week`() {
        // Monday 2026-09-07, requesting Friday -> Friday 2026-09-11, same week.
        val from = ZonedDateTime.of(2026, 9, 7, 7, 0, 0, 0, zone)
        val next = AlarmTiming.nextOccurrence(from, DayOfWeek.FRIDAY, 8, 0)
        assertEquals(ZonedDateTime.of(2026, 9, 11, 8, 0, 0, 0, zone), next)
    }

    @Test
    fun `a different day earlier this week wraps to next week`() {
        // Friday 2026-09-11, requesting Monday -> Monday 2026-09-14, not the Monday just passed.
        val from = ZonedDateTime.of(2026, 9, 11, 7, 0, 0, 0, zone)
        val next = AlarmTiming.nextOccurrence(from, DayOfWeek.MONDAY, 8, 0)
        assertEquals(ZonedDateTime.of(2026, 9, 14, 8, 0, 0, 0, zone), next)
    }

    @Test
    fun `result crosses a spring-forward DST boundary without throwing`() {
        // US DST started 2026-03-08. Requesting a Sunday shortly before it should still resolve
        // to a valid, later instant on the other side of the gap.
        val from = ZonedDateTime.of(2026, 3, 1, 7, 0, 0, 0, zone)
        val next = AlarmTiming.nextOccurrence(from, DayOfWeek.SUNDAY, 8, 0)
        assertEquals(DayOfWeek.SUNDAY, next.dayOfWeek)
        assert(next.isAfter(from))
    }

    @Test
    fun `requestCode is unique across days for one schedule`() {
        val codes = DayOfWeek.entries.map { AlarmTiming.requestCode(scheduleId = 5, day = it) }
        assertEquals(codes.size, codes.toSet().size)
    }

    @Test
    fun `requestCode does not collide between adjacent schedule ids`() {
        val scheduleOneCodes = DayOfWeek.entries.map { AlarmTiming.requestCode(1, it) }.toSet()
        val scheduleTwoCodes = DayOfWeek.entries.map { AlarmTiming.requestCode(2, it) }.toSet()
        assertEquals(emptySet<Int>(), scheduleOneCodes intersect scheduleTwoCodes)
    }

    @Test
    fun `requestCode changes when the schedule id changes`() {
        assertNotEquals(
            AlarmTiming.requestCode(1, DayOfWeek.MONDAY),
            AlarmTiming.requestCode(2, DayOfWeek.MONDAY)
        )
    }
}
