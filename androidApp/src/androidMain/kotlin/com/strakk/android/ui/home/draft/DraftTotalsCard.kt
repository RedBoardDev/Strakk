package com.strakk.android.ui.home.draft

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.strakk.android.R
import com.strakk.android.ui.theme.LocalStrakkColors
import com.strakk.shared.presentation.meal.MealDraftUiState

@Composable
internal fun TotalsCard(
    state: MealDraftUiState.Editing,
    modifier: Modifier = Modifier,
) {
    val totals = state.totals
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            MacroCell(
                label = stringResource(R.string.meal_draft_macro_protein),
                value = "${totals.protein.toInt()}g",
                color = MaterialTheme.colorScheme.primary,
            )
            MacroCell(
                label = stringResource(R.string.meal_draft_macro_calories),
                value = "${totals.calories.toInt()} kcal",
                color = LocalStrakkColors.current.calories,
            )
            MacroCell(
                label = stringResource(R.string.meal_draft_macro_fat),
                value = "${totals.fat.toInt()}g",
                color = LocalStrakkColors.current.textSecondary,
            )
            MacroCell(
                label = stringResource(R.string.meal_draft_macro_carbs),
                value = "${totals.carbs.toInt()}g",
                color = LocalStrakkColors.current.textSecondary,
            )
        }
    }
}

@Composable
internal fun MacroCell(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = color,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = LocalStrakkColors.current.textTertiary,
        )
    }
}
