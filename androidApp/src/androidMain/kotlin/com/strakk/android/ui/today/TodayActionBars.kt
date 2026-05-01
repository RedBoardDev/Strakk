package com.strakk.android.ui.today

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import com.strakk.android.ui.theme.LocalStrakkColors
import com.strakk.shared.domain.model.ActiveMealDraft
import com.strakk.shared.domain.model.DraftItem

@Composable
internal fun ActionButtonsBar(
    onNewMeal: () -> Unit,
    onQuickAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalStrakkColors.current

    Surface(
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
        modifier = modifier,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Button(
                onClick = onNewMeal,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.surface2,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Restaurant,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "Repas",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                )
            }

            Button(
                onClick = onQuickAdd,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Bolt,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "Rapide",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                )
            }
        }
    }
}

@Composable
internal fun DraftFloatingBar(
    draft: ActiveMealDraft,
    onTap: () -> Unit,
    onAdd: () -> Unit,
    onFinish: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val resolvedCount = draft.items.count { it is DraftItem.Resolved }
    val pendingCount = draft.items.size - resolvedCount
    val totalKcal = draft.items
        .filterIsInstance<DraftItem.Resolved>()
        .sumOf { it.entry.calories }
        .toInt()
    val isEmpty = draft.items.isEmpty()

    val itemsLabel = if (isEmpty) {
        "Aucun item · ajoute pour commencer"
    } else buildString {
        append("$resolvedCount items")
        if (pendingCount > 0) append(" + $pendingCount en attente")
        append(" · $totalKcal kcal")
    }

    Surface(
        onClick = onTap,
        color = LocalStrakkColors.current.surface2,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = draft.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = itemsLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalStrakkColors.current.textSecondary,
                )
            }
            Surface(
                onClick = onAdd,
                shape = RoundedCornerShape(8.dp),
                color = LocalStrakkColors.current.surface3,
            ) {
                Text(
                    text = "+ Ajouter",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
            Spacer(modifier = Modifier.size(8.dp))
            Surface(
                onClick = if (isEmpty) onDiscard else onFinish,
                shape = RoundedCornerShape(8.dp),
                color = if (isEmpty) {
                    LocalStrakkColors.current.surface3
                } else {
                    MaterialTheme.colorScheme.primary
                },
            ) {
                Text(
                    text = if (isEmpty) "Annuler" else "Terminer",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isEmpty) {
                        LocalStrakkColors.current.textSecondary
                    } else {
                        MaterialTheme.colorScheme.onPrimary
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}
