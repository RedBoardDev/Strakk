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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.strakk.android.R
import com.strakk.android.ui.components.StrakkEmptyState
import com.strakk.android.ui.components.StrakkLoadingState
import com.strakk.android.ui.theme.LocalStrakkColors
import com.strakk.android.ui.theme.LocalStrakkSpacing
import com.strakk.android.ui.theme.LocalStrakkTextStyles
import com.strakk.android.ui.theme.StrakkTheme
import com.strakk.shared.domain.model.CheckInListItem
import com.strakk.shared.presentation.checkin.CheckInListEvent
import com.strakk.shared.presentation.checkin.CheckInListUiState
import com.strakk.shared.presentation.checkin.QuickStats

private const val EMPTY_STATE_MAX_SIZE_FRACTION = 0.7f

@Suppress("FunctionSignature")
@Composable
fun CheckInListScreen(
    state: CheckInListUiState,
    onEvent: (CheckInListEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalStrakkColors.current

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onEvent(CheckInListEvent.OnCreateNew) },
                containerColor = colors.accentOrange,
                contentColor = androidx.compose.ui.graphics.Color.White,
                shape = CircleShape,
            ) {
                Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.checkin_list_new))
            }
        },
    ) { innerPadding ->
        when (state) {
            CheckInListUiState.Loading -> StrakkLoadingState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )

            is CheckInListUiState.Ready -> ReadyContent(
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
private fun ReadyContent(
    state: CheckInListUiState.Ready,
    onEvent: (CheckInListEvent) -> Unit,
    padding: PaddingValues,
) {
    val spacing = LocalStrakkSpacing.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(
            horizontal = spacing.md,
            vertical = spacing.md,
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        item(key = "header") {
            ListHeader(
                onStatsClick = { onEvent(CheckInListEvent.OnOpenStats) },
            )
            Spacer(Modifier.height(spacing.xs))
        }

        state.quickStats?.let { qs ->
            item(key = "quick_stats") {
                QuickStatsCard(stats = qs)
                Spacer(Modifier.height(spacing.sm))
            }
        }

        if (state.checkIns.isEmpty()) {
            item(key = "empty") {
                StrakkEmptyState(
                    title = stringResource(R.string.checkin_list_empty_title),
                    description = stringResource(R.string.checkin_list_empty_desc),
                    icon = Icons.Outlined.Assignment,
                    modifier = Modifier.fillParentMaxSize(EMPTY_STATE_MAX_SIZE_FRACTION),
                )
            }
        } else {
            items(state.checkIns, key = { it.id }) { item ->
                CheckInListItemCard(
                    item = item,
                    onClick = { onEvent(CheckInListEvent.OnOpenDetail(item.id)) },
                )
            }

            if (state.hiddenCount > 0) {
                item(key = "unlock") {
                    UnlockHistoryRow(
                        hiddenCount = state.hiddenCount,
                        onClick = { onEvent(CheckInListEvent.OnUnlockHistory) },
                    )
                }
            }
        }
    }
}

// =============================================================================
// Header
// =============================================================================

@Composable
private fun ListHeader(onStatsClick: () -> Unit) {
    val colors = LocalStrakkColors.current
    val textStyles = LocalStrakkTextStyles.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(R.string.checkin_list_title),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onStatsClick) {
            Icon(
                imageVector = Icons.Outlined.BarChart,
                contentDescription = stringResource(R.string.checkin_list_stats_btn),
                tint = colors.accentOrange,
            )
        }
    }
}

// =============================================================================
// Quick stats strip
// =============================================================================

