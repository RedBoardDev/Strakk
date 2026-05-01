package com.strakk.android.ui.onboarding.weight

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strakk.android.R
import com.strakk.android.ui.theme.LocalStrakkColors
import com.strakk.android.ui.theme.LocalStrakkSpacing
import com.strakk.android.ui.theme.StrakkTheme

private const val WEIGHT_MIN = 30
private const val WEIGHT_MAX = 250
private const val WEIGHT_STEP = 0.5

@Composable
fun WeightStepContent(
    weightKg: Double,
    onWeightChanged: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalStrakkColors.current
    val spacing = LocalStrakkSpacing.current

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(R.string.onboarding_weight_title),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = colors.textPrimary,
        )

        Spacer(modifier = Modifier.height(spacing.xxl))

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            IconButton(
                onClick = {
                    val next = (weightKg - WEIGHT_STEP).coerceAtLeast(WEIGHT_MIN.toDouble())
                    onWeightChanged(next)
                },
                modifier = Modifier.size(64.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Remove,
                    contentDescription = "Diminuer le poids",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp),
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = formatWeight(weightKg),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 72.sp,
                    ),
                    color = colors.textPrimary,
                )
                Text(
                    text = stringResource(R.string.onboarding_weight_unit),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textSecondary,
                )
            }

            IconButton(
                onClick = {
                    val next = (weightKg + WEIGHT_STEP).coerceAtMost(WEIGHT_MAX.toDouble())
                    onWeightChanged(next)
                },
                modifier = Modifier.size(64.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Augmenter le poids",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
    }
}

private fun formatWeight(kg: Double): String {
    return if (kg == kg.toLong().toDouble()) {
        kg.toInt().toString()
    } else {
        String.format("%.1f", kg)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050918)
@Composable
private fun WeightStepContentPreview() {
    StrakkTheme {
        WeightStepContent(
            weightKg = 75.0,
            onWeightChanged = {},
        )
    }
}
