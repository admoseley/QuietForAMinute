package com.admoseley.quietforaminute.data.db

import com.admoseley.quietforaminute.domain.model.Schedule
import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleEntityTest {

    private fun schedule(days: Set<DayOfWeek>) = Schedule(
        id = 1,
        label = "Test",
        days = days,
        triggerHour = 8,
        triggerMinute = 0,
        durationMinutes = 30
    )

    @Test
    fun `single day round-trips through the bitmask`() {
        DayOfWeek.entries.forEach { day ->
            val original = schedule(setOf(day))
            val roundTripped = original.toEntity().toDomain()
            assertEquals("round-trip failed for $day", setOf(day), roundTripped.days)
        }
    }

    @Test
    fun `every day round-trips through the bitmask`() {
        val allDays = DayOfWeek.entries.toSet()
        val roundTripped = schedule(allDays).toEntity().toDomain()
        assertEquals(allDays, roundTripped.days)
    }

    @Test
    fun `no days round-trips to an empty set`() {
        val roundTripped = schedule(emptySet()).toEntity().toDomain()
        assertEquals(emptySet<DayOfWeek>(), roundTripped.days)
    }

    @Test
    fun `weekdays only, weekend bits stay unset`() {
        val weekdays = setOf(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
        )
        val entity = schedule(weekdays).toEntity()

        // bit0=Mon(1) .. bit4=Fri(16) set, bit5=Sat(32)/bit6=Sun(64) clear -> 0b0011111 = 31
        assertEquals(31, entity.daysBitmask)
        assertEquals(weekdays, entity.toDomain().days)
    }

    @Test
    fun `sunday is the high bit not bit zero`() {
        // Common off-by-one: DayOfWeek.SUNDAY.value is 7, not 0.
        val entity = schedule(setOf(DayOfWeek.SUNDAY)).toEntity()
        assertEquals(1 shl 6, entity.daysBitmask)
    }

    @Test
    fun `dnd flag round-trips in both directions`() {
        // Both mappings are hand-written, so a field added to one and forgotten in the other
        // would silently drop the user's choice on every load or save.
        val on = schedule(setOf(DayOfWeek.MONDAY)).copy(dndEnabled = true)
        assertTrue(on.toEntity().dndEnabled)
        assertTrue(on.toEntity().toDomain().dndEnabled)

        val off = schedule(setOf(DayOfWeek.MONDAY)).copy(dndEnabled = false)
        assertFalse(off.toEntity().dndEnabled)
        assertFalse(off.toEntity().toDomain().dndEnabled)
    }

    @Test
    fun `dnd defaults to off for a schedule that never set it`() {
        // Matches the MIGRATION_1_2 column default, so a schedule saved before the DND option
        // existed keeps behaving exactly as it did.
        assertFalse(schedule(setOf(DayOfWeek.MONDAY)).dndEnabled)
        assertFalse(schedule(setOf(DayOfWeek.MONDAY)).toEntity().dndEnabled)
    }
}
