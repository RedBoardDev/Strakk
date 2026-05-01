package com.strakk.android.ui.onboarding.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strakk.android.R
import com.strakk.android.ui.theme.LocalStrakkColors
import com.strakk.android.ui.theme.LocalStrakkSpacing
import com.strakk.android.ui.theme.StrakkTheme

@Composable
fun DayPreviewContent(
    proteinGoal: Int,
    calorieGoal: Int,
    fatGoal: Int,
    carbGoal: Int,
    waterGoal: Int,
    modifier: Modifier = Modifier,
) {
    val colors = LocalStrakkColors.current
    val spacing = LocalStrakkSpacing.current

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(R.string.onboarding_preview_title),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = colors.textPrimary,
        )

        Spacer(modifier = Modifier.height(spacing.xxl))

        // 2x2 macro grid
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            modifier = Modifier.fillMaxWidth(),
        ) {
            MacroCard(
                label = stringResource(R.string.onboarding_preview_protein),
                value = "$proteinGoal",
                unit = "g",
                tintColor = colors.accentOrange,
                modifier = Modifier.weight(1f),
            )
            MacroCard(
                label = stringResource(R.string.onboarding_preview_calories),
                value = "$calorieGoal",
                unit = "kcal",
                tintColor = colors.accentOrangeLight,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(spacing.sm))

        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            modifier = Modifier.fillMaxWidth(),
        ) {
            MacroCard(
                label = stringResource(R.string.onboarding_preview_fat),
                value = "$fatGoal",
                unit = "g",
                tintColor = colors.accentYellow,
                modifier = Modifier.weight(1f),
            )
            MacroCard(
                label = stringResource(R.string.onboarding_preview_carbs),
                value = "$carbGoal",
                unit = "g",
                tintColor = colors.accentIndigo,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(spacing.xl))

        // Water bar
        WaterPreviewBar(
            waterGoal = waterGoal,
        )
    }
}

@Composable
private fun MacroCard(
    label: String,
    value: String,
    unit: String,
    tintColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    val colors = LocalStrakkColors.current
    val spacing = LocalStrakkSpacing.current

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface1)
            .padding(spacing.md),
    ) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
            )
            Spacer(modifier = Modifier.height(spacing.xxs))
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = tintColor,
                    ),
                )
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                    ),
                    color = colors.textSecondary,
                )
            }
            Spacer(modifier = Modifier.height(spacing.xs))
            // Small accent bar at bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(tintColor.copy(alpha = 0.3f)),
            )
        }
    }
}

@Composable
private fun WaterPreviewBar(
    waterGoal: Int,
    modifier: Modifier = Modifier,
) {
    val colors = LocalStrakkColors.current
    val spacing = LocalStrakkSpacing.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface1)
            .padding(spacing.md),
    ) {
        Column {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.onboarding_preview_water),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textSecondary,
                )
                Text(
                    text = stringResource(R.string.onboarding_preview_water_goal, waterGoal),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }

            Spacer(modifier = Modifier.height(spacing.xs))

            Text(
                text = "0 mL",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = colors.accentBlue,
            )

            Spacer(modifier = Modifier.height(spacing.xs))

            // Progress bar at 0
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(colors.surface2),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050918)
@Composable
private fun DayPreviewContentPreview() {
    StrakkTheme {
        DayPreviewContent(
            proteinGoal = 150,
            calorieGoal = 2200,
            fatGoal = 70,
            carbGoal = 250,
            waterGoal = 2500,
        )
    }
}
