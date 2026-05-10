package com.strakk.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.strakk.android.ui.theme.LocalStrakkColors
import com.strakk.android.ui.theme.LocalStrakkSpacing
import com.strakk.android.ui.theme.LocalStrakkTextStyles
import com.strakk.android.ui.theme.StrakkTheme

/**
 * Centered empty state: optional icon, heading-3 title, optional caption, optional CTA.
 * Per DESIGN.md §5 — center alignment is allowed only in empty/loading/error states.
 */
private const val ACTION_BUTTON_WIDTH_FRACTION = 0.6f

@Suppress("LongParameterList")
@Composable
fun StrakkEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: ImageVector? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = LocalStrakkColors.current
    val spacing = LocalStrakkSpacing.current
    val textStyles = LocalStrakkTextStyles.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.xxl, vertical = spacing.xxxl),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.height(spacing.md))
        }

        Text(
            text = title,
            style = textStyles.heading3,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )

        if (description != null) {
            Spacer(Modifier.height(spacing.xs))
            Text(
                text = description,
                style = textStyles.caption,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }

        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(spacing.lg))
            StrakkPrimaryButton(
                text = actionLabel,
                onClick = onAction,
                modifier = Modifier.fillMaxWidth(ACTION_BUTTON_WIDTH_FRACTION),
            )
        }
    }
}

@Suppress("UnusedPrivateMember")
@Preview(showBackground = true, backgroundColor = 0xFF050918)
@Composable
private fun StrakkEmptyStatePreview() {
    StrakkTheme {
        StrakkEmptyState(
            title = "Nothing logged today",
            description = "Tap + to log your first meal.",
            icon = Icons.Outlined.Inbox,
            actionLabel = "Add a meal",
            onAction = {},
        )
    }
}

@Suppress("UnusedPrivateMember")
@Preview(showBackground = true, backgroundColor = 0xFF050918)
@Composable
private fun StrakkEmptyStateMinimalPreview() {
    StrakkTheme {
        StrakkEmptyState(title = "No results")
    }
}
