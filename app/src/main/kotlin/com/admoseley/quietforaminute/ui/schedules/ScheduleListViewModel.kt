package com.admoseley.quietforaminute.ui.schedules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.admoseley.quietforaminute.data.repository.ScheduleRepository
import com.admoseley.quietforaminute.domain.model.Schedule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScheduleListViewModel @Inject constructor(
    private val repository: ScheduleRepository
) : ViewModel() {

    val schedules = repository.schedules.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    /** One-shot signal for the "enabled but couldn't actually arm alarms" case. */
    private val _alarmsNotArmed = MutableSharedFlow<Unit>()
    val alarmsNotArmed = _alarmsNotArmed.asSharedFlow()

    fun toggleEnabled(schedule: Schedule, enabled: Boolean) {
        viewModelScope.launch {
            val armed = repository.setEnabled(schedule, enabled)
            if (enabled && !armed) _alarmsNotArmed.emit(Unit)
        }
    }

    fun delete(schedule: Schedule) {
        viewModelScope.launch {
            repository.delete(schedule)
        }
    }
}
