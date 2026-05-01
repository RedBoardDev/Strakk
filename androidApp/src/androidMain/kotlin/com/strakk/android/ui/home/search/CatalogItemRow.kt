package com.strakk.android.ui.home.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.strakk.android.R
import com.strakk.android.ui.theme.LocalStrakkColors
import com.strakk.shared.domain.model.EntrySource
import com.strakk.shared.domain.model.FoodCatalogItem
import com.strakk.shared.domain.model.MealEntry

private fun buildScaledCatalogEntry(item: FoodCatalogItem, gramsInput: String): MealEntry {
    val grams = gramsInput.toDoubleOrNull() ?: item.defaultPortionGrams
    val factor = grams / 100.0
    return MealEntry(
        id = "",
        logDate = "",
        name = item.name,
        protein = item.protein * factor,
        calories = item.calories * factor,
        fat = item.fat?.times(factor),
        carbs = item.carbs?.times(factor),
        source = EntrySource.Search,
        createdAt = "",
        quantity = "${gramsInput}g",
    )
}

@Composable
internal fun CatalogItemRow(
    item: FoodCatalogItem,
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
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    item.brand?.let { brand ->
                        Text(
                            text = brand,
                            style = MaterialTheme.typography.labelSmall,
                            color = LocalStrakkColors.current.textSecondary,
                        )
                    }
                }
                item.nutriscore?.firstOrNull()?.let { grade ->
                    NutriscoreBadge(grade = grade)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = LocalStrakkColors.current.calories)) {
                        append("${item.calories.toInt()} kcal")
                    }
                    withStyle(SpanStyle(color = LocalStrakkColors.current.textTertiary)) { append(" · ") }
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                        append("${item.protein.toInt()} g prot")
                    }
                    item.fat?.let {
                        withStyle(SpanStyle(color = LocalStrakkColors.current.textTertiary)) { append(" · ") }
                        withStyle(SpanStyle(color = LocalStrakkColors.current.accentYellow)) {
                            append("${it.toInt()} g lip")
                        }
                    }
                    item.carbs?.let {
                        withStyle(SpanStyle(color = LocalStrakkColors.current.textTertiary)) { append(" · ") }
                        withStyle(SpanStyle(color = LocalStrakkColors.current.accentIndigo)) {
                            append("${it.toInt()} g gluc")
                        }
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.search_food_per_100g),
                style = MaterialTheme.typography.labelSmall,
                color = LocalStrakkColors.current.textTertiary,
            )

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(200)),
                exit = shrinkVertically(animationSpec = tween(200)),
            ) {
                var gramsInput by rememberSaveable { mutableStateOf(item.defaultPortionGrams.toInt().toString()) }
                ItemQuantityStepper(
                    gramsInput = gramsInput,
                    onGramsChange = { gramsInput = it },
                    onConfirm = {
                        onConfirm(buildScaledCatalogEntry(item, gramsInput))
                    },
                )
            }
        }
    }
}
