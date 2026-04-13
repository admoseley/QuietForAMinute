package com.admoseley.quietforaminute.domain.model

import java.time.DayOfWeek

data class Schedule(
    val id: Long = 0,
    val label: String,
    val days: Set<DayOfWeek>,
    val triggerHour: Int,
    val triggerMinute: Int,
    val durationMinutes: Int,
    val isEnabled: Boolean = true
) {
    val formattedTime: String
        get() {
            val h = triggerHour
            val m = triggerMinute
            val amPm = if (h < 12) "AM" else "PM"
            val hour12 = when {
                h == 0 -> 12
                h > 12 -> h - 12
                else -> h
            }
            return "%d:%02d %s".format(hour12, m, amPm)
        }

    val formattedDuration: String
        get() {
            val h = durationMinutes / 60
            val m = durationMinutes % 60
            return when {
                h > 0 && m > 0 -> "${h}h ${m}m"
                h > 0 -> "${h}h"
                else -> "${m}m"
            }
        }
}
