package com.strakk.android.ui.today

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.strakk.android.R
import com.strakk.android.ui.theme.LocalStrakkColors
import com.strakk.android.ui.theme.StrakkTheme
import com.strakk.shared.domain.model.DailySummary
import com.strakk.shared.domain.model.EntrySource
import com.strakk.shared.domain.model.MealEntry
import com.strakk.shared.domain.model.WaterEntry
import com.strakk.shared.presentation.today.TimelineItem
import com.strakk.shared.presentation.today.TodayEvent
import com.strakk.shared.presentation.today.TodayUiState

// =============================================================================
// Main content
// =============================================================================

@Composable
fun TodayContent(
    uiState: TodayUiState.Ready,
    onEvent: (TodayEvent) -> Unit,
    onNavigateToDraft: () -> Unit,
    onNavigateToQuickAdd: () -> Unit = {},
    onDiscardDraft: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
        ) {
            // Header
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.today_title),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = uiState.dateLabel,
                        style = MaterialTheme.typography.bodyLarge,
                        color = LocalStrakkColors.current.textSecondary,
                    )
                }
            }

            // 4 macro cards
            item {
                Spacer(modifier = Modifier.height(20.dp))
                ProgressSection(
                    summary = uiState.summary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Water section
            item {
                Spacer(modifier = Modifier.height(12.dp))
                WaterRow(
                    summary = uiState.summary,
                    onEvent = onEvent,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Timeline header
            item {
                Spacer(modifier = Modifier.height(28.dp))
                Text(
                    text = stringResource(R.string.today_timeline_header),
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalStrakkColors.current.textTertiary,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Timeline items
            if (uiState.timeline.isEmpty()) {
                item {
                    EmptyTimelinePlaceholder()
                }
            } else {
                items(
                    items = uiState.timeline,
                    key = { item ->
                        when (item) {
                            is TimelineItem.MealContainer -> "meal-${item.meal.id}"
                            is TimelineItem.OrphanEntry -> "entry-${item.entry.id}"
                        }
                    },
                ) { item ->
                    when (item) {
                        is TimelineItem.MealContainer -> {
                            Spacer(modifier = Modifier.height(8.dp))
                            MealContainerCard(
                                meal = item.meal,
                                onDelete = { onEvent(TodayEvent.OnDeleteMeal(item.meal.id)) },
                                onAddEntry = onNavigateToDraft,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItem(),
                            )
                        }
                        is TimelineItem.OrphanEntry -> {
                            Spacer(modifier = Modifier.height(8.dp))
                            OrphanEntryRow(
                                entry = item.entry,
                                onDelete = { onEvent(TodayEvent.OnDeleteOrphanEntry(item.entry.id)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItem(),
                            )
                        }
                    }
                }
            }

            // Bottom padding for sticky bar
            item {
                val bottomPadding = if (uiState.activeDraft != null) 140.dp else 100.dp
                Spacer(modifier = Modifier.height(bottomPadding))
            }
        }

        // Sticky bottom action bar
        val activeDraft = uiState.activeDraft
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
        ) {
            if (activeDraft != null) {
                DraftFloatingBar(
                    draft = activeDraft,
                    onTap = onNavigateToDraft,
                    onAdd = onNavigateToDraft,
                    onFinish = onNavigateToDraft,
                    onDiscard = onDiscardDraft,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                ActionButtonsBar(
                    onNewMeal = onNavigateToDraft,
                    onQuickAdd = onNavigateToQuickAdd,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

// =============================================================================
// Previews
// =============================================================================

@Preview(showBackground = true, backgroundColor = 0xFF151720)
@Composable
private fun TodayContentPreview() {
    StrakkTheme {
        TodayContent(
            uiState = TodayUiState.Ready(
                dateLabel = "Jeu. 24 avril",
                summary = DailySummary(
                    totalProtein = 142.0,
                    totalCalories = 1840.0,
                    totalFat = 54.0,
                    totalCarbs = 210.0,
                    totalWater = 1500,
                    proteinGoal = 160,
                    calorieGoal = 2200,
                    waterGoal = 2500,
                ),
                timeline = listOf(
                    TimelineItem.OrphanEntry(
                        MealEntry(
                            id = "e2",
                            logDate = "2026-04-24",
                            name = "Banane",
                            protein = 1.2,
                            calories = 90.0,
                            fat = null,
                            carbs = 23.0,
                            source = EntrySource.Search,
                            createdAt = "2026-04-24T15:45:00Z",
                        ),
                    ),
                ),
                waterEntries = listOf(
                    WaterEntry(id = "w1", logDate = "2026-04-24", amount = 500, createdAt = ""),
                ),
                activeDraft = null,
            ),
            onEvent = {},
            onNavigateToDraft = {},
        )
    }
}
