package com.admoseley.quietforaminute.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.admoseley.quietforaminute.domain.model.Schedule
import java.time.DayOfWeek

@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    // Bitmask: bit0=Mon (value 1), bit1=Tue (2), ..., bit6=Sun (64)
    val daysBitmask: Int,
    val triggerHour: Int,
    val triggerMinute: Int,
    val durationMinutes: Int,
    val isEnabled: Boolean = true,
    /** Also turn Do Not Disturb on for this schedule's duration (issue #45). */
    val dndEnabled: Boolean = false
)

// DayOfWeek.value is 1 (Monday) .. 7 (Sunday), hence the `- 1` when mapping to bit positions.
// These two functions are pure and are the first thing worth covering with a unit test.
fun ScheduleEntity.toDomain(): Schedule = Schedule(
    id = id,
    label = label,
    days = DayOfWeek.entries.filter { day ->
        daysBitmask and (1 shl (day.value - 1)) != 0
    }.toSet(),
    triggerHour = triggerHour,
    triggerMinute = triggerMinute,
    durationMinutes = durationMinutes,
    isEnabled = isEnabled,
    dndEnabled = dndEnabled
)

fun Schedule.toEntity(): ScheduleEntity = ScheduleEntity(
    id = id,
    label = label,
    daysBitmask = days.fold(0) { acc, day -> acc or (1 shl (day.value - 1)) },
    triggerHour = triggerHour,
    triggerMinute = triggerMinute,
    durationMinutes = durationMinutes,
    isEnabled = isEnabled,
    dndEnabled = dndEnabled
)
