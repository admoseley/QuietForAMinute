package com.admoseley.quietforaminute.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.admoseley.quietforaminute.R

/**
 * Duration input built from preset chips + a compact stepper, replacing a repurposed clock-face
 * TimePicker. A TimePicker reads as "what time is it", not "how long" — the clock metaphor was a
 * recurring point of confusion for a duration (see issue #13). Presets cover the common cases in
 * one tap; the stepper handles anything else without the ambiguity of dragging a clock hand.
 *
 * Minutes step by 5 — this app's own minimum mute duration is 5 minutes (see
 * ScheduleEditViewModel), and single-minute precision isn't meaningful for "how long to stay
 * muted". Hours step by 1, capped at [maxHours].
 */
@Composable
fun DurationPicker(
    hours: Int,
    minutes: Int,
    onHoursChange: (Int) -> Unit,
    onMinutesChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    maxHours: Int = 23
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DurationPresetChips(
            hours = hours,
            minutes = minutes,
            onPresetSelected = { h, m -> onHoursChange(h); onMinutesChange(m) }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DurationStepper(
                label = stringResource(R.string.duration_picker_hours_label),
                value = hours,
                onValueChange = { onHoursChange(it.coerceIn(0, maxHours)) },
                step = 1,
                min = 0,
                max = maxHours,
                decreaseCd = stringResource(R.string.duration_picker_decrease_hours_cd),
                increaseCd = stringResource(R.string.duration_picker_increase_hours_cd),
                modifier = Modifier.weight(1f)
            )
            DurationStepper(
                label = stringResource(R.string.duration_picker_minutes_label),
                value = minutes,
                onValueChange = { onMinutesChange(it.coerceIn(0, 55)) },
                step = 5,
                min = 0,
                max = 55,
                decreaseCd = stringResource(R.string.duration_picker_decrease_minutes_cd),
                increaseCd = stringResource(R.string.duration_picker_increase_minutes_cd),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DurationPresetChips(
    hours: Int,
    minutes: Int,
    onPresetSelected: (hours: Int, minutes: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val presets = listOf(
        Triple(0, 15, stringResource(R.string.duration_preset_15m)),
        Triple(0, 30, stringResource(R.string.duration_preset_30m)),
        Triple(1, 0, stringResource(R.string.duration_preset_1h)),
        Triple(2, 0, stringResource(R.string.duration_preset_2h))
    )
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        presets.forEach { (presetHours, presetMinutes, label) ->
            val selected = hours == presetHours && minutes == presetMinutes
            FilterChip(
                selected = selected,
                onClick = { onPresetSelected(presetHours, presetMinutes) },
                label = { Text(label) }
            )
        }
    }
}

@Composable
private fun DurationStepper(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    step: Int,
    min: Int,
    max: Int,
    decreaseCd: String,
    increaseCd: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(
                onClick = { onValueChange(value - step) },
                enabled = value > min,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(painterResource(R.drawable.ic_remove), contentDescription = decreaseCd, modifier = Modifier.size(18.dp))
            }
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .width(48.dp)
                    .padding(horizontal = 4.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            FilledTonalIconButton(
                onClick = { onValueChange(value + step) },
                enabled = value < max,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(painterResource(R.drawable.ic_add), contentDescription = increaseCd, modifier = Modifier.size(18.dp))
            }
        }
    }
}
