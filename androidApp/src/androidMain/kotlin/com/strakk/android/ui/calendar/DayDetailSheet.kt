package com.strakk.android.ui.calendar

import android.util.Log
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.strakk.android.R
import com.strakk.android.ui.components.MacroProgressGrid
import com.strakk.android.ui.components.StrakkPrimaryButton
import com.strakk.android.ui.components.StrakkSheet
import com.strakk.android.ui.theme.LocalStrakkColors
import com.strakk.shared.domain.model.DailySummary
import com.strakk.shared.domain.model.MealEntry
import java.text.SimpleDateFormat
import java.util.Locale

private const val TAG = "DayDetailSheet"

/**
 * Bottom sheet showing the detail of a selected calendar day.
 *
 * Uses [StrakkSheet] for chrome (drag handle, themed background, close button + date title).
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
    StrakkSheet(
        onDismiss = onDismiss,
        title = formatDayDetailDate(date),
        sheetState = sheetState,
        modifier = modifier,
    ) {
        DayDetailContent(
            summary = null,
            meals = emptyList(),
            onAddMealForDay = onAddMealForDay,
        )
    }
}

@Suppress("FunctionSignature")
@Composable
private fun DayDetailContent(
    summary: DailySummary?,
    meals: List<MealEntry>,
    onAddMealForDay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalStrakkColors.current

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Nutrition section (always shown; matches iOS "NUTRITION" header)
        SectionLabel(stringResource(R.string.day_detail_section_nutrition))

        if (summary != null) {
            MacroProgressGrid(summary = summary)

            SectionLabel(stringResource(R.string.day_detail_section_water))

            DayWaterBlock(
                totalMl = summary.totalWater,
                goalMl = summary.waterGoal ?: 0,
            )
        } else {
            Text(
                text = stringResource(R.string.day_detail_no_data),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )
        }

        // Meals list (conditional, same as iOS)
        if (meals.isNotEmpty()) {
            SectionLabel(stringResource(R.string.day_detail_section_meals))
            meals.forEach { meal ->
                MealSummaryRow(meal = meal)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        StrakkPrimaryButton(
            text = stringResource(R.string.day_detail_add_button),
            onClick = onAddMealForDay,
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// =============================================================================
// Section label
// =============================================================================

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = LocalStrakkColors.current.textTertiary,
        modifier = modifier,
    )
}

// =============================================================================
// Read-only water block (no add/remove in calendar context)
// =============================================================================

@Suppress("FunctionSignature")
@Composable
private fun DayWaterBlock(
    totalMl: Int,
    goalMl: Int,
    modifier: Modifier = Modifier,
) {
    val colors = LocalStrakkColors.current
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
                text = stringResource(R.string.day_detail_water_label),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
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
                color = colors.water,
                trackColor = colors.surface2,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
            )
        }
    }
}

// =============================================================================
// Meal row
// =============================================================================

@Suppress("FunctionSignature")
@Composable
private fun MealSummaryRow(
    meal: MealEntry,
    modifier: Modifier = Modifier,
) {
    val colors = LocalStrakkColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = meal.name ?: stringResource(R.string.day_detail_meal_fallback),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "${meal.protein.toInt()}g · ${meal.calories.toInt()} kcal",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
        )
    }
}

// =============================================================================
// Date formatter
// =============================================================================

private fun formatDayDetailDate(date: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val parsed = sdf.parse(date)
        if (parsed != null) {
            SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(parsed)
        } else {
            date
        }
    } catch (e: Exception) {
        Log.w(TAG, "formatDayDetailDate: failed to parse date='$date'", e)
        date
    }
}
