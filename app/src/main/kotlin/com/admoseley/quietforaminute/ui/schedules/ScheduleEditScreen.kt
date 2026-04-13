package com.admoseley.quietforaminute.ui.schedules

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.admoseley.quietforaminute.ui.schedules.components.DayChipSelector
import java.time.DayOfWeek

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleEditScreen(
    scheduleId: Long,
    onSaved: () -> Unit,
    onDeleted: () -> Unit,
    onBack: () -> Unit,
    viewModel: ScheduleEditViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(scheduleId) { viewModel.load(scheduleId) }

    // Collect one-shot navigation events (SharedFlow — only fires once, survives config change)
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                ScheduleEditEvent.Saved -> onSaved()
                ScheduleEditEvent.Deleted -> onDeleted()
            }
        }
    }

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showDurationPicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val isNew = scheduleId < 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isNew) "New Schedule" else "Edit Schedule",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isNew) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Button(
                    onClick = { viewModel.save() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    enabled = !state.isLoading
                ) {
                    Text(if (isNew) "Create Schedule" else "Save Changes")
                }
            }
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Label
            OutlinedTextField(
                value = state.label,
                onValueChange = { viewModel.setLabel(it) },
                label = { Text("Schedule name") },
                placeholder = { Text("e.g. Morning class, Friday meetings") },
                isError = state.labelError != null,
                supportingText = state.labelError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Day selector
            SectionCard(title = "Repeat on", error = state.daysError) {
                DayChipSelector(
                    selectedDays = state.days,
                    onDayToggled = { viewModel.toggleDay(it) },
                    modifier = Modifier.padding(top = 8.dp)
                )
                QuickDayPresets(
                    onWeekdays = {
                        val weekdays = setOf(
                            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
                        )
                        // Remove any non-weekdays, add any missing weekdays
                        (state.days - weekdays).forEach { viewModel.toggleDay(it) }
                        (weekdays - state.days).forEach { viewModel.toggleDay(it) }
                    },
                    onWeekends = {
                        val weekends = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
                        (state.days - weekends).forEach { viewModel.toggleDay(it) }
                        (weekends - state.days).forEach { viewModel.toggleDay(it) }
                    },
                    onEveryDay = {
                        val all = DayOfWeek.entries.toSet()
                        (all - state.days).forEach { viewModel.toggleDay(it) }
                    }
                )
            }

            // Trigger time
            SectionCard(title = "Start time") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val h = state.triggerHour
                    val m = state.triggerMinute
                    val amPm = if (h < 12) "AM" else "PM"
                    val h12 = when { h == 0 -> 12; h > 12 -> h - 12; else -> h }
                    Text(
                        text = "%d:%02d %s".format(h12, m, amPm),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedButton(onClick = { showStartTimePicker = true }) {
                        Text("Change")
                    }
                }
            }

            // Duration
            SectionCard(title = "Mute duration", error = state.durationError) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val h = state.durationHours
                    val m = state.durationMinutes
                    Text(
                        text = when {
                            h > 0 && m > 0 -> "${h}h ${m}m"
                            h > 0 -> "${h}h"
                            else -> "${m}m"
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedButton(onClick = { showDurationPicker = true }) {
                        Text("Change")
                    }
                }
            }
        }
    }

    // Start Time picker dialog
    if (showStartTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = state.triggerHour,
            initialMinute = state.triggerMinute,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showStartTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setTime(timePickerState.hour, timePickerState.minute)
                    showStartTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartTimePicker = false }) { Text("Cancel") }
            },
            text = { TimePicker(state = timePickerState) }
        )
    }

    // Duration picker dialog
    if (showDurationPicker) {
        val durationPickerState = rememberTimePickerState(
            initialHour = state.durationHours,
            initialMinute = state.durationMinutes,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showDurationPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setDurationHours(durationPickerState.hour)
                    viewModel.setDurationMinutes(durationPickerState.minute)
                    showDurationPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDurationPicker = false }) { Text("Cancel") }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Select Duration (Hours : Minutes)",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    TimePicker(state = durationPickerState)
                }
            }
        )
    }

    // Delete confirm dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete schedule?") },
            text = { Text("\"${state.label}\" will be permanently deleted and all future alarms removed.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.delete(); showDeleteConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    error: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            content()
            if (error != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = error,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun QuickDayPresets(
    onWeekdays: () -> Unit,
    onWeekends: () -> Unit,
    onEveryDay: () -> Unit
) {
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SuggestionChip(onClick = onWeekdays, label = { Text("Weekdays") })
        SuggestionChip(onClick = onWeekends, label = { Text("Weekends") })
        SuggestionChip(onClick = onEveryDay, label = { Text("Every day") })
    }
}
