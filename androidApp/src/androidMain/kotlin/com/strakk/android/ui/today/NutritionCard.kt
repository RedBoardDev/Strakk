package com.strakk.android.ui.today

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.strakk.android.ui.theme.LocalStrakkColors
import com.strakk.android.ui.theme.LocalStrakkRadius
import com.strakk.android.ui.theme.LocalStrakkSpacing
import com.strakk.android.ui.theme.LocalStrakkTextStyles
import com.strakk.android.ui.theme.StrakkTheme
import com.strakk.shared.domain.model.DailySummary

private val EaseOut: Easing = Easing { x -> 1f - (1f - x) * (1f - x) }

/**
 * Carte nutrition — Section STATS.
 * Zone gauche : ring protéines (180dp) + overline PROTÉINES.
 * Zone droite : 3 macros empilées (Calories, Glucides, Lipides).
 * Séparées par un divider vertical.
 */
@Composable
fun NutritionCard(
    summary: DailySummary,
    modifier: Modifier = Modifier,
) {
    val colors = LocalStrakkColors.current
    val spacing = LocalStrakkSpacing.current
    val radius = LocalStrakkRadius.current

    val proteinProgress = summary.proteinGoal
        ?.takeIf { it > 0 }
        ?.let { (summary.totalProtein / it.toDouble()).coerceIn(0.0, 1.0) }
        ?: 0.0

    val animatedProgress by animateFloatAsState(
        targetValue = proteinProgress.toFloat(),
        animationSpec = tween(durationMillis = 400, easing = EaseOut),
        label = "proteinRingProgress",
    )

    val cardBrush = Brush.verticalGradient(
        colors = listOf(colors.surface1GradientTop, colors.surface1GradientBottom),
    )

    Surface(
        shape = RoundedCornerShape(radius.xxl),
        color = Color.Transparent,
        border = BorderStroke(width = 1.dp, color = colors.borderSubtle),
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBrush),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(spacing.xl),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // ---- Zone gauche : protéines ----
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = "PROTÉINES",
                        style = LocalStrakkTextStyles.current.overline,
                        color = colors.accentOrange,
                    )
                    Spacer(Modifier.height(spacing.md))
                    ProteinRing(
                        progress = animatedProgress,
                        totalProtein = summary.totalProtein,
                        proteinGoal = summary.proteinGoal,
                        ringColor = colors.accentOrange,
                        ringColorEnd = colors.accentOrangeLight,
                        trackColor = colors.surface3,
                        diameter = 160.dp,
                        strokeWidth = 14.dp,
                    )
                }

                // ---- Divider vertical ----
                Spacer(Modifier.width(spacing.md))
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(colors.dividerStrong),
                )
                Spacer(Modifier.width(spacing.md))

                // ---- Zone droite : 3 macros ----
                Column(
                    modifier = Modifier.weight(1.2f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    MacroRow(
                        icon = Icons.Filled.LocalFireDepartment,
                        iconTint = colors.accentOrange,
                        iconBg = colors.accentOrangeFaint,
                        iconBorder = colors.accentOrangeBorder,
                        label = "CALORIES",
                        value = summary.totalCalories.toInt(),
                        goal = summary.calorieGoal,
                        unit = "kcal",
                        progressColor = colors.accentOrange,
                        progressColorEnd = colors.accentOrangeLight,
                        showProgress = true,
                    )
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = colors.dividerWeak,
                        modifier = Modifier.padding(vertical = spacing.sm),
                    )
                    MacroRow(
                        icon = Icons.Filled.Spa,
                        iconTint = colors.accentIndigo,
                        iconBg = colors.accentIndigoFaint,
                        iconBorder = colors.accentIndigoBorder,
                        label = "GLUCIDES",
                        value = summary.totalCarbs.toInt(),
                        goal = null,
                        unit = "g",
                        progressColor = colors.accentIndigo,
                        progressColorEnd = colors.accentIndigo,
                        showProgress = false,
                    )
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = colors.dividerWeak,
                        modifier = Modifier.padding(vertical = spacing.sm),
                    )
                    MacroRow(
                        icon = Icons.Filled.Opacity,
                        iconTint = colors.accentYellow,
                        iconBg = colors.accentYellowFaint,
                        iconBorder = colors.accentYellowBorder,
                        label = "LIPIDES",
                        value = summary.totalFat.toInt(),
                        goal = null,
                        unit = "g",
                        progressColor = colors.accentYellow,
                        progressColorEnd = colors.accentYellow,
                        showProgress = false,
                    )
                }
            }
        }
    }
}

// =============================================================================
// Protein ring
// =============================================================================

