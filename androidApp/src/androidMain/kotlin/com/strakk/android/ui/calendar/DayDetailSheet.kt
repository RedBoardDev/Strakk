package com.strakk.android.ui.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.strakk.android.ui.theme.LocalStrakkColors
import com.strakk.shared.domain.model.DailySummary
import com.strakk.shared.domain.model.MealEntry

/**
 * Bottom sheet showing the detail of a selected calendar day.
 *
 * Data is stub until CalendarContract is connected from the KMP layer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailSheet(
    date: String,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onAddMealForDay: () -> Unit,
    // Future: summary: DailySummary? = null, meals: List<MealEntry> = emptyList()
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
    ) {
        DayDetailContent(
            date = date,
            summary = null,
            meals = emptyList(),
            onAddMealForDay = onAddMealForDay,
        )
    }
}

@Composable
private fun DayDetailContent(
    date: String,
    summary: DailySummary?,
    meals: List<MealEntry>,
    onAddMealForDay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Date header
        Text(
            text = formatDayDetailDate(date),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        if (summary != null) {
            // Macros section
            SectionLabel("MACROS")

            MacroRow(
                label = "Protéines",
                value = summary.totalProtein.toInt(),
                unit = "g",
                goal = summary.proteinGoal ?: 0,
                color = MaterialTheme.colorScheme.primary,
            )
            MacroRow(
                label = "Calories",
                value = summary.totalCalories.toInt(),
                unit = "kcal",
                goal = summary.calorieGoal ?: 0,
                color = LocalStrakkColors.current.calories,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Lipides",
                        style = MaterialTheme.typography.labelSmall,
                        color = LocalStrakkColors.current.textSecondary,
                    )
                    Text(
                        text = "${summary.totalFat.toInt()}g",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Glucides",
                        style = MaterialTheme.typography.labelSmall,
                        color = LocalStrakkColors.current.textSecondary,
                    )
                    Text(
                        text = "${summary.totalCarbs.toInt()}g",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            // Water section
            SectionLabel("EAU")
            WaterRow(
                totalMl = summary.totalWater,
                goalMl = summary.waterGoal ?: 0,
            )
        } else {
            // No data for this day yet
            Text(
                text = "Aucune donnée pour ce jour.",
                style = MaterialTheme.typography.bodyMedium,
                color = LocalStrakkColors.current.textSecondary,
            )
        }

        // Meals list
        if (meals.isNotEmpty()) {
            SectionLabel("REPAS")
            meals.forEach { meal ->
                MealSummaryRow(meal = meal)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Add meal for that day CTA
        Button(
            onClick = onAddMealForDay,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text("+ Ajouter pour ce jour")
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = LocalStrakkColors.current.textTertiary,
        modifier = modifier,
    )
}

@Composable
private fun MacroRow(
    label: String,
    value: Int,
    unit: String,
    goal: Int,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = LocalStrakkColors.current.textSecondary,
            )
            Text(
                text = "$value / $goal$unit",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (goal > 0) {
            LinearProgressIndicator(
                progress = { (value.toFloat() / goal).coerceIn(0f, 1f) },
                color = color,
                trackColor = LocalStrakkColors.current.surface2,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
            )
        }
    }
}

@Composable
private fun WaterRow(
    totalMl: Int,
    goalMl: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "Eau",
                style = MaterialTheme.typography.bodyMedium,
                color = LocalStrakkColors.current.textSecondary,
            )
            Text(
                text = "$totalMl / ${goalMl}mL",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (goalMl > 0) {
            LinearProgressIndicator(
                progress = { (totalMl.toFloat() / goalMl).coerceIn(0f, 1f) },
                color = LocalStrakkColors.current.water,
                trackColor = LocalStrakkColors.current.surface2,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
            )
        }
    }
}

@Composable
private fun MealSummaryRow(
    meal: MealEntry,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = meal.name ?: "Repas",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "${meal.protein.toInt()}g · ${meal.calories.toInt()} kcal",
            style = MaterialTheme.typography.bodySmall,
            color = LocalStrakkColors.current.textSecondary,
        )
    }
}

private fun formatDayDetailDate(date: String): String {
    return try {
        val parts = date.split("-")
        if (parts.size == 3) {
            val day = parts[2].trimStart('0').ifEmpty { "0" }
            val month = when (parts[1].toInt()) {
                1 -> "janvier"; 2 -> "février"; 3 -> "mars"; 4 -> "avril"
                5 -> "mai"; 6 -> "juin"; 7 -> "juillet"; 8 -> "août"
                9 -> "septembre"; 10 -> "octobre"; 11 -> "novembre"; 12 -> "décembre"
                else -> "?"
            }
            "$day $month ${parts[0]}"
        } else date
    } catch (_: Exception) {
        date
    }
}
