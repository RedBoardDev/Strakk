package com.strakk.android.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// =============================================================================
// Canonical Strakk text styles (DESIGN.md §3.1) — declared first so the M3
// Typography below can reference them.
// =============================================================================

val StrakkDisplayHero = TextStyle(
    fontWeight = FontWeight.ExtraBold,
    fontSize = 56.sp,
    lineHeight = (56 * 1.05).sp,
    letterSpacing = (-0.4).sp,
)

val StrakkDisplay = TextStyle(
    fontWeight = FontWeight.Bold,
    fontSize = 32.sp,
    lineHeight = (32 * 1.1).sp,
    letterSpacing = 0.sp,
)

val StrakkHeading1 = TextStyle(
    fontWeight = FontWeight.Bold,
    fontSize = 24.sp,
    lineHeight = (24 * 1.2).sp,
    letterSpacing = 0.sp,
)

val StrakkHeading2 = TextStyle(
    fontWeight = FontWeight.Bold,
    fontSize = 20.sp,
    lineHeight = (20 * 1.2).sp,
    letterSpacing = 0.sp,
)

val StrakkHeading3 = TextStyle(
    fontWeight = FontWeight.SemiBold,
    fontSize = 17.sp,
    lineHeight = (17 * 1.25).sp,
    letterSpacing = 0.sp,
)

val StrakkBodyLarge = TextStyle(
    fontWeight = FontWeight.Medium,
    fontSize = 17.sp,
    lineHeight = (17 * 1.3).sp,
    letterSpacing = 0.sp,
)

val StrakkBody = TextStyle(
    fontWeight = FontWeight.Normal,
    fontSize = 15.sp,
    lineHeight = (15 * 1.4).sp,
    letterSpacing = 0.sp,
)

val StrakkBodyBold = TextStyle(
    fontWeight = FontWeight.SemiBold,
    fontSize = 15.sp,
    lineHeight = (15 * 1.4).sp,
    letterSpacing = 0.sp,
)

val StrakkCaption = TextStyle(
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    lineHeight = (13 * 1.4).sp,
    letterSpacing = 0.sp,
)

val StrakkCaptionBold = TextStyle(
    fontWeight = FontWeight.SemiBold,
    fontSize = 13.sp,
    lineHeight = (13 * 1.4).sp,
    letterSpacing = 0.sp,
)

val StrakkOverline = TextStyle(
    fontWeight = FontWeight.Bold,
    fontSize = 11.sp,
    lineHeight = (11 * 1.4).sp,
    letterSpacing = 1.sp,
)

// =============================================================================
// Material 3 Typography wiring — provides sensible defaults for library
// components (DatePicker, etc.). Prefer LocalStrakkTextStyles for app code.
// =============================================================================

val StrakkTypography = Typography(
    displayLarge = StrakkDisplayHero,
    displayMedium = StrakkDisplay,
    headlineLarge = StrakkHeading1,
    headlineMedium = StrakkHeading2,
    titleLarge = StrakkHeading3,
    titleMedium = StrakkHeading3,
    titleSmall = StrakkBodyBold,
    bodyLarge = StrakkBody,
    bodyMedium = StrakkBody,
    bodySmall = StrakkCaption,
    labelLarge = StrakkBodyBold,
    labelMedium = StrakkCaptionBold,
    labelSmall = StrakkOverline,
)

// =============================================================================
// Strakk text-styles bundle exposed via composition local
// =============================================================================

@Immutable
data class StrakkTextStyles(
    val displayHero: TextStyle,
    val display: TextStyle,
    val heading1: TextStyle,
    val heading2: TextStyle,
    val heading3: TextStyle,
    val bodyLarge: TextStyle,
    val body: TextStyle,
    val bodyBold: TextStyle,
    val caption: TextStyle,
    val captionBold: TextStyle,
    val overline: TextStyle,
)

val StrakkTextStylesDefault = StrakkTextStyles(
    displayHero = StrakkDisplayHero,
    display = StrakkDisplay,
    heading1 = StrakkHeading1,
    heading2 = StrakkHeading2,
    heading3 = StrakkHeading3,
    bodyLarge = StrakkBodyLarge,
    body = StrakkBody,
    bodyBold = StrakkBodyBold,
    caption = StrakkCaption,
    captionBold = StrakkCaptionBold,
    overline = StrakkOverline,
)

val LocalStrakkTextStyles = staticCompositionLocalOf { StrakkTextStylesDefault }
