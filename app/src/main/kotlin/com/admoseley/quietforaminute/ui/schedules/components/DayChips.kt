package com.admoseley.quietforaminute.ui.schedules.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek

private val dayLabels = mapOf(
    DayOfWeek.SUNDAY to "S",
    DayOfWeek.MONDAY to "M",
    DayOfWeek.TUESDAY to "T",
    DayOfWeek.WEDNESDAY to "W",
    DayOfWeek.THURSDAY to "T",
    DayOfWeek.FRIDAY to "F",
    DayOfWeek.SATURDAY to "S"
)

private val daysInOrder = listOf(
    DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, 
    DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY
)

@Composable
fun DayChipsDisplay(
    activeDays: Set<DayOfWeek>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        daysInOrder.forEach { day ->
            val active = day in activeDays
            Surface(
                shape = CircleShape,
                color = if (active) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = dayLabels[day] ?: "",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.sp,
                            fontWeight = if (active) FontWeight.ExtraBold else FontWeight.Normal
                        ),
                        color = if (active) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayChipSelector(
    selectedDays: Set<DayOfWeek>,
    onDayToggled: (DayOfWeek) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        daysInOrder.forEach { day ->
            val selected = day in selectedDays
            val containerColor = if (selected) MaterialTheme.colorScheme.primary 
                                else MaterialTheme.colorScheme.surfaceVariant
            val contentColor = if (selected) MaterialTheme.colorScheme.onPrimary 
                              else MaterialTheme.colorScheme.onSurfaceVariant

            Surface(
                selected = selected,
                onClick = { onDayToggled(day) },
                shape = CircleShape,
                color = containerColor,
                contentColor = contentColor,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = dayLabels[day] ?: "",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    )
                }
            }
        }
    }
}
