package com.strakk.android.ui.settings

import android.text.format.DateFormat
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.strakk.android.R
import com.strakk.android.ui.components.StrakkDestructiveButton
import com.strakk.android.ui.components.StrakkPrimaryButton
import com.strakk.android.ui.components.StrakkSecondaryButton
import com.strakk.android.ui.components.StrakkTextButton
import com.strakk.android.ui.theme.LocalStrakkColors
import com.strakk.android.ui.theme.LocalStrakkTextStyles
import com.strakk.android.ui.theme.StrakkTheme
import com.strakk.shared.domain.model.DevSubscriptionOverride
import com.strakk.shared.domain.model.SubscriptionPlan
import com.strakk.shared.presentation.settings.SettingsEvent
import com.strakk.shared.presentation.settings.SettingsUiState
import com.strakk.shared.presentation.settings.SubscriptionDisplay
import kotlinx.datetime.LocalDate

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

        if (stringResource(R.string.app_name).contains("Dev")) {
            DevSubscriptionSection(
                selected = state.devSubscriptionOverride,
                onSelected = { onEvent(SettingsEvent.OnDevSubscriptionOverrideSelected(it)) },
            )
        }

        GoalsSection(
            protein = state.proteinGoal,
            calorie = state.calorieGoal,
            fat = state.fatGoal,
            carbs = state.carbGoal,
            water = state.waterGoal,
            onProteinChanged = { onEvent(SettingsEvent.OnProteinGoalChanged(it)) },
            onCalorieChanged = { onEvent(SettingsEvent.OnCalorieGoalChanged(it)) },
            onFatChanged = { onEvent(SettingsEvent.OnFatGoalChanged(it)) },
            onCarbsChanged = { onEvent(SettingsEvent.OnCarbGoalChanged(it)) },
            onWaterChanged = { onEvent(SettingsEvent.OnWaterGoalChanged(it)) },
        )

        DataSourcesSection()

        StrakkDestructiveButton(
            text = stringResource(R.string.settings_sign_out),
            onClick = { onEvent(SettingsEvent.OnSignOut) },
        )

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

@Suppress("LongParameterList")
@Composable
private fun GoalsSection(
    protein: String,
    calorie: String,
    fat: String,
    carbs: String,
    water: String,
    onProteinChanged: (String) -> Unit,
    onCalorieChanged: (String) -> Unit,
    onFatChanged: (String) -> Unit,
    onCarbsChanged: (String) -> Unit,
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
            label = stringResource(R.string.settings_goal_fat),
            value = fat,
            onValueChange = onFatChanged,
        )
        GoalField(
            label = stringResource(R.string.settings_goal_carbs),
            value = carbs,
            onValueChange = onCarbsChanged,
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

@Composable
private fun DevSubscriptionSection(
    selected: DevSubscriptionOverride,
    onSelected: (DevSubscriptionOverride) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val colors = LocalStrakkColors.current

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier,
    ) {
        Text(
            text = stringResource(R.string.settings_dev_subscription_section),
            style = MaterialTheme.typography.labelSmall,
            color = colors.textTertiary,
        )

        Box {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface1)
                    .clickable { expanded = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_dev_subscription_label),
                    style = LocalStrakkTextStyles.current.bodyBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = selected.devLabel(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                DevSubscriptionOverride.entries.forEach { override ->
                    DropdownMenuItem(
                        text = { Text(override.devLabel()) },
                        onClick = {
                            expanded = false
                            onSelected(override)
                        },
                    )
                }
            }
        }
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
            text = stringResource(R.string.settings_section_current_plan),
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
                endsAtLabel = subscriptionDisplay.endsAtLabel,
                onChoosePlan = onUpgrade,
            )
            is SubscriptionDisplay.Active -> ProSectionActive(
                plan = subscriptionDisplay.plan,
                renewalLabel = subscriptionDisplay.renewalLabel,
                canUpgradeToAnnual = subscriptionDisplay.canUpgradeToAnnual,
                onUpgrade = onUpgrade,
                onManage = onManage,
            )
            is SubscriptionDisplay.PaymentFailed -> ProSectionPaymentFailed(
                onFix = onManage,
            )
        }
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
        PlanStatusPill(text = stringResource(R.string.settings_plan_free_badge), color = colors.textSecondary)
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
        StrakkPrimaryButton(
            text = stringResource(R.string.settings_pro_upgrade),
            onClick = onUpgrade,
        )
        StrakkTextButton(
            text = stringResource(R.string.paywall_restore),
            onClick = onRestore,
            emphasized = false,
        )
    }
}

