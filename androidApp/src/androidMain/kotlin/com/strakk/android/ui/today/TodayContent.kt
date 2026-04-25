package com.strakk.android.ui.today

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.TextSnippet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.strakk.android.ui.theme.LocalStrakkColors
import com.strakk.android.ui.theme.StrakkTheme
import com.strakk.shared.domain.model.ActiveMealDraft
import com.strakk.shared.domain.model.DailySummary
import com.strakk.shared.domain.model.DraftItem
import com.strakk.shared.domain.model.EntrySource
import com.strakk.shared.domain.model.Meal
import com.strakk.shared.domain.model.MealEntry
import com.strakk.shared.domain.model.WaterEntry
import com.strakk.shared.presentation.today.TimelineItem
import com.strakk.shared.presentation.today.TodayEvent
import com.strakk.shared.presentation.today.TodayUiState

// =============================================================================
// Main content
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
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
                        text = "Today",
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
                    text = "TIMELINE",
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

            // Padding de bas pour la barre sticky
            item {
                val bottomPadding = if (uiState.activeDraft != null) 140.dp else 100.dp
                Spacer(modifier = Modifier.height(bottomPadding))
            }
        }

        // Barre d'actions sticky en bas — 2 boutons ou DraftBar
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
// Barre 2 boutons sticky (remplace FAB + MiniPickerContent)
// =============================================================================

@Composable
private fun ActionButtonsBar(
    onNewMeal: () -> Unit,
    onQuickAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalStrakkColors.current

    Surface(
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
        modifier = modifier,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            // Bouton Repas
            Button(
                onClick = onNewMeal,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.surface2,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Restaurant,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "Repas",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                )
            }

            // Bouton Rapide
            Button(
                onClick = onQuickAdd,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Bolt,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "Rapide",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                )
            }
        }
    }
}

// =============================================================================
// Meal container card (expandable)
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MealContainerCard(
    meal: Meal,
    onDelete: () -> Unit,
    onAddEntry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable(meal.id) { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState()

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onDelete()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Surface(
                color = LocalStrakkColors.current.error.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Supprimer",
                        tint = LocalStrakkColors.current.error,
                    )
                }
            }
        },
        enableDismissFromStartToEnd = false,
        modifier = modifier,
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(animationSpec = tween(200)),
        ) {
            Column {
                // Header row — tap to expand
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = meal.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        val totalKcal = meal.entries.sumOf { it.calories }.toInt()
                        val totalProtein = meal.entries.sumOf { it.protein }.toInt()
                        Text(
                            text = buildAnnotatedString {
                                withStyle(SpanStyle(color = LocalStrakkColors.current.textSecondary)) {
                                    append("${meal.entries.size} items · $totalKcal kcal · ")
                                }
                                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                                    append("${totalProtein}g prot")
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Icon(
                        imageVector = if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                        contentDescription = if (expanded) "Réduire" else "Développer",
                        tint = LocalStrakkColors.current.textTertiary,
                        modifier = Modifier.size(20.dp),
                    )
                }

                // Expandable entries list
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically(animationSpec = tween(200)),
                    exit = shrinkVertically(animationSpec = tween(200)),
                ) {
                    Column(modifier = Modifier.padding(bottom = 8.dp)) {
                        meal.entries.forEach { entry ->
                            EntryInMealRow(
                                entry = entry,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                            )
                        }
                        TextButton(
                            onClick = onAddEntry,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.size(4.dp))
                            Text(
                                text = "Ajouter un item",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// Entry row inside a meal container
// =============================================================================

@Composable
private fun EntryInMealRow(
    entry: MealEntry,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        SourceBadge(
            source = entry.source,
            modifier = Modifier.padding(end = 8.dp),
        )
        Text(
            text = entry.name ?: "—",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                    append("${entry.protein.toInt()}g")
                }
                withStyle(SpanStyle(color = LocalStrakkColors.current.textSecondary)) {
                    append(" · ${entry.calories.toInt()} kcal")
                }
            },
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

// =============================================================================
// Orphan entry row (quick-add, flat)
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrphanEntryRow(
    entry: MealEntry,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dismissState = rememberSwipeToDismissBoxState()

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onDelete()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Surface(
                color = LocalStrakkColors.current.error.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Supprimer",
                        tint = LocalStrakkColors.current.error,
                    )
                }
            }
        },
        enableDismissFromStartToEnd = false,
        modifier = modifier,
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                SourceBadge(
                    source = entry.source,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text(
                    text = entry.name ?: "—",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                            append("${entry.protein.toInt()}g")
                        }
                        withStyle(SpanStyle(color = LocalStrakkColors.current.textSecondary)) {
                            append(" · ${entry.calories.toInt()} kcal")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

// =============================================================================
// Source badge
// =============================================================================

@Composable
private fun SourceBadge(
    source: EntrySource,
    modifier: Modifier = Modifier,
) {
    val icon = when (source) {
        EntrySource.PhotoAi -> Icons.Outlined.CameraAlt
        EntrySource.Barcode -> Icons.Outlined.QrCodeScanner
        EntrySource.Manual -> Icons.Outlined.Edit
        EntrySource.Search, EntrySource.Frequent -> Icons.Outlined.Search
        EntrySource.TextAi -> Icons.Outlined.TextSnippet
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(LocalStrakkColors.current.surface2),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = LocalStrakkColors.current.textTertiary,
            modifier = Modifier.size(13.dp),
        )
    }
}

// =============================================================================
// Floating draft bar
// =============================================================================

@Composable
private fun DraftFloatingBar(
    draft: ActiveMealDraft,
    onTap: () -> Unit,
    onAdd: () -> Unit,
    onFinish: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val resolvedCount = draft.items.count { it is DraftItem.Resolved }
    val pendingCount = draft.items.size - resolvedCount
    val totalKcal = draft.items
        .filterIsInstance<DraftItem.Resolved>()
        .sumOf { it.entry.calories }
        .toInt()
    val isEmpty = draft.items.isEmpty()

    val itemsLabel = if (isEmpty) {
        "Aucun item · ajoute pour commencer"
    } else buildString {
        append("$resolvedCount items")
        if (pendingCount > 0) append(" + $pendingCount en attente")
        append(" · $totalKcal kcal")
    }

    Surface(
        onClick = onTap,
        color = LocalStrakkColors.current.surface2,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = draft.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = itemsLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalStrakkColors.current.textSecondary,
                )
            }
            Surface(
                onClick = onAdd,
                shape = RoundedCornerShape(8.dp),
                color = LocalStrakkColors.current.surface3,
            ) {
                Text(
                    text = "+ Ajouter",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
            Spacer(modifier = Modifier.size(8.dp))
            Surface(
                onClick = if (isEmpty) onDiscard else onFinish,
                shape = RoundedCornerShape(8.dp),
                color = if (isEmpty) {
                    LocalStrakkColors.current.surface3
                } else {
                    MaterialTheme.colorScheme.primary
                },
            ) {
                Text(
                    text = if (isEmpty) "Annuler" else "Terminer",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isEmpty) {
                        LocalStrakkColors.current.textSecondary
                    } else {
                        MaterialTheme.colorScheme.onPrimary
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

// =============================================================================
// Empty placeholder
// =============================================================================

@Composable
private fun EmptyTimelinePlaceholder(modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        border = BorderStroke(width = 1.dp, color = LocalStrakkColors.current.divider),
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
        ) {
            Text(
                text = "Rien enregistré aujourd'hui",
                style = MaterialTheme.typography.bodyLarge,
                color = LocalStrakkColors.current.textSecondary,
            )
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
