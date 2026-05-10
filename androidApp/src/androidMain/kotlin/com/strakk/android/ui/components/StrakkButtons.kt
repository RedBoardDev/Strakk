package com.strakk.android.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.strakk.android.ui.theme.LocalStrakkColors
import com.strakk.android.ui.theme.LocalStrakkRadius
import com.strakk.android.ui.theme.LocalStrakkTextStyles
import com.strakk.android.ui.theme.StrakkTheme

private val ButtonHeight = 52.dp
private val ButtonProgressSize = 22.dp
private val ButtonHorizontalPadding = PaddingValues(horizontal = 16.dp)

// =============================================================================
// Internal base — avoids repeating layout + animation for each variant.
// =============================================================================

@Suppress("LongParameterList")
@Composable
private fun StrakkBaseButton(
    text: String,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    disabledContainerColor: Color,
    disabledContentColor: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val radius = LocalStrakkRadius.current
    val textStyles = LocalStrakkTextStyles.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "buttonScale",
    )

    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(radius.sm),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
        ),
        contentPadding = ButtonHorizontalPadding,
        modifier = modifier
            .fillMaxWidth()
            .height(ButtonHeight)
            .scale(scale),
    ) {
        if (loading) {
            CircularProgressIndicator(
                color = contentColor,
                strokeWidth = 2.dp,
                modifier = Modifier.size(ButtonProgressSize),
            )
        } else {
            Text(text = text, style = textStyles.bodyBold)
        }
    }
}

// =============================================================================
// Public variants
// =============================================================================

/**
 * Primary CTA. Orange background, white text, 52dp height.
 * Disabled state falls back to surface-2 / text-tertiary.
 */
@Composable
fun StrakkPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val colors = LocalStrakkColors.current
    StrakkBaseButton(
        text = text,
        onClick = onClick,
        containerColor = colors.accentOrange,
        contentColor = Color.White,
        disabledContainerColor = colors.surface2,
        disabledContentColor = colors.textTertiary,
        modifier = modifier,
        enabled = enabled,
        loading = loading,
    )
}

/**
 * Secondary button. Surface-2 background, text-primary text, 52dp height.
 */
@Composable
fun StrakkSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val colors = LocalStrakkColors.current
    StrakkBaseButton(
        text = text,
        onClick = onClick,
        containerColor = colors.surface2,
        contentColor = colors.textPrimary,
        disabledContainerColor = colors.surface2.copy(alpha = 0.5f),
        disabledContentColor = colors.textTertiary,
        modifier = modifier,
        enabled = enabled,
        loading = loading,
    )
}

/**
 * Destructive button. Error-red background, white text. Use only for
 * destructive confirmations (delete, discard, sign-out).
 */
@Composable
fun StrakkDestructiveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val colors = LocalStrakkColors.current
    StrakkBaseButton(
        text = text,
        onClick = onClick,
        containerColor = colors.error,
        contentColor = Color.White,
        disabledContainerColor = colors.surface2,
        disabledContentColor = colors.textTertiary,
        modifier = modifier,
        enabled = enabled,
        loading = loading,
    )
}

/**
 * Text/link button. No background. Use for "Skip", "See all", inline links.
 * [emphasized] = true uses bodyBold, false uses body.
 */
@Composable
fun StrakkTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    emphasized: Boolean = true,
) {
    val colors = LocalStrakkColors.current
    val textStyles = LocalStrakkTextStyles.current
    TextButton(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(
            contentColor = colors.accentOrange,
            disabledContentColor = colors.textDisabled,
        ),
        modifier = modifier,
    ) {
        Text(
            text = text,
            style = if (emphasized) textStyles.bodyBold else textStyles.body,
        )
    }
}

// =============================================================================
// Preview
// =============================================================================

@Suppress("UnusedPrivateMember")
@Preview(showBackground = true, backgroundColor = 0xFF050918)
@Composable
private fun StrakkButtonsPreview() {
    StrakkTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            StrakkPrimaryButton(text = "Confirm meal", onClick = {})
            StrakkPrimaryButton(text = "Loading…", onClick = {}, loading = true)
            StrakkPrimaryButton(text = "Disabled", onClick = {}, enabled = false)
            StrakkSecondaryButton(text = "Cancel", onClick = {})
            StrakkDestructiveButton(text = "Delete meal", onClick = {})
            StrakkTextButton(text = "Skip", onClick = {})
            StrakkTextButton(text = "See all", onClick = {}, emphasized = false)
        }
    }
}
