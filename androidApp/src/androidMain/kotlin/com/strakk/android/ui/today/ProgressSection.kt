package com.strakk.android.ui.today

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.strakk.android.ui.components.MacroProgressGrid
import com.strakk.android.ui.theme.StrakkTheme
import com.strakk.shared.domain.model.DailySummary

/**
 * Today screen macro section — delegates to the canonical [MacroProgressGrid].
 * Kept as a named wrapper so call-sites in TodayContent remain readable.
 */
@Composable
fun ProgressSection(
    summary: DailySummary,
    modifier: Modifier = Modifier,
) {
    MacroProgressGrid(summary = summary, modifier = modifier)
}

@Preview(showBackground = true, backgroundColor = 0xFF151720)
@Composable
private fun ProgressSectionPreview() {
    StrakkTheme {
        ProgressSection(
            summary = DailySummary(
                totalProtein = 142.0,
                totalCalories = 1840.0,
                totalFat = 54.0,
                totalCarbs = 210.0,
                totalWater = 1500,
                proteinGoal = 160,
                calorieGoal = 2200,
                fatGoal = 70,
                carbGoal = 250,
                waterGoal = 2500,
            ),
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF151720)
@Composable
internal fun ProgressSectionGoalReachedPreview() {
    StrakkTheme {
        ProgressSection(
            summary = DailySummary(
                totalProtein = 165.0,
                totalCalories = 2250.0,
                totalFat = 72.0,
                totalCarbs = 255.0,
                totalWater = 2300,
                proteinGoal = 160,
                calorieGoal = 2200,
                fatGoal = 70,
                carbGoal = 250,
                waterGoal = 2500,
            ),
            modifier = Modifier.padding(16.dp),
        )
    }
}
