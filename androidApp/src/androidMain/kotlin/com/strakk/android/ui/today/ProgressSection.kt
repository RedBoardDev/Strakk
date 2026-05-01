package com.strakk.android.ui.today

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strakk.android.R
import com.strakk.android.ui.theme.LocalStrakkColors
import com.strakk.android.ui.theme.StrakkTheme
import com.strakk.shared.domain.model.DailySummary

/**
 * Section "Stats" du Today screen — Direction A : Hero compact + ledger.
 *
 * Card unique [surface] :
 *  - À gauche : ring protéines (96dp) avec valeur centrale
 *  - À droite : ledger vertical de 3 macros (Calories / Glucides / Lipides)
 *    séparées par des dividers fins.
 *
 * Aucun fond teinté primaire — l'accent vient uniquement du ring et du label.
 */
@Composable
fun ProgressSection(
    summary: DailySummary,
    modifier: Modifier = Modifier,
) {
    val colors = LocalStrakkColors.current
    val proteinProgress = summary.proteinGoal
        ?.takeIf { it > 0 }
        ?.let { (summary.totalProtein / it.toDouble()).coerceIn(0.0, 2.0) }
        ?: 0.0
    val isProteinReached = proteinProgress >= 1.0
    val animatedProgress by animateFloatAsState(
        targetValue = proteinProgress.toFloat().coerceAtMost(1f),
        label = "proteinProgress",
    )

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header label "PROTÉINES" + check optionnel
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.progress_protein_label),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                    ),
                    color = colors.protein,
                )
                if (isProteinReached) {
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = stringResource(R.string.progress_goal_reached),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = colors.success,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Ring protéines (gauche)
                ProteinRing(
                    progress = animatedProgress,
                    totalProtein = summary.totalProtein,
                    proteinGoal = summary.proteinGoal,
                    ringColor = if (isProteinReached) colors.success else colors.protein,
                    trackColor = colors.surface3,
                )

                Spacer(Modifier.width(20.dp))

                // Ledger 3 macros (droite)
                Column(modifier = Modifier.weight(1f)) {
                    LedgerRow(
                        label = stringResource(R.string.progress_calories_label),
                        value = summary.totalCalories.toInt().toString(),
                        suffix = summary.calorieGoal?.let { "/ $it kcal" } ?: "kcal",
                    )
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = colors.divider,
                        modifier = Modifier.padding(vertical = 10.dp),
                    )
                    LedgerRow(
                        label = stringResource(R.string.progress_carbs_label),
                        value = summary.totalCarbs.toInt().toString(),
                        suffix = "g",
                    )
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = colors.divider,
                        modifier = Modifier.padding(vertical = 10.dp),
                    )
                    LedgerRow(
                        label = stringResource(R.string.progress_fat_label),
                        value = summary.totalFat.toInt().toString(),
                        suffix = "g",
                    )
                }
            }
        }
    }
}

@Composable
private fun ProteinRing(
    progress: Float,
    totalProtein: Double,
    proteinGoal: Int?,
    ringColor: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
) {
    val colors = LocalStrakkColors.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(96.dp),
    ) {
        Canvas(modifier = Modifier.size(96.dp)) {
            val stroke = 6.dp.toPx()
            val diameter = size.minDimension - stroke
            val topLeft = Offset(stroke / 2, stroke / 2)
            val arcSize = Size(diameter, diameter)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
                topLeft = topLeft,
                size = arcSize,
            )
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
                topLeft = topLeft,
                size = arcSize,
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = totalProtein.toInt().toString(),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = proteinGoal?.let { "/ $it g" } ?: "g",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textTertiary,
            )
        }
    }
}

@Composable
private fun LedgerRow(
    label: String,
    value: String,
    suffix: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalStrakkColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp,
            ),
            color = colors.textSecondary,
        )
        Text(
            text = buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    ),
                ) { append(value) }
                append(" ")
                withStyle(SpanStyle(color = colors.textTertiary)) { append(suffix) }
            },
            style = MaterialTheme.typography.titleMedium,
        )
    }
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
                waterGoal = 2500,
            ),
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF151720)
@Composable
private fun ProgressSectionReachedPreview() {
    StrakkTheme {
        ProgressSection(
            summary = DailySummary(
                totalProtein = 165.0,
                totalCalories = 2150.0,
                totalFat = 68.0,
                totalCarbs = 248.0,
                totalWater = 2300,
                proteinGoal = 160,
                calorieGoal = 2200,
                waterGoal = 2500,
            ),
            modifier = Modifier.padding(16.dp),
        )
    }
}
