package com.strakk.android.ui.home.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.strakk.android.ui.theme.LocalStrakkColors
import com.strakk.shared.domain.model.EntrySource
import com.strakk.shared.domain.model.FrequentItem
import com.strakk.shared.domain.model.MealEntry

private fun buildScaledFrequentEntry(item: FrequentItem, gramsInput: String): MealEntry {
    val grams = gramsInput.toDoubleOrNull() ?: 100.0
    val factor = grams / 100.0
    return MealEntry(
        id = "",
        logDate = "",
        name = item.name ?: item.normalizedName,
        protein = item.protein * factor,
        calories = item.calories * factor,
        fat = item.fat?.times(factor),
        carbs = item.carbs?.times(factor),
        source = EntrySource.Frequent,
        createdAt = "",
        quantity = "${gramsInput}g",
    )
}

@Composable
internal fun FrequentItemRow(
    item: FrequentItem,
    isExpanded: Boolean,
    onTap: () -> Unit,
    onConfirm: (MealEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onTap,
        shape = RoundedCornerShape(12.dp),
        color = if (isExpanded) LocalStrakkColors.current.surface2 else MaterialTheme.colorScheme.surface,
        modifier = modifier.animateContentSize(animationSpec = tween(200)),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name ?: item.normalizedName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                                append("${item.protein.toInt()}g prot")
                            }
                            withStyle(SpanStyle(color = LocalStrakkColors.current.textSecondary)) {
                                append(" · ${item.calories.toInt()} kcal")
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = "Utilisé récemment",
                    tint = LocalStrakkColors.current.textTertiary,
                    modifier = Modifier.size(16.dp),
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(200)),
                exit = shrinkVertically(animationSpec = tween(200)),
            ) {
                var gramsInput by rememberSaveable { mutableStateOf("100") }
                ItemQuantityStepper(
                    gramsInput = gramsInput,
                    onGramsChange = { gramsInput = it },
                    onConfirm = {
                        onConfirm(buildScaledFrequentEntry(item, gramsInput))
                    },
                )
            }
        }
    }
}