@Composable
private fun ProSectionTrial(
    daysRemaining: Int,
    endsAtLabel: String,
    onChoosePlan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalStrakkColors.current
    val dateLabel = subscriptionDateLabel(endsAtLabel)

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface1)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            PlanStatusPill(text = stringResource(R.string.settings_plan_trial_badge), color = colors.success)
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.settings_pro_trial_remaining, daysRemaining),
                style = LocalStrakkTextStyles.current.caption,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Text(
            text = stringResource(R.string.settings_pro_trial_title),
            style = LocalStrakkTextStyles.current.bodyBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.settings_pro_trial_ends, dateLabel),
            style = MaterialTheme.typography.bodySmall,
            color = colors.warning,
        )
        Text(
            text = stringResource(R.string.settings_pro_trial_body),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
        )
        StrakkPrimaryButton(
            text = stringResource(R.string.settings_pro_trial_choose_plan),
            onClick = onChoosePlan,
        )
    }
}

@Suppress("LongParameterList")
@Composable
private fun ProSectionActive(
    plan: SubscriptionPlan,
    renewalLabel: String,
    canUpgradeToAnnual: Boolean,
    onUpgrade: () -> Unit,
    onManage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalStrakkColors.current
    val dateLabel = subscriptionDateLabel(renewalLabel)
    val planLabel = when (plan) {
        SubscriptionPlan.MONTHLY -> stringResource(R.string.paywall_plan_monthly)
        SubscriptionPlan.ANNUAL -> stringResource(R.string.paywall_plan_annual)
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface1)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            PlanStatusPill(text = stringResource(R.string.settings_plan_active_badge), color = colors.success)
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.settings_pro_badge),
                style = LocalStrakkTextStyles.current.caption,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = stringResource(R.string.settings_pro_active_label, planLabel.lowercase()),
            style = LocalStrakkTextStyles.current.bodyBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.settings_pro_active_renews, dateLabel),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
        )

        if (canUpgradeToAnnual && plan == SubscriptionPlan.MONTHLY) {
            AnnualUpsellCard(onUpgrade = onUpgrade)
        }

        StrakkSecondaryButton(
            text = stringResource(R.string.settings_pro_manage),
            onClick = onManage,
        )
    }
}

@Composable
private fun PlanStatusPill(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            style = LocalStrakkTextStyles.current.overline,
            color = color,
        )
    }
}

@Composable
private fun AnnualUpsellCard(onUpgrade: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalStrakkColors.current

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surface2)
            .padding(12.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_pro_annual_upsell_title),
            style = LocalStrakkTextStyles.current.captionBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.settings_pro_annual_upsell_body),
            style = LocalStrakkTextStyles.current.caption,
            color = colors.textSecondary,
        )
        StrakkTextButton(
            text = stringResource(R.string.settings_pro_annual_upsell_cta),
            onClick = onUpgrade,
            emphasized = false,
            modifier = Modifier.padding(start = 0.dp),
        )
    }
}

@Composable
private fun subscriptionDateLabel(isoDate: String): String {
    val context = LocalContext.current
    return remember(isoDate) {
        runCatching {
            val date = LocalDate.parse(isoDate)
            val cal = java.util.Calendar.getInstance().apply {
                set(date.year, date.monthNumber - 1, date.dayOfMonth)
            }
            DateFormat.getMediumDateFormat(context).format(cal.time)
        }.getOrDefault(isoDate)
    }
}

@Composable
private fun DevSubscriptionOverride.devLabel(): String = stringResource(
    when (this) {
        DevSubscriptionOverride.SERVER -> R.string.settings_dev_subscription_server
        DevSubscriptionOverride.FREE -> R.string.settings_dev_subscription_free
        DevSubscriptionOverride.TRIAL_7_DAYS -> R.string.settings_dev_subscription_trial_7
        DevSubscriptionOverride.TRIAL_1_DAY -> R.string.settings_dev_subscription_trial_1
        DevSubscriptionOverride.EXPIRED -> R.string.settings_dev_subscription_expired
        DevSubscriptionOverride.PRO_MONTHLY -> R.string.settings_dev_subscription_pro_monthly
        DevSubscriptionOverride.PRO_ANNUAL -> R.string.settings_dev_subscription_pro_annual
        DevSubscriptionOverride.PAYMENT_FAILED -> R.string.settings_dev_subscription_payment_failed
    },
)

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
            StrakkDestructiveButton(
                text = stringResource(R.string.settings_pro_payment_failed_cta),
                onClick = onFix,
            )
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
                fatGoal = "70",
                carbGoal = "250",
                waterGoal = "2500",
                hevyApiKey = "",
            ),
            snackbar = SnackbarHostState(),
            onEvent = {},
        )
    }
}
