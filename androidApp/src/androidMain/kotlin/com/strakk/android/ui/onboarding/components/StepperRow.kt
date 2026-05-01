package com.strakk.android.ui.onboarding.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.dp
import com.strakk.android.ui.theme.LocalStrakkColors

@Composable
fun StepperRow(
    label: String,
    value: Int,
    unit: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier,
    minValue: Int = 0,
    maxValue: Int = Int.MAX_VALUE,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = LocalStrakkColors.current.textPrimary,
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconButton(
                onClick = onDecrement,
                enabled = value > minValue,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Remove,
                    contentDescription = "Diminuer $label",
                    tint = if (value > minValue) MaterialTheme.colorScheme.primary
                    else LocalStrakkColors.current.textDisabled,
                )
            }

            Text(
                text = "$value $unit",
                style = MaterialTheme.typography.titleMedium,
                color = LocalStrakkColors.current.textPrimary,
                modifier = Modifier,
            )

            IconButton(
                onClick = onIncrement,
                enabled = value < maxValue,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Augmenter $label",
                    tint = if (value < maxValue) MaterialTheme.colorScheme.primary
                    else LocalStrakkColors.current.textDisabled,
                )
            }
        }
    }
}
