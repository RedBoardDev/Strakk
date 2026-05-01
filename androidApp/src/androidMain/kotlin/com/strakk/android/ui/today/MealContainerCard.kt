package com.strakk.android.ui.today

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.outlined.TextSnippet
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.strakk.android.R
import com.strakk.android.ui.theme.LocalStrakkColors
import com.strakk.shared.domain.model.EntrySource
import com.strakk.shared.domain.model.Meal
import com.strakk.shared.domain.model.MealEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MealContainerCard(
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
                        contentDescription = stringResource(R.string.common_delete),
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    val totalKcal = remember(meal.entries) { meal.entries.sumOf { it.calories }.toInt() }
                    val totalProtein = remember(meal.entries) { meal.entries.sumOf { it.protein }.toInt() }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = meal.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
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
                        contentDescription = if (expanded) stringResource(R.string.meal_card_collapse_cd) else stringResource(R.string.meal_card_expand_cd),
                        tint = LocalStrakkColors.current.textTertiary,
                        modifier = Modifier.size(20.dp),
                    )
                }

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
                                text = stringResource(R.string.meal_card_add_item),
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

@Composable
internal fun EntryInMealRow(
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

@Composable
internal fun SourceBadge(
    source: EntrySource,
    modifier: Modifier = Modifier,
) {
    val (icon, description) = when (source) {
        EntrySource.PhotoAi -> Icons.Outlined.CameraAlt to "Ajouté par photo"
        EntrySource.Barcode -> Icons.Outlined.QrCodeScanner to "Ajouté par code-barres"
        EntrySource.Manual -> Icons.Outlined.Edit to "Ajouté manuellement"
        EntrySource.Search -> Icons.Outlined.Search to "Ajouté via recherche"
        EntrySource.Frequent -> Icons.Outlined.Search to "Ajouté depuis les fréquents"
        EntrySource.TextAi -> Icons.AutoMirrored.Outlined.TextSnippet to "Ajouté par description textuelle"
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
            contentDescription = description,
            tint = LocalStrakkColors.current.textTertiary,
            modifier = Modifier.size(13.dp),
        )
    }
}
