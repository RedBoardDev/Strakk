package com.strakk.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Strakk app theme — dark only for v1.
 *
 * Applies the DESIGN.md color scheme and typography scale.
 * Custom semantic colors available via [LocalStrakkColors].
 * Dynamic color is intentionally disabled — Strakk uses its own warm palette.
 */
@Composable
fun StrakkTheme(
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalStrakkColors provides StrakkSemanticColorsDark,
        LocalStrakkTextStyles provides StrakkTextStylesDefault,
        LocalStrakkSpacing provides StrakkSpacingDefault,
        LocalStrakkRadius provides StrakkRadiusDefault,
    ) {
        MaterialTheme(
            colorScheme = StrakkDarkColorScheme,
            typography = StrakkTypography,
            content = content,
        )
    }
}
