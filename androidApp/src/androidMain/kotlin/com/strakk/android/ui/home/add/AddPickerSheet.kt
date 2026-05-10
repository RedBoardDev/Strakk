package com.strakk.android.ui.home.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.strakk.android.R
import com.strakk.android.ui.components.ProBadge
import com.strakk.android.ui.components.StrakkSheet
import com.strakk.android.ui.theme.LocalStrakkColors
import com.strakk.android.ui.theme.StrakkTheme

/**
 * Bottom sheet picker for adding items to a Draft or doing a quick-add.
 * Wrapped in [StrakkSheet] for consistent drag handle, corners, and dismiss semantics.
 *
 * @param draftName Non-null when opened in Draft context; null for quick-add.
 */
@Suppress("LongParameterList")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPickerSheet(
    draftName: String?,
    showProBadges: Boolean,
    onSearch: () -> Unit,
    onManual: () -> Unit,
    onText: () -> Unit,
    onPhoto: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StrakkSheet(
        onDismiss = onDismiss,
        showCloseButton = false,
        modifier = modifier,
    ) {
        AddPickerContent(
            draftName = draftName,
            showProBadges = showProBadges,
            onSearch = onSearch,
            onManual = onManual,
            onText = onText,
            onPhoto = onPhoto,
        )
    }
}

@Suppress("LongMethod", "LongParameterList")
@Composable
private fun AddPickerContent(
    draftName: String?,
    showProBadges: Boolean,
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
            text = if (draftName != null) {
                stringResource(R.string.add_picker_title_draft, draftName)
            } else {
                stringResource(R.string.add_picker_title_quick)
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.add_picker_subtitle),
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
                label = stringResource(R.string.add_picker_search_label),
                description = stringResource(R.string.add_picker_search_desc),
                onClick = onSearch,
                modifier = Modifier.weight(1f),
            )
            PickerTile(
                icon = Icons.Outlined.Create,
                label = stringResource(R.string.add_picker_manual_label),
                description = stringResource(R.string.add_picker_manual_desc),
                onClick = onManual,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Row 2 — Text, Photo (AI-gated — Pro badge overlay)
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(modifier = Modifier.weight(1f)) {
                PickerTile(
                    icon = Icons.Outlined.TextSnippet,
                    label = stringResource(R.string.add_picker_text_label),
                    description = stringResource(R.string.add_picker_text_desc),
                    onClick = onText,
                    enabled = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (showProBadges) {
                    ProBadge(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp),
                    )
                }
            }
            Box(modifier = Modifier.weight(1f)) {
                PickerTile(
                    icon = Icons.Outlined.CameraAlt,
                    label = stringResource(R.string.add_picker_photo_label),
                    description = stringResource(R.string.add_picker_photo_desc),
                    onClick = onPhoto,
                    enabled = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (showProBadges) {
                    ProBadge(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp),
                    )
                }
            }
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
                tint = colors.accentOrange.copy(alpha = contentAlpha),
                modifier = Modifier.size(28.dp),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = colors.textPrimary.copy(alpha = contentAlpha),
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
            draftName = "Lunch",
            showProBadges = true,
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
            showProBadges = true,
            onSearch = {},
            onManual = {},
            onText = {},
            onPhoto = {},
        )
    }
}
