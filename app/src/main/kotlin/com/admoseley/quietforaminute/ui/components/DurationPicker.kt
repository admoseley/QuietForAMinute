package com.admoseley.quietforaminute.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.admoseley.quietforaminute.R

/** Largest minute value that can be entered. Steppers move in 5s; typing allows any of 0..59. */
private const val MAX_MINUTES = 59

/**
 * Duration input built from preset chips + editable hour/minute fields with steppers, replacing a
 * repurposed clock-face TimePicker. A TimePicker reads as "what time is it", not "how long" — the
 * clock metaphor was a recurring point of confusion for a duration (see issue #13).
 *
 * Three ways in, deliberately: presets for the common cases in one tap, +/- for small nudges, and
 * typing for an exact value the other two can't reach (issue #41 — the steppers move minutes in
 * 5s, so 3 or 47 minutes was previously unreachable).
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
                fieldCd = stringResource(R.string.duration_picker_hours_field_cd),
                modifier = Modifier.weight(1f)
            )
            DurationStepper(
                label = stringResource(R.string.duration_picker_minutes_label),
                value = minutes,
                onValueChange = { onMinutesChange(it.coerceIn(0, MAX_MINUTES)) },
                step = 5,
                min = 0,
                max = MAX_MINUTES,
                decreaseCd = stringResource(R.string.duration_picker_decrease_minutes_cd),
                increaseCd = stringResource(R.string.duration_picker_increase_minutes_cd),
                fieldCd = stringResource(R.string.duration_picker_minutes_field_cd),
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
    fieldCd: String,
    modifier: Modifier = Modifier
) {
    // Local text state, re-seeded whenever [value] changes from outside (a preset, a +/- tap).
    // Keeping the raw string separate from the parsed Int is what lets the field be momentarily
    // empty while the user clears it to type a new number, instead of snapping back to "0"
    // under the cursor.
    var text by remember(value) { mutableStateOf(value.toString()) }

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

            BasicTextField(
                value = text,
                onValueChange = { raw ->
                    // Digits only, and never more than 2 — anything longer can't be a valid
                    // hour or minute, and silently dropping the extra keeps the field from
                    // growing wider than its slot.
                    val digits = raw.filter { it.isDigit() }.take(2)
                    text = digits
                    // An empty field is a legal intermediate state while typing, so only push a
                    // value up when there's actually a number to push.
                    digits.toIntOrNull()?.let { onValueChange(it.coerceIn(min, max)) }
                },
                textStyle = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                // BasicTextField has no contentDescription parameter, so label it via semantics.
                modifier = Modifier
                    .width(48.dp)
                    .padding(horizontal = 4.dp)
                    .semantics { contentDescription = fieldCd }
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
