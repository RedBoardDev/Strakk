package com.strakk.android.ui.paywall

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Compare
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.strakk.android.R
import com.strakk.android.ui.components.StrakkCloseButton
import com.strakk.android.ui.components.StrakkPrimaryButton
import com.strakk.android.ui.components.StrakkTextButton
import com.strakk.android.ui.theme.LocalStrakkColors
import com.strakk.android.ui.theme.LocalStrakkTextStyles
import com.strakk.android.ui.theme.StrakkTheme
import com.strakk.shared.domain.model.Feature
import com.strakk.shared.domain.model.FeatureMetadata
import com.strakk.shared.domain.model.FeatureRegistry
import com.strakk.shared.domain.model.PaywallOfferPrices
import com.strakk.shared.domain.model.SubscriptionPlan
import com.strakk.shared.presentation.paywall.PaywallEvent
import com.strakk.shared.presentation.paywall.PaywallUiState

@Composable
fun PaywallScreen(
    uiState: PaywallUiState,
    snackbarHostState: SnackbarHostState,
    onEvent: (PaywallEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) { Snackbar(it) } },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 8.dp, top = 4.dp),
                contentAlignment = Alignment.TopEnd,
            ) {
                StrakkCloseButton(onClick = { onEvent(PaywallEvent.OnDismiss) })
            }
            PaywallBody(uiState = uiState, onEvent = onEvent)
        }
    }
}

@Composable
private fun PaywallBody(uiState: PaywallUiState, onEvent: (PaywallEvent) -> Unit) {
    val colors = LocalStrakkColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Text(
            text = stringResource(R.string.paywall_label),
            style = LocalStrakkTextStyles.current.overline,
            color = colors.accentOrangeLight,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.paywall_headline),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.paywall_subheadline),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
        )
        Spacer(Modifier.height(24.dp))
        uiState.features.forEach { featureInfo ->
            FeatureRow(
                featureInfo = featureInfo,
                isHighlighted = featureInfo.feature == uiState.highlightedFeature,
            )
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(16.dp))
        PlanToggle(
            selectedPlan = uiState.selectedPlan,
            onPlanSelected = { onEvent(PaywallEvent.OnPlanSelected(it)) },
        )
        Spacer(Modifier.height(12.dp))
        PriceCard(
            selectedPlan = uiState.selectedPlan,
            offerPrices = uiState.offerPrices,
        )
        Spacer(Modifier.height(24.dp))
        PaywallCtaSection(uiState = uiState, onEvent = onEvent)
    }
}

@Composable
private fun PaywallCtaSection(uiState: PaywallUiState, onEvent: (PaywallEvent) -> Unit) {
    val colors = LocalStrakkColors.current
    val ctaText = when {
        uiState.selectedPlanIsCurrent -> stringResource(R.string.paywall_cta_current_plan)
        uiState.isTrial -> stringResource(R.string.paywall_cta_choose_plan)
        else -> stringResource(R.string.paywall_cta)
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        StrakkPrimaryButton(
            text = ctaText,
            onClick = { onEvent(PaywallEvent.OnSubscribeTapped) },
            enabled = !uiState.selectedPlanIsCurrent,
            loading = uiState.isProcessing,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.paywall_footer_cancel),
            style = LocalStrakkTextStyles.current.caption,
            color = colors.textTertiary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.paywall_footer_secure),
            style = LocalStrakkTextStyles.current.caption,
            color = colors.textTertiary,
        )
        Spacer(Modifier.height(16.dp))
        StrakkTextButton(
            text = stringResource(R.string.paywall_restore),
            onClick = { onEvent(PaywallEvent.OnRestoreTapped) },
            emphasized = false,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Suppress("FunctionSignature")
@Composable
private fun FeatureRow(
    featureInfo: FeatureMetadata,
    isHighlighted: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LocalStrakkColors.current
    val icon = iconForAndroid(featureInfo.iconAndroid)

    val rowModifier = if (isHighlighted) {
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surface1)
            .border(
                width = 3.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp),
            )
            .padding(12.dp)
    } else {
        modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
    }

    Row(
        verticalAlignment = Alignment.Top,
        modifier = rowModifier,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = resolveFeatureTitle(featureInfo.titleKey),
                style = LocalStrakkTextStyles.current.bodyBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = resolveFeatureDescription(featureInfo.descriptionKey),
                style = LocalStrakkTextStyles.current.caption,
                color = colors.textSecondary,
            )
        }
    }
}

