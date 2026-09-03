package com.admoseley.quietforaminute.ui.schedules

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.admoseley.quietforaminute.R
import com.admoseley.quietforaminute.ui.schedules.components.ScheduleCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleListScreen(
    onAddSchedule: () -> Unit,
    onEditSchedule: (Long) -> Unit,
    justSavedWithoutAlarms: Boolean = false,
    onJustSavedWithoutAlarmsConsumed: () -> Unit = {},
    viewModel: ScheduleListViewModel = hiltViewModel()
) {
    val schedules by viewModel.schedules.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    // stringResource() is @Composable-only, so these are captured here rather than inside the
    // plain showAlarmsNotArmedSnackbar() function below.
    val alarmsNotArmedMessage = stringResource(R.string.schedule_alarms_not_armed_snackbar)
    val grantActionLabel = stringResource(R.string.action_grant)

    fun showAlarmsNotArmedSnackbar() {
        coroutineScope.launch {
            val result = snackbarHostState.showSnackbar(
                message = alarmsNotArmedMessage,
                actionLabel = grantActionLabel,
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                context.startActivity(
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = "package:${context.packageName}".toUri()
                    }
                )
            }
        }
    }

    // Fired when a save on the edit screen went through but nothing could actually be armed.
    LaunchedEffect(justSavedWithoutAlarms) {
        if (justSavedWithoutAlarms) {
            showAlarmsNotArmedSnackbar()
            onJustSavedWithoutAlarmsConsumed()
        }
    }

    // Same warning, for toggling a schedule back on directly from this list.
    LaunchedEffect(Unit) {
        viewModel.alarmsNotArmed.collect { showAlarmsNotArmedSnackbar() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.schedules_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddSchedule,
                icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = stringResource(R.string.schedule_add_cd)) },
                text = { Text(stringResource(R.string.schedule_add_fab)) }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            AnimatedVisibility(
                visible = schedules.isEmpty(),
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                EmptyState(onAddSchedule = onAddSchedule)
            }

            AnimatedVisibility(
                visible = schedules.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 96.dp // space for FAB
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(schedules, key = { it.id }) { schedule ->
                        // confirmValueChange is deprecated (Compose foundation now recommends
                        // driving the anchor set instead of vetoing changes via callback). The
                        // veto here was never actually restricting direction — that's already
                        // enableDismissFromStartToEnd = false below — it was only used to fire
                        // the delete as a side effect, which a LaunchedEffect does just as well.
                        val dismissState = rememberSwipeToDismissBoxState()

                        LaunchedEffect(dismissState.currentValue) {
                            if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                                viewModel.delete(schedule)
                            }
                        }

                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            backgroundContent = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 8.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Text(
                                        stringResource(R.string.schedule_swipe_delete_label),
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        ) {
                            ScheduleCard(
                                schedule = schedule,
                                onToggleEnabled = { enabled -> viewModel.toggleEnabled(schedule, enabled) },
                                onEdit = { onEditSchedule(schedule.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(onAddSchedule: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(32.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_calendar_month),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(72.dp)
        )
        Text(
            text = stringResource(R.string.schedule_empty_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.schedule_empty_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        FilledTonalButton(onClick = onAddSchedule) {
            Icon(painterResource(R.drawable.ic_add), null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.schedule_create_first_button))
        }
    }
}
