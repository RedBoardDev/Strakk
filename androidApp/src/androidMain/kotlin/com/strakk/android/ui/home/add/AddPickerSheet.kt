package com.strakk.android.ui.home.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.TextSnippet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.strakk.android.ui.theme.LocalStrakkColors
import com.strakk.android.ui.theme.StrakkTheme

/**
 * Bottom sheet picker for adding items to a Draft or doing a quick-add.
 *
 * @param draftName Non-null when opened in Draft context; null for quick-add.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPickerSheet(
    draftName: String?,
    onSearch: () -> Unit,
    onManual: () -> Unit,
    onText: () -> Unit,
    onPhoto: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = LocalStrakkColors.current.surface2,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = modifier,
    ) {
        AddPickerContent(
            draftName = draftName,
            onSearch = onSearch,
            onManual = onManual,
            onText = onText,
            onPhoto = onPhoto,
        )
    }
}

@Composable
private fun AddPickerContent(
    draftName: String?,
    onSearch: () -> Unit,
    onManual: () -> Unit,
    onText: () -> Unit,
    onPhoto: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
    ) {
        // Header
        Text(
            text = if (draftName != null) "Ajouter à $draftName" else "Ajout rapide",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Choisissez une source",
            style = MaterialTheme.typography.bodySmall,
            color = LocalStrakkColors.current.textSecondary,
        )
        Spacer(modifier = Modifier.height(20.dp))

        // Row 1 — Search, Barcode, Manual
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            PickerTile(
                icon = Icons.Outlined.Search,
                label = "Rechercher",
                description = "Historique + catalogue",
                onClick = onSearch,
                modifier = Modifier.weight(1f),
            )
            PickerTile(
                icon = Icons.Outlined.Create,
                label = "Manuel",
                description = "Saisir les macros",
                onClick = onManual,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Row 2 — Text, Photo (only in draft context, greyed out otherwise)
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            PickerTile(
                icon = Icons.Outlined.TextSnippet,
                label = "Texte libre",
                description = "Décris ton repas",
                onClick = onText,
                enabled = true,
                modifier = Modifier.weight(1f),
            )
            PickerTile(
                icon = Icons.Outlined.CameraAlt,
                label = "Photo + hint",
                description = "Analyse IA",
                onClick = onPhoto,
                enabled = true,
                modifier = Modifier.weight(1f),
            )
            // Spacer to balance the 3+2 layout
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun PickerTile(
    icon: ImageVector,
    label: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val contentAlpha = if (enabled) 1f else 0.38f
    val colors = LocalStrakkColors.current

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        color = colors.surface3,
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(12.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = contentAlpha),
                modifier = Modifier.size(28.dp),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textTertiary.copy(alpha = contentAlpha),
            )
        }
    }
}

// =============================================================================
// Preview
// =============================================================================

@Preview(showBackground = true, backgroundColor = 0xFF242536)
@Composable
private fun AddPickerContentPreview() {
    StrakkTheme {
        AddPickerContent(
            draftName = "Déjeuner",
            onSearch = {},
            onManual = {},
            onText = {},
            onPhoto = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF242536)
@Composable
private fun AddPickerContentQuickAddPreview() {
    StrakkTheme {
        AddPickerContent(
            draftName = null,
            onSearch = {},
            onManual = {},
            onText = {},
            onPhoto = {},
        )
    }
}
