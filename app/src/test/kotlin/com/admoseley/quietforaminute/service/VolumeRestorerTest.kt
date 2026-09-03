package com.admoseley.quietforaminute.service

import org.junit.Assert.assertEquals
import org.junit.Test

class VolumeRestorerTest {

    @Test
    fun `same max returns the same value`() {
        assertEquals(5, scaleVolumeUnits(units = 5, fromMax = 10, toMax = 10))
    }

    @Test
    fun `scales down proportionally to a smaller max`() {
        // 5 of 10 (50%) -> 50% of a 0..4 range, rounded.
        assertEquals(2, scaleVolumeUnits(units = 5, fromMax = 10, toMax = 4))
    }

    @Test
    fun `scales up proportionally to a larger max`() {
        assertEquals(10, scaleVolumeUnits(units = 5, fromMax = 10, toMax = 20))
    }

    @Test
    fun `zero units scales to zero regardless of max`() {
        assertEquals(0, scaleVolumeUnits(units = 0, fromMax = 10, toMax = 7))
    }

    @Test
    fun `a zero source max does not divide by zero`() {
        assertEquals(0, scaleVolumeUnits(units = 5, fromMax = 0, toMax = 10))
    }

    @Test
    fun `rounds to nearest instead of truncating`() {
        // 3 of 10 (30%) of a 0..7 range = 2.1 -> rounds to 2, not truncates to 2 (same here);
        // use a value where truncation and rounding actually differ.
        assertEquals(4, scaleVolumeUnits(units = 5, fromMax = 9, toMax = 7)) // 3.888... -> 4
    }
}