@Composable
private fun PlanToggle(
    selectedPlan: SubscriptionPlan,
    onPlanSelected: (SubscriptionPlan) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalStrakkColors.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface1),
    ) {
        SubscriptionPlan.entries.forEach { plan ->
            val isSelected = plan == selectedPlan
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) colors.surface2 else Color.Transparent)
                    .clickable { onPlanSelected(plan) }
                    .padding(vertical = 12.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = when (plan) {
                            SubscriptionPlan.MONTHLY -> stringResource(R.string.paywall_plan_monthly)
                            SubscriptionPlan.ANNUAL -> stringResource(R.string.paywall_plan_annual)
                        },
                        style = LocalStrakkTextStyles.current.bodyBold,
                        color = if (isSelected) MaterialTheme.colorScheme.onBackground else colors.textSecondary,
                    )
                    if (plan == SubscriptionPlan.ANNUAL) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.paywall_plan_popular),
                            style = LocalStrakkTextStyles.current.overline,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Suppress("FunctionSignature")
@Composable
private fun PriceCard(
    selectedPlan: SubscriptionPlan,
    offerPrices: PaywallOfferPrices,
    modifier: Modifier = Modifier,
) {
    val colors = LocalStrakkColors.current
    val placeholder = stringResource(R.string.paywall_price_placeholder)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface1)
            .padding(16.dp),
    ) {
        when (selectedPlan) {
            SubscriptionPlan.ANNUAL -> {
                Column {
                    Text(
                        text = offerPrices.annualFormatted ?: placeholder,
                        style = LocalStrakkTextStyles.current.bodyBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(4.dp))
                    val perMo = offerPrices.annualPricePerMonthFormatted
                    if (!perMo.isNullOrBlank()) {
                        Text(
                            text = stringResource(R.string.paywall_annual_per_month_line, perMo),
                            style = LocalStrakkTextStyles.current.caption,
                            color = colors.textSecondary,
                        )
                    }
                }
            }
            SubscriptionPlan.MONTHLY -> {
                Column {
                    Text(
                        text = offerPrices.monthlyFormatted ?: placeholder,
                        style = LocalStrakkTextStyles.current.bodyBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }
    }
}

internal fun iconForAndroid(iconKey: String): ImageVector = when (iconKey) {
    "CameraAlt" -> Icons.Outlined.CameraAlt
    "TextFields" -> Icons.Outlined.TextFields
    "BarChart" -> Icons.Outlined.BarChart
    "MonitorHeart" -> Icons.Outlined.MonitorHeart
    "History" -> Icons.Outlined.History
    "Compare" -> Icons.Outlined.Compare
    "Upload" -> Icons.Outlined.Upload
    else -> Icons.Outlined.BarChart
}

@Composable
internal fun resolveFeatureTitle(titleKey: String): String = stringResource(
    when (titleKey) {
        "feature_ai_photo_title" -> R.string.feature_ai_photo_title
        "feature_ai_text_title" -> R.string.feature_ai_text_title
        "feature_ai_summary_title" -> R.string.feature_ai_summary_title
        "feature_health_sync_title" -> R.string.feature_health_sync_title
        "feature_unlimited_history_title" -> R.string.feature_unlimited_history_title
        "feature_photo_comparison_title" -> R.string.feature_photo_comparison_title
        "feature_hevy_export_title" -> R.string.feature_hevy_export_title
        else -> R.string.feature_ai_photo_title
    },
)

@Composable
internal fun resolveFeatureDescription(descriptionKey: String): String = stringResource(
    when (descriptionKey) {
        "feature_ai_photo_description" -> R.string.feature_ai_photo_description
        "feature_ai_text_description" -> R.string.feature_ai_text_description
        "feature_ai_summary_description" -> R.string.feature_ai_summary_description
        "feature_health_sync_description" -> R.string.feature_health_sync_description
        "feature_unlimited_history_description" -> R.string.feature_unlimited_history_description
        "feature_photo_comparison_description" -> R.string.feature_photo_comparison_description
        "feature_hevy_export_description" -> R.string.feature_hevy_export_description
        else -> R.string.feature_ai_photo_description
    },
)

@Preview(showBackground = true, backgroundColor = 0xFF050918)
@Composable
internal fun PaywallScreenPreview() {
    StrakkTheme {
        PaywallScreen(
            uiState = PaywallUiState(
                features = FeatureRegistry.all(),
                highlightedFeature = Feature.AI_PHOTO_ANALYSIS,
                selectedPlan = SubscriptionPlan.ANNUAL,
                offerPrices = PaywallOfferPrices(),
            ),
            snackbarHostState = SnackbarHostState(),
            onEvent = {},
        )
    }
}