@Suppress("FunctionSignature")
@Composable
private fun QuickStatsCard(stats: QuickStats) {
    val colors = LocalStrakkColors.current
    val textStyles = LocalStrakkTextStyles.current
    val spacing = LocalStrakkSpacing.current

    Row(
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface1)
            .padding(horizontal = spacing.md, vertical = spacing.sm),
    ) {
        if (stats.lastWeight != null) {
            QuickStatItem(
                label = stringResource(R.string.checkin_stats_weight_trend),
                value = String.format(java.util.Locale.getDefault(), "%.1f kg", stats.lastWeight),
                delta = stats.weightDelta,
                modifier = Modifier.weight(1f),
            )
        }
        if (stats.lastAvgArm != null) {
            QuickStatItem(
                label = stringResource(R.string.checkin_quick_stat_arm),
                value = String.format(java.util.Locale.getDefault(), "%.1f cm", stats.lastAvgArm),
                delta = stats.armDelta,
                modifier = Modifier.weight(1f),
            )
        }
        if (stats.lastWaist != null) {
            QuickStatItem(
                label = stringResource(R.string.checkin_stats_waist_trend),
                value = String.format(java.util.Locale.getDefault(), "%.1f cm", stats.lastWaist),
                delta = stats.waistDelta,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Suppress("FunctionSignature")
@Composable
private fun QuickStatItem(
    label: String,
    value: String,
    delta: Double?,
    modifier: Modifier = Modifier,
) {
    val colors = LocalStrakkColors.current
    val textStyles = LocalStrakkTextStyles.current

    Column(modifier = modifier) {
        Text(text = label, style = textStyles.overline, color = colors.textTertiary)
        Text(text = value, style = textStyles.bodyBold, color = colors.textPrimary)
        if (delta != null && delta != 0.0) {
            val sign = if (delta > 0) "+" else ""
            val deltaColor = if (delta < 0) colors.success else colors.warning
            Text(
                text = String.format(java.util.Locale.getDefault(), "%s%.1f", sign, delta),
                style = textStyles.caption,
                color = deltaColor,
            )
        }
    }
}

// =============================================================================
// List item card
// =============================================================================

@Suppress("FunctionSignature")
@Composable
private fun CheckInListItemCard(
    item: CheckInListItem,
    onClick: () -> Unit,
) {
    val colors = LocalStrakkColors.current
    val textStyles = LocalStrakkTextStyles.current
    val spacing = LocalStrakkSpacing.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface1)
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.md, vertical = spacing.sm),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.weekLabel,
                style = textStyles.bodyBold,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (item.weight != null) {
                    Text(
                        text = stringResource(
                            R.string.checkin_list_item_weight,
                            String.format(java.util.Locale.getDefault(), "%.1f", item.weight),
                        ),
                        style = textStyles.caption,
                        color = colors.textSecondary,
                    )
                }
                if (item.photoCount > 0) {
                    Text(
                        text = stringResource(R.string.checkin_list_item_photos, item.photoCount),
                        style = textStyles.caption,
                        color = colors.textTertiary,
                    )
                }
                if (item.hasAiSummary) {
                    AiSummaryBadge()
                }
            }
        }
    }
}

@Composable
private fun AiSummaryBadge() {
    val colors = LocalStrakkColors.current
    val textStyles = LocalStrakkTextStyles.current

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(colors.accentOrangeFaint)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(text = "AI", style = textStyles.overline, color = colors.accentOrange)
    }
}

// =============================================================================
// Unlock history row
// =============================================================================

@Suppress("FunctionSignature")
@Composable
private fun UnlockHistoryRow(
    hiddenCount: Int,
    onClick: () -> Unit,
) {
    val colors = LocalStrakkColors.current
    val textStyles = LocalStrakkTextStyles.current
    val spacing = LocalStrakkSpacing.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface2)
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.md, vertical = spacing.sm),
    ) {
        Icon(
            imageVector = Icons.Outlined.Lock,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(spacing.xs))
        Text(
            text = stringResource(R.string.checkin_list_hidden, hiddenCount),
            style = textStyles.caption,
            color = colors.textSecondary,
            modifier = Modifier.weight(1f),
        )
    }
}

// =============================================================================
// Preview
// =============================================================================

@Suppress("UnusedPrivateMember")
@Preview(showBackground = true, backgroundColor = 0xFF050918)
@Composable
private fun CheckInListScreenEmptyPreview() {
    StrakkTheme {
        CheckInListScreen(
            state = CheckInListUiState.Ready(
                checkIns = emptyList(),
                quickStats = null,
                hiddenCount = 0,
            ),
            onEvent = {},
        )
    }
}

@Suppress("UnusedPrivateMember")
@Preview(showBackground = true, backgroundColor = 0xFF050918)
@Composable
private fun CheckInListScreenReadyPreview() {
    StrakkTheme {
        CheckInListScreen(
            state = CheckInListUiState.Ready(
                checkIns = listOf(
                    CheckInListItem(
                        id = "1",
                        weekLabel = "2024-W12",
                        weight = 75.2,
                        photoCount = 2,
                        hasAiSummary = true,
                        createdAt = "2024-03-18",
                    ),
                    CheckInListItem(
                        id = "2",
                        weekLabel = "2024-W11",
                        weight = 76.0,
                        photoCount = 0,
                        hasAiSummary = false,
                        createdAt = "2024-03-11",
                    ),
                ),
                quickStats = QuickStats(
                    lastWeight = 75.2,
                    weightDelta = -0.8,
                    lastAvgArm = 32.5,
                    armDelta = 0.3,
                    lastWaist = 81.0,
                    waistDelta = -1.2,
                ),
                hiddenCount = 3,
            ),
            onEvent = {},
        )
    }
}
