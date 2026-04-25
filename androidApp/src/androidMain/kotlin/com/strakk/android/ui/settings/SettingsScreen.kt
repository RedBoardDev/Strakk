package com.strakk.android.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.strakk.android.ui.theme.LocalStrakkColors
import com.strakk.android.ui.theme.StrakkTheme
import com.strakk.shared.presentation.settings.SettingsEvent
import com.strakk.shared.presentation.settings.SettingsUiState

private val DAY_LABELS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    snackbar: SnackbarHostState,
    onEvent: (SettingsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) { Snackbar(it) } },
    ) { innerPadding ->
        when (state) {
            is SettingsUiState.Loading -> LoadingView(innerPadding)
            is SettingsUiState.Ready -> ReadyView(state, onEvent, innerPadding)
        }
    }
}

@Composable
private fun LoadingView(padding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Loading…", color = LocalStrakkColors.current.textSecondary)
    }
}

@Composable
private fun ReadyView(
    state: SettingsUiState.Ready,
    onEvent: (SettingsEvent) -> Unit,
    padding: PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )

        AccountSection(email = state.email)

        GoalsSection(
            protein = state.proteinGoal,
            calorie = state.calorieGoal,
            water = state.waterGoal,
            onProteinChanged = { onEvent(SettingsEvent.OnProteinGoalChanged(it)) },
            onCalorieChanged = { onEvent(SettingsEvent.OnCalorieGoalChanged(it)) },
            onWaterChanged = { onEvent(SettingsEvent.OnWaterGoalChanged(it)) },
        )

        TrackingReminderSection(
            enabled = state.trackingReminderEnabled,
            time = state.trackingReminderTime,
            onEnabledChanged = { onEvent(SettingsEvent.OnTrackingReminderEnabledChanged(it)) },
            onTimeChanged = { onEvent(SettingsEvent.OnTrackingReminderTimeChanged(it)) },
        )

        CheckinReminderSection(
            enabled = state.checkinReminderEnabled,
            day = state.checkinReminderDay,
            time = state.checkinReminderTime,
            onEnabledChanged = { onEvent(SettingsEvent.OnCheckinReminderEnabledChanged(it)) },
            onDayChanged = { onEvent(SettingsEvent.OnCheckinReminderDayChanged(it)) },
            onTimeChanged = { onEvent(SettingsEvent.OnCheckinReminderTimeChanged(it)) },
        )

        Button(
            onClick = { onEvent(SettingsEvent.OnSignOut) },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
        ) {
            Text("Sign out", fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun AccountSection(email: String?) {
    SectionCard(title = "ACCOUNT") {
        Text(
            text = email ?: "—",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun GoalsSection(
    protein: String,
    calorie: String,
    water: String,
    onProteinChanged: (String) -> Unit,
    onCalorieChanged: (String) -> Unit,
    onWaterChanged: (String) -> Unit,
) {
    SectionCard(title = "DAILY GOALS") {
        GoalField(
            label = "Protein (g)",
            value = protein,
            onValueChange = onProteinChanged,
        )
        GoalField(
            label = "Calories (kcal)",
            value = calorie,
            onValueChange = onCalorieChanged,
        )
        GoalField(
            label = "Water (mL)",
            value = water,
            onValueChange = onWaterChanged,
            imeAction = ImeAction.Done,
        )
    }
}

@Composable
private fun GoalField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    imeAction: ImeAction = ImeAction.Next,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = imeAction,
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = LocalStrakkColors.current.divider,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun TrackingReminderSection(
    enabled: Boolean,
    time: String,
    onEnabledChanged: (Boolean) -> Unit,
    onTimeChanged: (String) -> Unit,
) {
    SectionCard(title = "DAILY TRACKING REMINDER") {
        ToggleRow(
            label = "Remind me to log",
            enabled = enabled,
            onEnabledChanged = onEnabledChanged,
        )
        if (enabled) {
            OutlinedTextField(
                value = time,
                onValueChange = onTimeChanged,
                label = { Text("Time (HH:mm)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CheckinReminderSection(
    enabled: Boolean,
    day: Int,
    time: String,
    onEnabledChanged: (Boolean) -> Unit,
    onDayChanged: (Int) -> Unit,
    onTimeChanged: (String) -> Unit,
) {
    SectionCard(title = "WEEKLY CHECK-IN REMINDER") {
        ToggleRow(
            label = "Remind me for check-in",
            enabled = enabled,
            onEnabledChanged = onEnabledChanged,
        )
        if (enabled) {
            DayPicker(selected = day, onSelected = onDayChanged)
            OutlinedTextField(
                value = time,
                onValueChange = onTimeChanged,
                label = { Text("Time (HH:mm)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun DayPicker(selected: Int, onSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        DAY_LABELS.forEachIndexed { index, label ->
            val isSelected = index == selected
            Button(
                onClick = { onSelected(index) },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        LocalStrakkColors.current.surface2
                    },
                    contentColor = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                ),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                modifier = Modifier.height(40.dp),
            ) {
                Text(label, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    enabled: Boolean,
    onEnabledChanged: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Switch(checked = enabled, onCheckedChange = onEnabledChanged)
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = LocalStrakkColors.current.textTertiary,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            content()
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF151720)
@Composable
private fun SettingsScreenPreview() {
    StrakkTheme {
        SettingsScreen(
            state = SettingsUiState.Ready(
                email = "thomas@strakk.app",
                proteinGoal = "150",
                calorieGoal = "2400",
                waterGoal = "2500",
                trackingReminderEnabled = true,
                trackingReminderTime = "17:00",
                checkinReminderEnabled = true,
                checkinReminderDay = 6,
                checkinReminderTime = "10:00",
                hevyApiKey = "",
            ),
            snackbar = SnackbarHostState(),
            onEvent = {},
        )
    }
}
