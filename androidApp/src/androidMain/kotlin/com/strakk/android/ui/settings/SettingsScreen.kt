package com.strakk.android.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.strakk.android.R
import com.strakk.android.ui.theme.LocalStrakkColors
import com.strakk.android.ui.theme.LocalStrakkTextStyles
import com.strakk.android.ui.theme.StrakkTheme
import com.strakk.shared.presentation.settings.SettingsEvent
import com.strakk.shared.presentation.settings.SettingsUiState
import com.strakk.shared.presentation.settings.SubscriptionDisplay

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
        Text(stringResource(R.string.settings_loading), color = LocalStrakkColors.current.textSecondary)
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
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )

        AccountSection(email = state.email)

        ProSection(
            subscriptionDisplay = state.subscriptionDisplay,
            onUpgrade = { onEvent(SettingsEvent.OnUpgradeTapped) },
            onManage = { onEvent(SettingsEvent.OnManageSubscription) },
            onRestore = { onEvent(SettingsEvent.OnRestorePurchase) },
        )

        GoalsSection(
            protein = state.proteinGoal,
            calorie = state.calorieGoal,
            water = state.waterGoal,
            onProteinChanged = { onEvent(SettingsEvent.OnProteinGoalChanged(it)) },
            onCalorieChanged = { onEvent(SettingsEvent.OnCalorieGoalChanged(it)) },
            onWaterChanged = { onEvent(SettingsEvent.OnWaterGoalChanged(it)) },
        )

        DataSourcesSection()

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
            Text(stringResource(R.string.settings_sign_out), fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun AccountSection(email: String?) {
    SectionCard(title = stringResource(R.string.settings_section_account)) {
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
    SectionCard(title = stringResource(R.string.settings_section_daily_goals)) {
        GoalField(
            label = stringResource(R.string.settings_goal_protein),
            value = protein,
            onValueChange = onProteinChanged,
        )
        GoalField(
            label = stringResource(R.string.settings_goal_calories),
            value = calorie,
            onValueChange = onCalorieChanged,
        )
        GoalField(
            label = stringResource(R.string.settings_goal_water),
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
private fun DataSourcesSection() {
    SectionCard(title = stringResource(R.string.settings_section_data_sources)) {
        Text(
            text = stringResource(R.string.settings_data_sources_body),
            style = MaterialTheme.typography.bodySmall,
            color = LocalStrakkColors.current.textSecondary,
        )
    }
}

// =============================================================================
// PRO section
// =============================================================================

@Composable
private fun ProSection(
    subscriptionDisplay: SubscriptionDisplay,
    onUpgrade: () -> Unit,
    onManage: () -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalStrakkColors.current

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier,
    ) {
        Text(
            text = stringResource(R.string.settings_section_pro),
            style = MaterialTheme.typography.labelSmall,
            color = colors.textTertiary,
        )

        when (subscriptionDisplay) {
            is SubscriptionDisplay.Free -> ProSectionFree(
                onUpgrade = onUpgrade,
                onRestore = onRestore,
            )
            is SubscriptionDisplay.Trial -> ProSectionTrial(
                daysRemaining = subscriptionDisplay.daysRemaining,
                onManage = onManage,
            )
            is SubscriptionDisplay.Active -> ProSectionActive(
                planLabel = subscriptionDisplay.planLabel,
                expiresLabel = subscriptionDisplay.expiresLabel,
                onManage = onManage,
                onRestore = onRestore,
            )
            is SubscriptionDisplay.PaymentFailed -> ProSectionPaymentFailed(
                onFix = onManage,
            )
        }
    }
}

@Composable
private fun ProBadge(modifier: Modifier = Modifier) {
    val colors = LocalStrakkColors.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(colors.accentOrangeFaint)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_pro_badge),
            style = LocalStrakkTextStyles.current.overline,
            color = colors.accentOrange,
        )
    }
}

@Composable
private fun ProSectionFree(onUpgrade: () -> Unit, onRestore: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalStrakkColors.current

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface1)
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_pro_free_title),
            style = LocalStrakkTextStyles.current.bodyBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.settings_pro_free_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
        )
        Button(
            onClick = onUpgrade,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_pro_upgrade),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            )
        }
        TextButton(
            onClick = onRestore,
            modifier = Modifier.fillMaxWidth().height(40.dp),
        ) {
            Text(
                text = stringResource(R.string.paywall_restore),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )
        }
    }
}

@Composable
private fun ProSectionTrial(daysRemaining: Int, onManage: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalStrakkColors.current

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface1)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProBadge()
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.settings_pro_trial_title),
                style = LocalStrakkTextStyles.current.bodyBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Text(
            text = stringResource(R.string.settings_pro_trial_expires, daysRemaining),
            style = MaterialTheme.typography.bodySmall,
            color = colors.warning,
        )
        Button(
            onClick = onManage,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.surface2,
                contentColor = MaterialTheme.colorScheme.onBackground,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_pro_manage),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ProSectionActive(
    planLabel: String,
    expiresLabel: String,
    onManage: () -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalStrakkColors.current

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface1)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProBadge()
            Spacer(Modifier.width(8.dp))
            // Green dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(colors.success),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.settings_pro_active_label, planLabel),
                style = LocalStrakkTextStyles.current.bodyBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Text(
            text = stringResource(R.string.settings_pro_active_renews, expiresLabel),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
        )
        Button(
            onClick = onManage,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.surface2,
                contentColor = MaterialTheme.colorScheme.onBackground,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_pro_manage),
                fontWeight = FontWeight.SemiBold,
            )
        }
        TextButton(
            onClick = onRestore,
            modifier = Modifier.fillMaxWidth().height(40.dp),
        ) {
            Text(
                text = stringResource(R.string.paywall_restore),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )
        }
    }
}

@Composable
private fun ProSectionPaymentFailed(onFix: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalStrakkColors.current

    // Left error border: outer box is the error color, inner box is surface1 with a start offset
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.error),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 3.dp)
                .clip(RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp))
                .background(colors.surface1)
                .padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_pro_payment_failed_title),
                style = LocalStrakkTextStyles.current.bodyBold,
                color = colors.error,
            )
            Text(
                text = stringResource(R.string.settings_pro_payment_failed_body),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
            Button(
                onClick = onFix,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.error,
                    contentColor = Color.White,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_pro_payment_failed_cta),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

// =============================================================================
// Generic section card
// =============================================================================

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
                email = "preview@strakk.app",
                proteinGoal = "150",
                calorieGoal = "2400",
                waterGoal = "2500",
                hevyApiKey = "",
            ),
            snackbar = SnackbarHostState(),
            onEvent = {},
        )
    }
}
