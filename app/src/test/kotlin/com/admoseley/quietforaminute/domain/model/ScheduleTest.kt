package com.admoseley.quietforaminute.domain.model

import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Test

class ScheduleTest {

    private fun schedule(hour: Int, minute: Int, durationMinutes: Int) = Schedule(
        label = "Test",
        days = setOf(DayOfWeek.MONDAY),
        triggerHour = hour,
        triggerMinute = minute,
        durationMinutes = durationMinutes
    )

    @Test
    fun `midnight formats as 12 AM, not 0 AM`() {
        assertEquals("12:00 AM", schedule(0, 0, 30).formattedTime)
    }

    @Test
    fun `noon formats as 12 PM, not 0 PM`() {
        assertEquals("12:00 PM", schedule(12, 0, 30).formattedTime)
    }

    @Test
    fun `morning hour formats as-is with AM`() {
        assertEquals("9:05 AM", schedule(9, 5, 30).formattedTime)
    }

    @Test
    fun `afternoon hour subtracts 12 and formats with PM`() {
        assertEquals("1:00 PM", schedule(13, 0, 30).formattedTime)
    }

    @Test
    fun `last minute before midnight formats correctly`() {
        assertEquals("11:59 PM", schedule(23, 59, 30).formattedTime)
    }

    @Test
    fun `duration under an hour shows minutes only`() {
        assertEquals("45m", schedule(8, 0, 45).formattedDuration)
    }

    @Test
    fun `duration of exactly one hour shows hours only`() {
        assertEquals("1h", schedule(8, 0, 60).formattedDuration)
    }

    @Test
    fun `duration with both hours and minutes shows both`() {
        assertEquals("1h 30m", schedule(8, 0, 90).formattedDuration)
    }

    @Test
    fun `multi-hour duration formats correctly`() {
        assertEquals("8h", schedule(8, 0, 480).formattedDuration)
    }
}
