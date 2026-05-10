package com.strakk.android.ui.components

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Opacity
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.strakk.android.R
import com.strakk.android.ui.theme.LocalStrakkColors
import com.strakk.android.ui.theme.LocalStrakkRadius
import com.strakk.android.ui.theme.LocalStrakkSpacing
import com.strakk.android.ui.theme.LocalStrakkTextStyles
import com.strakk.android.ui.theme.StrakkTheme
import com.strakk.shared.domain.model.DailySummary

private val EaseOut: Easing = Easing { x -> 1f - (1f - x) * (1f - x) }

// =============================================================================
// MacroCard
// =============================================================================

/**
 * Single macro tile: icon-tile + label (overline) + big value + optional goal +
 * animated 6dp progress bar.
 *
 * Per DESIGN.md §5 (Macro card):
 *  - Goal reached → bar fill switches to [StrakkSemanticColors.success].
 *  - When [goal] is null, bar fills to 0 and the unit appears without "/ goal".
 *
 * @param color       Primary accent for this macro (icon tint + progress fill).
 * @param colorFaint  Tinted background for the icon tile.
 * @param colorBorder Border for the icon tile.
 */
@Suppress("LongMethod", "LongParameterList")
@Composable
fun MacroCard(
    label: String,
    value: Int,
    unit: String,
    color: Color,
    colorFaint: Color,
    colorBorder: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    goal: Int? = null,
) {
    val colors = LocalStrakkColors.current
    val spacing = LocalStrakkSpacing.current
    val radius = LocalStrakkRadius.current
    val textStyles = LocalStrakkTextStyles.current

    val progress = goal?.takeIf { it > 0 }
        ?.let { (value.toFloat() / it.toFloat()).coerceIn(0f, 1f) }
        ?: 0f
    val goalReached = goal != null && value >= goal
    val barColor = if (goalReached) colors.success else color

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 300, easing = EaseOut),
        label = "macroCardProgress",
    )

    Surface(
        shape = RoundedCornerShape(radius.sm),
        color = colors.surface1,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(spacing.sm),
        ) {
            // Header: icon tile + label
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(radius.sm),
                    color = colorFaint,
                    border = BorderStroke(1.dp, colorBorder),
                    modifier = Modifier.size(28.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                Spacer(Modifier.width(spacing.xxs))
                Text(
                    text = label,
                    style = textStyles.overline,
                    color = colors.textSecondary,
                )
            }

            Spacer(Modifier.height(spacing.xs))

            // Value
            Text(
                text = value.toString(),
                style = textStyles.heading2.copy(fontFeatureSettings = "tnum"),
                color = colors.textPrimary,
            )

            // Goal / unit subtitle
            Text(
                text = if (goal != null) "/ $goal $unit" else unit,
                style = textStyles.caption,
                color = colors.textTertiary,
            )

            Spacer(Modifier.height(spacing.xs))

            // Progress bar (6dp, capsule, ease-out, success on goal reached)
            MacroLinearProgressBar(
                progress = animatedProgress,
                barColor = barColor,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// =============================================================================
// MacroProgressGrid — 2×2 canonical grid wired to DailySummary
// =============================================================================

/**
 * Canonical 2×2 macro grid consumed by Today and Calendar Day screens.
 * Protein · Calories (top row) / Fat · Carbs (bottom row).
 * Uses [MacroCard] for each cell with the palette defined in DESIGN.md §5.
 */
@Suppress("LongMethod", "FunctionSignature")
@Composable
fun MacroProgressGrid(
    summary: DailySummary,
    modifier: Modifier = Modifier,
) {
    val colors = LocalStrakkColors.current
    val spacing = LocalStrakkSpacing.current

    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            modifier = Modifier.fillMaxWidth(),
        ) {
            MacroCard(
                label = stringResource(R.string.progress_protein_label),
                value = summary.totalProtein.toInt(),
                unit = "g",
                color = colors.accentOrange,
                colorFaint = colors.accentOrangeFaint,
                colorBorder = colors.accentOrangeBorder,
                icon = Icons.Outlined.FitnessCenter,
                goal = summary.proteinGoal,
                modifier = Modifier.weight(1f),
            )
            MacroCard(
                label = stringResource(R.string.progress_calories_label),
                value = summary.totalCalories.toInt(),
                unit = "kcal",
                color = colors.accentOrangeLight,
                colorFaint = colors.accentOrangeFaint,
                colorBorder = colors.accentOrangeBorder,
                icon = Icons.Outlined.LocalFireDepartment,
                goal = summary.calorieGoal,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            modifier = Modifier.fillMaxWidth(),
        ) {
            MacroCard(
                label = stringResource(R.string.progress_fat_label),
                value = summary.totalFat.toInt(),
                unit = "g",
                color = colors.accentYellow,
                colorFaint = colors.accentYellowFaint,
                colorBorder = colors.accentYellowBorder,
                icon = Icons.Outlined.Opacity,
                goal = summary.fatGoal,
                modifier = Modifier.weight(1f),
            )
            MacroCard(
                label = stringResource(R.string.progress_carbs_label),
                value = summary.totalCarbs.toInt(),
                unit = "g",
                color = colors.accentIndigo,
                colorFaint = colors.accentIndigoFaint,
                colorBorder = colors.accentIndigoBorder,
                icon = Icons.Outlined.Spa,
                goal = summary.carbGoal,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// =============================================================================
// Progress bar primitive
// =============================================================================

/**
 * 6dp-height capsule progress bar. Track is [StrakkSemanticColors.surface2],
 * fill is [barColor]. Designed for use inside [MacroCard].
 */
@Suppress("FunctionSignature")
@Composable
private fun MacroLinearProgressBar(
    progress: Float,
    barColor: Color,
    modifier: Modifier = Modifier,
) {
    val colors = LocalStrakkColors.current
    val fillBrush = remember(barColor) {
        Brush.horizontalGradient(listOf(barColor, barColor))
    }
    Canvas(modifier = modifier.height(6.dp)) {
        val cr = CornerRadius(size.height / 2f)
        // Track
        drawRoundRect(color = colors.surface2, cornerRadius = cr)
        // Fill
        if (progress > 0f) {
            drawRoundRect(
                brush = fillBrush,
                size = Size(size.width * progress, size.height),
                cornerRadius = cr,
            )
        }
    }
}

// =============================================================================
// Preview
// =============================================================================

@Suppress("UnusedPrivateMember")
@Preview(showBackground = true, backgroundColor = 0xFF050918)
@Composable
private fun MacroCardPreview() {
    StrakkTheme {
        val colors = LocalStrakkColors.current
        MacroCard(
            label = "PROTEINS",
            value = 124,
            unit = "g",
            color = colors.accentOrange,
            colorFaint = colors.accentOrangeFaint,
            colorBorder = colors.accentOrangeBorder,
            icon = Icons.Outlined.FitnessCenter,
            goal = 160,
            modifier = Modifier
                .padding(16.dp)
                .width(160.dp),
        )
    }
}

@Suppress("UnusedPrivateMember")
@Preview(showBackground = true, backgroundColor = 0xFF050918)
@Composable
private fun MacroProgressGridPreview() {
    StrakkTheme {
        MacroProgressGrid(
            summary = DailySummary(
                totalProtein = 124.0,
                totalCalories = 1640.0,
                totalFat = 52.0,
                totalCarbs = 188.0,
                totalWater = 1800,
                proteinGoal = 160,
                calorieGoal = 2200,
                fatGoal = 70,
                carbGoal = 250,
                waterGoal = 3000,
            ),
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Suppress("UnusedPrivateMember")
@Preview(showBackground = true, backgroundColor = 0xFF050918)
@Composable
private fun MacroProgressGridGoalReachedPreview() {
    StrakkTheme {
        MacroProgressGrid(
            summary = DailySummary(
                totalProtein = 165.0,
                totalCalories = 2250.0,
                totalFat = 72.0,
                totalCarbs = 255.0,
                totalWater = 3100,
                proteinGoal = 160,
                calorieGoal = 2200,
                fatGoal = 70,
                carbGoal = 250,
                waterGoal = 3000,
            ),
            modifier = Modifier.padding(16.dp),
        )
    }
}
