package com.admoseley.quietforaminute.ui.schedules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.admoseley.quietforaminute.data.repository.ScheduleRepository
import com.admoseley.quietforaminute.domain.model.Schedule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import javax.inject.Inject

data class ScheduleEditState(
    val id: Long = 0,
    val label: String = "",
    val days: Set<DayOfWeek> = setOf(DayOfWeek.MONDAY),
    val triggerHour: Int = 8,
    val triggerMinute: Int = 0,
    val durationHours: Int = 1,
    val durationMinutes: Int = 0,
    val isEnabled: Boolean = true,
    val isLoading: Boolean = true,
    val labelError: String? = null,
    val daysError: String? = null,
    val durationError: String? = null
)

/** One-shot navigation events — avoids the stale-boolean problem on config change. */
sealed class ScheduleEditEvent {
    object Saved : ScheduleEditEvent()
    /** Saved successfully, but exact alarms couldn't be armed (permission not granted). */
    object SavedWithoutAlarms : ScheduleEditEvent()
    object Deleted : ScheduleEditEvent()
}

@HiltViewModel
class ScheduleEditViewModel @Inject constructor(
    private val repository: ScheduleRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ScheduleEditState())
    val state = _state.asStateFlow()

    /** One-shot events consumed by the UI via collectAsEffect. */
    private val _events = MutableSharedFlow<ScheduleEditEvent>()
    val events = _events.asSharedFlow()

    private var hasLoaded = false

    fun load(scheduleId: Long) {
        // Guard against re-loading on recomposition / config change
        if (hasLoaded) return
        hasLoaded = true

        if (scheduleId < 0) {
            _state.update { it.copy(isLoading = false) }
            return
        }
        viewModelScope.launch {
            val schedule = repository.getById(scheduleId)
            if (schedule != null) {
                _state.update {
                    it.copy(
                        id = schedule.id,
                        label = schedule.label,
                        days = schedule.days,
                        triggerHour = schedule.triggerHour,
                        triggerMinute = schedule.triggerMinute,
                        durationHours = schedule.durationMinutes / 60,
                        durationMinutes = schedule.durationMinutes % 60,
                        isEnabled = schedule.isEnabled,
                        isLoading = false
                    )
                }
            } else {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun setLabel(label: String) = _state.update { it.copy(label = label, labelError = null) }
    fun toggleDay(day: DayOfWeek) = _state.update { s ->
        val newDays = if (day in s.days) s.days - day else s.days + day
        s.copy(days = newDays, daysError = null)
    }
    fun setTime(hour: Int, minute: Int) = _state.update { it.copy(triggerHour = hour, triggerMinute = minute) }
    fun setDurationHours(h: Int) = _state.update { it.copy(durationHours = h, durationError = null) }
    fun setDurationMinutes(m: Int) = _state.update { it.copy(durationMinutes = m, durationError = null) }

    fun save() {
        _state.update { it.copy(labelError = null, daysError = null, durationError = null) }
        val s = _state.value
        var hasError = false

        if (s.label.isBlank()) {
            _state.update { it.copy(labelError = "Name is required") }
            hasError = true
        }
        if (s.days.isEmpty()) {
            _state.update { it.copy(daysError = "Select at least one day") }
            hasError = true
        }
        // Scheduled mutes need a floor: a 1-minute mute is indistinguishable from an alarm glitch
        // and the restore chime would fire almost immediately. Keep INSTRUCTIONS.md in sync.
        val totalMinutes = s.durationHours * 60 + s.durationMinutes
        if (totalMinutes < 5) {
            _state.update { it.copy(durationError = "Duration must be at least 5 minutes") }
            hasError = true
        }
        if (hasError) return

        viewModelScope.launch {
            val result = repository.save(
                Schedule(
                    id = s.id,
                    label = s.label.trim(),
                    days = s.days,
                    triggerHour = s.triggerHour,
                    triggerMinute = s.triggerMinute,
                    durationMinutes = totalMinutes,
                    isEnabled = s.isEnabled
                )
            )
            _events.emit(if (result.alarmsArmed) ScheduleEditEvent.Saved else ScheduleEditEvent.SavedWithoutAlarms)
        }
    }

    fun delete() {
        val s = _state.value
        if (s.id <= 0) return
        viewModelScope.launch {
            repository.delete(
                Schedule(
                    id = s.id, label = s.label, days = s.days,
                    triggerHour = s.triggerHour, triggerMinute = s.triggerMinute,
                    durationMinutes = s.durationHours * 60 + s.durationMinutes,
                    isEnabled = s.isEnabled
                )
            )
            _events.emit(ScheduleEditEvent.Deleted)
        }
    }
}
