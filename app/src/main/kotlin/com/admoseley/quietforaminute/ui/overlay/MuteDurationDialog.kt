package com.admoseley.quietforaminute.ui.overlay

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.admoseley.quietforaminute.R
import kotlin.math.roundToInt

/**
 * Content of the system-overlay popup. Hosted by OverlayViewController inside a ComposeView, so
 * it must not use Dialog()/AlertDialog() (those need an Activity window).
 *
 * @param maxVolume max index of the stream that was muted — ring and music differ, so the caller
 *                  passes the right one and [initialRestoreVolume] is already in those units.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MuteDurationDialog(
    initialRestoreVolume: Int,
    maxVolume: Int,
    onConfirm: (hours: Int, minutes: Int, restoreVolume: Int) -> Unit,
    onDismiss: () -> Unit
) {
    // A TimePicker is repurposed as an hours:minutes *duration* input (24h mode so the hour dial
    // reads 0..23). It works, but the clock-face metaphor is a known point of confusion; a pair
    // of number steppers or preset chips (15m / 30m / 1h / 2h) would be more direct.
    val timePickerState = rememberTimePickerState(
        initialHour = 0,
        initialMinute = 30,
        is24Hour = true // 24h mode is best for selecting a duration
    )

    var restoreVolume by remember { mutableIntStateOf(initialRestoreVolume) }

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 16.dp,
        modifier = Modifier
            .padding(16.dp)
            .width(IntrinsicSize.Min)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )

            Text(
                text = stringResource(R.string.mute_dialog_title),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )

            // Material 3 Time Picker component
            TimePicker(
                state = timePickerState,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            HorizontalDivider()

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (restoreVolume == 0) Icons.AutoMirrored.Filled.VolumeOff
                        else if (restoreVolume < maxVolume / 2) Icons.AutoMirrored.Filled.VolumeDown
                        else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        stringResource(R.string.mute_dialog_restore_volume_label),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "${(restoreVolume.toFloat() / maxVolume * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = restoreVolume.toFloat(),
                    onValueChange = { restoreVolume = it.roundToInt() },
                    valueRange = 0f..maxVolume.toFloat(),
                    steps = maxVolume - 1,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_skip))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { onConfirm(timePickerState.hour, timePickerState.minute, restoreVolume) },
                    enabled = timePickerState.hour > 0 || timePickerState.minute > 0
                ) {
                    Text(stringResource(R.string.action_start))
                }
            }
        }
    }
}
