package com.strakk.android.ui.onboarding.goals

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.strakk.android.R
import com.strakk.android.ui.onboarding.components.StepperRow
import com.strakk.android.ui.theme.LocalStrakkColors
import com.strakk.android.ui.theme.LocalStrakkSpacing
import com.strakk.android.ui.theme.StrakkTheme
import com.strakk.shared.presentation.onboarding.AiCalculationState

@Composable
fun NutritionGoalsContent(
    proteinGoal: Int,
    calorieGoal: Int,
    fatGoal: Int,
    carbGoal: Int,
    waterGoal: Int,
    aiState: AiCalculationState,
    onCalculateWithAi: () -> Unit,
    onProteinGoalChanged: (Int) -> Unit,
    onCalorieGoalChanged: (Int) -> Unit,
    onFatGoalChanged: (Int) -> Unit,
    onCarbGoalChanged: (Int) -> Unit,
    onWaterGoalChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalStrakkColors.current
    val spacing = LocalStrakkSpacing.current

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(R.string.onboarding_nutrition_title),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = colors.textPrimary,
        )

        Spacer(modifier = Modifier.height(spacing.xl))

        // AI button
        Button(
            onClick = onCalculateWithAi,
            enabled = aiState == AiCalculationState.AVAILABLE || aiState == AiCalculationState.FAILED,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                disabledContainerColor = when (aiState) {
                    AiCalculationState.COMPLETED -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                },
                disabledContentColor = Color.White.copy(alpha = 0.7f),
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            when (aiState) {
                AiCalculationState.LOADING -> {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                }
                else -> {
                    Text(
                        text = when (aiState) {
                            AiCalculationState.AVAILABLE -> stringResource(R.string.onboarding_nutrition_ai_available)
                            AiCalculationState.COMPLETED -> stringResource(R.string.onboarding_nutrition_ai_completed)
                            AiCalculationState.FAILED -> stringResource(R.string.onboarding_nutrition_ai_failed)
                            AiCalculationState.LOADING -> ""
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(spacing.xl))

        HorizontalDivider(color = colors.dividerWeak)

        StepperRow(
            label = stringResource(R.string.onboarding_nutrition_protein),
            value = proteinGoal,
            unit = stringResource(R.string.onboarding_nutrition_unit_g),
            onDecrement = { onProteinGoalChanged((proteinGoal - 5).coerceAtLeast(0)) },
            onIncrement = { onProteinGoalChanged((proteinGoal + 5).coerceAtMost(500)) },
            minValue = 0,
            maxValue = 500,
            modifier = Modifier.padding(vertical = spacing.sm),
        )

        HorizontalDivider(color = colors.dividerWeak, modifier = Modifier.fillMaxWidth())

        StepperRow(
            label = stringResource(R.string.onboarding_nutrition_calories),
            value = calorieGoal,
            unit = stringResource(R.string.onboarding_nutrition_unit_kcal),
            onDecrement = { onCalorieGoalChanged((calorieGoal - 50).coerceAtLeast(0)) },
            onIncrement = { onCalorieGoalChanged((calorieGoal + 50).coerceAtMost(10000)) },
            minValue = 0,
            maxValue = 10000,
            modifier = Modifier.padding(vertical = spacing.sm),
        )

        HorizontalDivider(color = colors.dividerWeak, modifier = Modifier.fillMaxWidth())

        StepperRow(
            label = stringResource(R.string.onboarding_nutrition_fat),
            value = fatGoal,
            unit = stringResource(R.string.onboarding_nutrition_unit_g),
            onDecrement = { onFatGoalChanged((fatGoal - 5).coerceAtLeast(0)) },
            onIncrement = { onFatGoalChanged((fatGoal + 5).coerceAtMost(500)) },
            minValue = 0,
            maxValue = 500,
            modifier = Modifier.padding(vertical = spacing.sm),
        )

        HorizontalDivider(color = colors.dividerWeak, modifier = Modifier.fillMaxWidth())

        StepperRow(
            label = stringResource(R.string.onboarding_nutrition_carbs),
            value = carbGoal,
            unit = stringResource(R.string.onboarding_nutrition_unit_g),
            onDecrement = { onCarbGoalChanged((carbGoal - 5).coerceAtLeast(0)) },
            onIncrement = { onCarbGoalChanged((carbGoal + 5).coerceAtMost(1000)) },
            minValue = 0,
            maxValue = 1000,
            modifier = Modifier.padding(vertical = spacing.sm),
        )

        HorizontalDivider(color = colors.dividerWeak, modifier = Modifier.fillMaxWidth())

        StepperRow(
            label = stringResource(R.string.onboarding_nutrition_water),
            value = waterGoal,
            unit = stringResource(R.string.onboarding_nutrition_unit_ml),
            onDecrement = { onWaterGoalChanged((waterGoal - 250).coerceAtLeast(0)) },
            onIncrement = { onWaterGoalChanged((waterGoal + 250).coerceAtMost(8000)) },
            minValue = 0,
            maxValue = 8000,
            modifier = Modifier.padding(vertical = spacing.sm),
        )

        HorizontalDivider(color = colors.dividerWeak, modifier = Modifier.fillMaxWidth())
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050918)
@Composable
private fun NutritionGoalsContentPreview() {
    StrakkTheme {
        NutritionGoalsContent(
            proteinGoal = 150,
            calorieGoal = 2200,
            fatGoal = 70,
            carbGoal = 250,
            waterGoal = 2500,
            aiState = AiCalculationState.AVAILABLE,
            onCalculateWithAi = {},
            onProteinGoalChanged = {},
            onCalorieGoalChanged = {},
            onFatGoalChanged = {},
            onCarbGoalChanged = {},
            onWaterGoalChanged = {},
        )
    }
}
