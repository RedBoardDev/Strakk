package com.strakk.android.ui.today

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.strakk.android.ui.theme.ColorWater
import com.strakk.android.ui.theme.LocalStrakkColors
import com.strakk.android.ui.theme.StrakkTheme
import com.strakk.shared.domain.model.DailySummary
import com.strakk.shared.presentation.today.TodayEvent

private const val DEFAULT_AMOUNT_ML = 250
private const val MIN_AMOUNT_ML = 50
private const val MAX_AMOUNT_ML = 2000

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterRow(
    summary: DailySummary,
    onEvent: (TodayEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalStrakkColors.current
    val totalL = summary.totalWater / 1000f
    val goalL = summary.waterGoal?.let { it / 1000f }
    val isEmpty = summary.totalWater <= 0

    var dialogOpen by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            // Goutte
            Icon(
                imageVector = Icons.Filled.WaterDrop,
                contentDescription = null,
                tint = ColorWater,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(10.dp))

            // Texte total + goal
            Text(
                text = buildString {
                    append("%.1f L".format(totalL))
                    goalL?.let { append(" / %.1f L".format(it)) }
                },
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum",
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.weight(1f))

            // − bouton
            WaterIconButton(
                icon = Icons.Filled.Remove,
                contentDescription = "Retirer 250 mL d'eau",
                background = colors.surface3,
                tint = if (isEmpty) colors.textTertiary else MaterialTheme.colorScheme.onSurface,
                enabled = !isEmpty,
                onClick = {
                    if (!isEmpty) onEvent(TodayEvent.OnRemoveWater(amount = DEFAULT_AMOUNT_ML))
                },
            )
            Spacer(Modifier.width(8.dp))
            // + bouton
            WaterIconButton(
                icon = Icons.Filled.Add,
                contentDescription = "Ajouter 250 mL d'eau",
                background = ColorWater.copy(alpha = 0.18f),
                tint = ColorWater,
                onClick = {
                    onEvent(TodayEvent.OnAddWater(amount = DEFAULT_AMOUNT_ML))
                },
            )
            Spacer(Modifier.width(8.dp))
            // 3ème bouton — quantité personnalisée
            WaterIconButton(
                icon = Icons.Filled.Tune,
                contentDescription = "Quantité personnalisée",
                background = colors.surface3,
                tint = MaterialTheme.colorScheme.onSurface,
                onClick = { dialogOpen = true },
            )
        }
    }

    if (dialogOpen) {
        WaterCustomDialog(
            onDismiss = { dialogOpen = false },
            onAdd = { amount ->
                onEvent(TodayEvent.OnAddWater(amount = amount))
                dialogOpen = false
            },
            onRemove = { amount ->
                onEvent(TodayEvent.OnRemoveWater(amount = amount))
                dialogOpen = false
            },
            canRemove = !isEmpty,
        )
    }
}

@Composable
private fun WaterIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    background: Color,
    tint: Color,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        color = background,
        modifier = Modifier.size(40.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun WaterCustomDialog(
    onDismiss: () -> Unit,
    onAdd: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    canRemove: Boolean,
) {
    val colors = LocalStrakkColors.current
    var text by remember { mutableStateOf(DEFAULT_AMOUNT_ML.toString()) }
    val parsed = text.toIntOrNull()
    val isValid = parsed != null && parsed in MIN_AMOUNT_ML..MAX_AMOUNT_ML

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Quantité personnalisée",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
        },
        text = {
            Column {
                Text(
                    text = "Entre $MIN_AMOUNT_ML et $MAX_AMOUNT_ML mL",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textTertiary,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { newValue ->
                        if (newValue.length <= 4 && newValue.all { it.isDigit() }) {
                            text = newValue
                        }
                    },
                    label = { Text("mL") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = text.isNotBlank() && !isValid,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        enabled = isValid && canRemove,
                        onClick = { parsed?.let(onRemove) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Remove,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Retirer")
                    }
                    androidx.compose.material3.Button(
                        enabled = isValid,
                        onClick = { parsed?.let(onAdd) },
                        modifier = Modifier.weight(1f),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = ColorWater,
                            contentColor = Color.White,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Ajouter")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        },
        containerColor = colors.surface2,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF151720)
@Composable
private fun WaterRowPreview() {
    StrakkTheme {
        WaterRow(
            summary = DailySummary(
                totalProtein = 0.0,
                totalCalories = 0.0,
                totalFat = 0.0,
                totalCarbs = 0.0,
                totalWater = 1500,
                proteinGoal = null,
                calorieGoal = null,
                waterGoal = 5000,
            ),
            onEvent = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF151720)
@Composable
private fun WaterRowEmptyPreview() {
    StrakkTheme {
        WaterRow(
            summary = DailySummary(
                totalProtein = 0.0,
                totalCalories = 0.0,
                totalFat = 0.0,
                totalCarbs = 0.0,
                totalWater = 0,
                proteinGoal = null,
                calorieGoal = null,
                waterGoal = 5000,
            ),
            onEvent = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
