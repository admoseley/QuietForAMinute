package com.admoseley.quietforaminute.ui.overlay

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.admoseley.quietforaminute.R
import com.admoseley.quietforaminute.ui.components.DurationPicker
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
    var hours by remember { mutableIntStateOf(0) }
    var minutes by remember { mutableIntStateOf(30) }
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
                painter = painterResource(R.drawable.ic_volume_off),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )

            Text(
                text = stringResource(R.string.mute_dialog_title),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )

            DurationPicker(
                hours = hours,
                minutes = minutes,
                onHoursChange = { hours = it },
                onMinutesChange = { minutes = it },
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
                        painter = painterResource(
                            if (restoreVolume == 0) R.drawable.ic_volume_off
                            else if (restoreVolume < maxVolume / 2) R.drawable.ic_volume_down
                            else R.drawable.ic_volume_up
                        ),
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
                    onClick = { onConfirm(hours, minutes, restoreVolume) },
                    enabled = hours > 0 || minutes > 0
                ) {
                    Text(stringResource(R.string.action_start))
                }
            }
        }
    }
}
