package com.strakk.android.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Corner-radius scale — mirror of DESIGN.md §5.
 */
@Immutable
data class StrakkRadius(
    val sm: Dp = 12.dp,
    val md: Dp = 18.dp,
    val lg: Dp = 24.dp,
    val xl: Dp = 32.dp,
    val xxl: Dp = 36.dp,
    val xxxl: Dp = 56.dp,
)

val StrakkRadiusDefault = StrakkRadius()

val LocalStrakkRadius = staticCompositionLocalOf { StrakkRadiusDefault }
