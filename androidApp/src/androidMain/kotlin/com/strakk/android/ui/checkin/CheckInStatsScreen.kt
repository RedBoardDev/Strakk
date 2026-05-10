package com.strakk.android.ui.checkin

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.strakk.android.R
import com.strakk.android.ui.components.StrakkEmptyState
import com.strakk.android.ui.components.StrakkLoadingState
import com.strakk.android.ui.theme.LocalStrakkColors
import com.strakk.android.ui.theme.LocalStrakkSpacing
import com.strakk.android.ui.theme.LocalStrakkTextStyles
import com.strakk.android.ui.theme.StrakkTheme
import com.strakk.shared.presentation.checkin.CheckInStatsEvent
import com.strakk.shared.presentation.checkin.CheckInStatsUiState
import com.strakk.shared.presentation.checkin.RegularityInfo
import com.strakk.shared.presentation.checkin.StatsPeriod
import com.strakk.shared.presentation.checkin.TrendInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInStatsScreen(
    state: CheckInStatsUiState,
    onEvent: (CheckInStatsEvent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.checkin_stats_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        when (state) {
            CheckInStatsUiState.Loading -> StrakkLoadingState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )

            is CheckInStatsUiState.Ready -> StatsContent(
                state = state,
                onEvent = onEvent,
                padding = innerPadding,
            )
        }
    }
}

// =============================================================================
// Ready content
// =============================================================================

@Composable
private fun StatsContent(
    state: CheckInStatsUiState.Ready,
    onEvent: (CheckInStatsEvent) -> Unit,
    padding: PaddingValues,
) {
    val spacing = LocalStrakkSpacing.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        PeriodSelector(
            selected = state.selectedPeriod,
            onSelect = { onEvent(CheckInStatsEvent.OnPeriodSelected(it)) },
        )

        if (state.filteredSeries.isEmpty()) {
            StrakkEmptyState(title = stringResource(R.string.checkin_stats_empty))
        } else {
            state.weightTrend?.let { trend ->
                TrendCard(
                    title = stringResource(R.string.checkin_stats_weight_trend),
                    trend = trend,
                    unit = "kg",
                )
            }
            state.waistTrend?.let { trend ->
                TrendCard(
                    title = stringResource(R.string.checkin_stats_waist_trend),
                    trend = trend,
                    unit = "cm",
                )
            }
            RegularityCard(info = state.regularity)
        }

        Spacer(Modifier.height(spacing.lg))
    }
}

// =============================================================================
// Period selector
// =============================================================================

@Suppress("FunctionSignature")
@Composable
private fun PeriodSelector(
    selected: StatsPeriod,
    onSelect: (StatsPeriod) -> Unit,
) {
    val periods = listOf(
        StatsPeriod.FourWeeks to stringResource(R.string.checkin_stats_period_4w),
        StatsPeriod.TwelveWeeks to stringResource(R.string.checkin_stats_period_12w),
        StatsPeriod.All to stringResource(R.string.checkin_stats_period_all),
    )
    val colors = LocalStrakkColors.current
    val textStyles = LocalStrakkTextStyles.current

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        periods.forEach { (period, label) ->
            val isSelected = period == selected
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) colors.accentOrange else colors.surface2)
                    .clickable { onSelect(period) }
                    .padding(vertical = 10.dp),
            ) {
                Text(
                    text = label,
                    style = if (isSelected) textStyles.captionBold else textStyles.caption,
                    color = if (isSelected) androidx.compose.ui.graphics.Color.White else colors.textSecondary,
                )
            }
        }
    }
}

// =============================================================================
// Trend card
// =============================================================================

@Suppress("FunctionSignature")
@Composable
private fun TrendCard(
    title: String,
    trend: TrendInfo,
    unit: String,
) {
    val colors = LocalStrakkColors.current
    val textStyles = LocalStrakkTextStyles.current
    val spacing = LocalStrakkSpacing.current

    val isPositive = trend.delta > 0
    val deltaColor = if (trend.delta < 0) colors.success else colors.warning
    val sign = if (isPositive) "+" else ""
    val deltaText = String.format(java.util.Locale.getDefault(), "%s%.1f %s", sign, trend.delta, unit)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface1)
            .padding(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        Text(text = title.uppercase(), style = textStyles.overline, color = colors.textTertiary)
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = deltaText,
                style = MaterialTheme.typography.headlineMedium,
                color = deltaColor,
            )
            Text(
                text = stringResource(R.string.checkin_stats_over_n_weeks, trend.weeks),
                style = textStyles.caption,
                color = colors.textTertiary,
            )
        }
    }
}

// =============================================================================
// Regularity card
// =============================================================================

@Composable
private fun RegularityCard(info: RegularityInfo) {
    val colors = LocalStrakkColors.current
    val textStyles = LocalStrakkTextStyles.current
    val spacing = LocalStrakkSpacing.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface1)
            .padding(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        Text(
            text = stringResource(R.string.checkin_stats_regularity).uppercase(),
            style = textStyles.overline,
            color = colors.textTertiary,
        )
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "${info.percentage}%",
                style = MaterialTheme.typography.headlineMedium,
                color = colors.accentOrange,
            )
            Text(
                text = stringResource(
                    R.string.checkin_stats_regularity_value,
                    info.checkInCount,
                    info.totalWeeks,
                ),
                style = textStyles.caption,
                color = colors.textTertiary,
            )
        }
    }
}

// =============================================================================
// Preview
// =============================================================================

@Suppress("UnusedPrivateMember")
@Preview(showBackground = true, backgroundColor = 0xFF050918)
@Composable
private fun CheckInStatsScreenPreview() {
    StrakkTheme {
        CheckInStatsScreen(
            state = CheckInStatsUiState.Ready(
                selectedPeriod = StatsPeriod.TwelveWeeks,
                series = emptyList(),
                filteredSeries = emptyList(),
                weightTrend = TrendInfo(delta = -2.5, weeks = 12),
                waistTrend = TrendInfo(delta = -3.0, weeks = 12),
                regularity = RegularityInfo(checkInCount = 8, totalWeeks = 12, percentage = 66),
            ),
            onEvent = {},
            onNavigateBack = {},
        )
    }
}
