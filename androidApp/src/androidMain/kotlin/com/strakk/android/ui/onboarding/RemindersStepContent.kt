package com.strakk.android.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.strakk.android.ui.theme.LocalStrakkColors
import com.strakk.android.ui.theme.StrakkTheme

private val DAY_LABELS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersStepContent(
    trackingReminderEnabled: Boolean,
    trackingReminderTime: String,
    checkinReminderEnabled: Boolean,
    checkinReminderDay: Int,
    checkinReminderTime: String,
    onTrackingReminderToggled: (Boolean) -> Unit,
    onTrackingReminderTimeChanged: (String) -> Unit,
    onCheckinReminderToggled: (Boolean) -> Unit,
    onCheckinReminderDayChanged: (Int) -> Unit,
    onCheckinReminderTimeChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showTrackingTimePicker by remember { mutableStateOf(false) }
    var showCheckinTimePicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Icon(
            imageVector = Icons.Outlined.Notifications,
            contentDescription = null,
            tint = LocalStrakkColors.current.textSecondary,
            modifier = Modifier
                .size(56.dp)
                .align(Alignment.CenterHorizontally),
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Stay on track",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "We'll gently remind you — you can change these later",
            style = MaterialTheme.typography.bodyLarge,
            color = LocalStrakkColors.current.textSecondary,
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Daily tracking reminder card
        ReminderCard(
            title = "Daily tracking",
            enabled = trackingReminderEnabled,
            onToggle = onTrackingReminderToggled,
        ) {
            AnimatedVisibility(visible = trackingReminderEnabled) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    TimeRow(
                        label = "Reminder time",
                        time = trackingReminderTime,
                        onTimeClick = { showTrackingTimePicker = true },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Weekly check-in card
        ReminderCard(
            title = "Weekly check-in",
            enabled = checkinReminderEnabled,
            onToggle = onCheckinReminderToggled,
        ) {
            AnimatedVisibility(visible = checkinReminderEnabled) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Day of week",
                        style = MaterialTheme.typography.labelMedium,
                        color = LocalStrakkColors.current.textSecondary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    DayPicker(
                        selectedDay = checkinReminderDay,
                        onDaySelected = onCheckinReminderDayChanged,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    TimeRow(
                        label = "Reminder time",
                        time = checkinReminderTime,
                        onTimeClick = { showCheckinTimePicker = true },
                    )
                }
            }
        }
    }

    // Time picker dialogs
    if (showTrackingTimePicker) {
        val parts = trackingReminderTime.split(":")
        val state = rememberTimePickerState(
            initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 17,
            initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0,
            is24Hour = true,
        )
        TimePickerDialog(
            onConfirm = {
                val h = state.hour.toString().padStart(2, '0')
                val m = state.minute.toString().padStart(2, '0')
                onTrackingReminderTimeChanged("$h:$m")
                showTrackingTimePicker = false
            },
            onDismiss = { showTrackingTimePicker = false },
        ) {
            TimePicker(
                state = state,
                colors = TimePickerDefaults.colors(
                    clockDialColor = LocalStrakkColors.current.surface2,
                    clockDialSelectedContentColor = MaterialTheme.colorScheme.onBackground,
                    clockDialUnselectedContentColor = LocalStrakkColors.current.textSecondary,
                    selectorColor = MaterialTheme.colorScheme.primary,
                    timeSelectorSelectedContainerColor = MaterialTheme.colorScheme.primary,
                    timeSelectorUnselectedContainerColor = LocalStrakkColors.current.surface2,
                    timeSelectorSelectedContentColor = MaterialTheme.colorScheme.onBackground,
                    timeSelectorUnselectedContentColor = LocalStrakkColors.current.textSecondary,
                ),
            )
        }
    }

    if (showCheckinTimePicker) {
        val parts = checkinReminderTime.split(":")
        val state = rememberTimePickerState(
            initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 10,
            initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0,
            is24Hour = true,
        )
        TimePickerDialog(
            onConfirm = {
                val h = state.hour.toString().padStart(2, '0')
                val m = state.minute.toString().padStart(2, '0')
                onCheckinReminderTimeChanged("$h:$m")
                showCheckinTimePicker = false
            },
            onDismiss = { showCheckinTimePicker = false },
        ) {
            TimePicker(
                state = state,
                colors = TimePickerDefaults.colors(
                    clockDialColor = LocalStrakkColors.current.surface2,
                    clockDialSelectedContentColor = MaterialTheme.colorScheme.onBackground,
                    clockDialUnselectedContentColor = LocalStrakkColors.current.textSecondary,
                    selectorColor = MaterialTheme.colorScheme.primary,
                    timeSelectorSelectedContainerColor = MaterialTheme.colorScheme.primary,
                    timeSelectorUnselectedContainerColor = LocalStrakkColors.current.surface2,
                    timeSelectorSelectedContentColor = MaterialTheme.colorScheme.onBackground,
                    timeSelectorUnselectedContentColor = LocalStrakkColors.current.textSecondary,
                ),
            )
        }
    }
}

@Composable
private fun ReminderCard(
    title: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    expandedContent: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onBackground,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = LocalStrakkColors.current.textSecondary,
                    uncheckedTrackColor = LocalStrakkColors.current.surface2,
                    uncheckedBorderColor = LocalStrakkColors.current.divider,
                ),
            )
        }
        expandedContent()
    }
}

@Composable
private fun TimeRow(
    label: String,
    time: String,
    onTimeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = LocalStrakkColors.current.textSecondary,
        )
        TextButton(onClick = onTimeClick) {
            Text(
                text = time,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun DayPicker(
    selectedDay: Int,
    onDaySelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        DAY_LABELS.forEachIndexed { index, label ->
            val isSelected = index == selectedDay
            TextButton(
                onClick = { onDaySelected(index) },
                shape = RoundedCornerShape(8.dp),
                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else LocalStrakkColors.current.surface2,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onBackground else LocalStrakkColors.current.textSecondary,
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun TimePickerDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = "OK", color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel", color = LocalStrakkColors.current.textSecondary)
            }
        },
        containerColor = LocalStrakkColors.current.surface2,
        text = { content() },
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF151720)
@Composable
private fun RemindersStepContentPreview() {
    StrakkTheme {
        RemindersStepContent(
            trackingReminderEnabled = true,
            trackingReminderTime = "17:00",
            checkinReminderEnabled = true,
            checkinReminderDay = 6,
            checkinReminderTime = "10:00",
            onTrackingReminderToggled = {},
            onTrackingReminderTimeChanged = {},
            onCheckinReminderToggled = {},
            onCheckinReminderDayChanged = {},
            onCheckinReminderTimeChanged = {},
        )
    }
}