@Composable
private fun ProteinRing(
    progress: Float,
    totalProtein: Double,
    proteinGoal: Int?,
    ringColor: Color,
    ringColorEnd: Color,
    trackColor: Color,
    diameter: Dp,
    strokeWidth: Dp,
    modifier: Modifier = Modifier,
) {
    val textStyles = LocalStrakkTextStyles.current
    val colors = LocalStrakkColors.current

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(diameter),
    ) {
        val ringBrush = Brush.sweepGradient(listOf(ringColor, ringColorEnd))
        Canvas(modifier = Modifier.size(diameter)) {
            val stroke = strokeWidth.toPx()
            val diam = size.minDimension - stroke
            val topLeft = Offset(stroke / 2, stroke / 2)
            val arcSize = Size(diam, diam)

            // Track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
                topLeft = topLeft,
                size = arcSize,
            )

            // Progress
            if (progress > 0f) {
                drawArc(
                    brush = ringBrush,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                    topLeft = topLeft,
                    size = arcSize,
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = totalProtein.toInt().toString(),
                style = textStyles.display.copy(
                    fontFeatureSettings = "tnum",
                    fontWeight = FontWeight.ExtraBold,
                ),
                color = colors.textPrimary,
            )
            if (proteinGoal != null) {
                Text(
                    text = "/ $proteinGoal g",
                    style = textStyles.heading2.copy(
                        fontFeatureSettings = "tnum",
                        fontWeight = FontWeight.Medium,
                    ),
                    color = colors.textSecondary,
                )
            }
        }
    }
}

// =============================================================================
// Macro row
// =============================================================================

@Composable
private fun MacroRow(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    iconBorder: Color,
    label: String,
    value: Int,
    goal: Int?,
    unit: String,
    progressColor: Color,
    progressColorEnd: Color,
    showProgress: Boolean,
    modifier: Modifier = Modifier,
) {
    val textStyles = LocalStrakkTextStyles.current
    val colors = LocalStrakkColors.current
    val spacing = LocalStrakkSpacing.current
    val radius = LocalStrakkRadius.current

    val calorieProgress by animateFloatAsState(
        targetValue = if (showProgress && goal != null && goal > 0) {
            (value.toFloat() / goal.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        },
        animationSpec = tween(durationMillis = 300, easing = EaseOut),
        label = "macroProgress",
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        // Icon container 40dp
        Surface(
            shape = RoundedCornerShape(radius.md),
            color = iconBg,
            border = BorderStroke(width = 1.dp, color = iconBorder),
            modifier = Modifier.size(40.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Spacer(Modifier.width(spacing.xs))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = textStyles.overline,
                color = colors.textSecondary,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value.toString(),
                    style = textStyles.heading1.copy(fontFeatureSettings = "tnum"),
                    color = colors.textPrimary,
                )
                if (goal != null || unit.isNotEmpty()) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (goal != null) "/ $goal $unit" else unit,
                        style = textStyles.body.copy(fontFeatureSettings = "tnum"),
                        color = colors.textSecondary,
                    )
                }
            }
            if (showProgress && goal != null) {
                Spacer(Modifier.height(4.dp))
                MiniProgressBar(
                    progress = calorieProgress,
                    fillColor = progressColor,
                    fillColorEnd = progressColorEnd,
                    height = 4.dp,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

// =============================================================================
// Mini progress bar (4dp — calories)
// =============================================================================

@Composable
private fun MiniProgressBar(
    progress: Float,
    fillColor: Color,
    fillColorEnd: Color,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    val colors = LocalStrakkColors.current
    Canvas(
        modifier = modifier.height(height),
    ) {
        val cornerRadius = height.toPx() / 2f
        // Track
        drawRoundRect(
            color = colors.dividerWeak,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius),
        )
        // Fill
        if (progress > 0f) {
            drawRoundRect(
                brush = Brush.horizontalGradient(listOf(fillColor, fillColorEnd)),
                size = Size(size.width * progress, size.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius),
            )
        }
    }
}

// =============================================================================
// Preview
// =============================================================================

@Preview(showBackground = true, backgroundColor = 0xFF050918)
@Composable
private fun NutritionCardPreview() {
    StrakkTheme {
        NutritionCard(
            summary = DailySummary(
                totalProtein = 72.0,
                totalCalories = 1420.0,
                totalFat = 48.0,
                totalCarbs = 175.0,
                totalWater = 1500,
                proteinGoal = 160,
                calorieGoal = 2200,
                waterGoal = 3000,
            ),
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050918)
@Composable
private fun NutritionCardEmptyPreview() {
    StrakkTheme {
        NutritionCard(
            summary = DailySummary(
                totalProtein = 0.0,
                totalCalories = 0.0,
                totalFat = 0.0,
                totalCarbs = 0.0,
                totalWater = 0,
                proteinGoal = 160,
                calorieGoal = 2200,
                waterGoal = 3000,
            ),
            modifier = Modifier.padding(16.dp),
        )
    }
}
