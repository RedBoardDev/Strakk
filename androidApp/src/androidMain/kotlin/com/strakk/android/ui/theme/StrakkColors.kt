package com.strakk.android.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// =============================================================================
// Raw color tokens — single source of truth, mirror of DESIGN.md §2.
// Never hard-code a hex outside this file.
// =============================================================================

// ---- Background & surfaces ----
val ColorBackground = Color(0xFF050918)
val ColorBackgroundElevated = Color(0xFF080D1F)
val ColorBackgroundEdge = Color(0xFF0B1028)
val ColorSurface1 = Color(0xFF10162F)
val ColorSurface1GradientTop = Color(0xFF121833)
val ColorSurface1GradientBottom = Color(0xFF0C1127)
val ColorSurface2 = Color(0xFF151B38)
val ColorSurface3 = Color(0xFF1A2142)

// ---- Borders & dividers ----
val ColorBorderSubtle = Color(0x407D89BE)        // rgba(125, 137, 190, 0.25)
val ColorBorderFaint = Color(0x2E858FBE)         // rgba(133, 143, 190, 0.18)
val ColorDividerStrong = Color(0x38969DC8)       // rgba(150, 157, 200, 0.22)
val ColorDividerWeak = Color(0x1FFFFFFF)         // rgba(255, 255, 255, 0.12)

// ---- Text ----
val ColorTextPrimary = Color(0xFFF4F6FF)
val ColorTextSecondary = Color(0xFF9CA1B8)
val ColorTextTertiary = Color(0xFF6F748C)
val ColorTextDisabled = Color(0xFF50566F)

// ---- Accent — Orange (primary, protein, Rapide) ----
val ColorAccentOrange = Color(0xFFFF7A3D)
val ColorAccentOrangeLight = Color(0xFFFF9A55)
val ColorAccentOrangeGlow = Color(0x59FF7A3D)
val ColorAccentOrangeFaint = Color(0x14FF7A3D)
val ColorAccentOrangeBorder = Color(0x2EFF7A3D)

// ---- Accent — Blue (water) ----
val ColorAccentBlue = Color(0xFF4B8DFF)
val ColorAccentBlueLight = Color(0xFF67B7FF)
val ColorAccentBlueGlow = Color(0x594B8DFF)
val ColorAccentBlueFaint = Color(0x1F4B8DFF)
val ColorAccentBlueBorder = Color(0x474B8DFF)

// ---- Accent — Yellow (lipids) ----
val ColorAccentYellow = Color(0xFFFFC84D)
val ColorAccentYellowFaint = Color(0x1AFFC84D)
val ColorAccentYellowBorder = Color(0x38FFC84D)

// ---- Accent — Indigo (carbs) ----
val ColorAccentIndigo = Color(0xFF637CFF)
val ColorAccentIndigoFaint = Color(0x1A637CFF)
val ColorAccentIndigoBorder = Color(0x38637CFF)

// ---- Semantic ----
val ColorSuccess = Color(0xFF4DAE6A)
val ColorError = Color(0xFFE05252)
val ColorWarning = Color(0xFFE0A84D)

// ---- Legacy alias kept for transitional callers ----
val ColorWater = ColorAccentBlue

// =============================================================================
// Material 3 dark color scheme — wires the M3 names to our tokens.
// =============================================================================

val StrakkDarkColorScheme = darkColorScheme(
    primary = ColorAccentOrange,
    onPrimary = Color.White,
    primaryContainer = ColorAccentOrangeFaint,
    onPrimaryContainer = ColorAccentOrange,
    secondary = ColorAccentBlue,
    onSecondary = Color.White,
    secondaryContainer = ColorAccentBlueFaint,
    onSecondaryContainer = ColorAccentBlueLight,
    tertiary = ColorAccentYellow,
    background = ColorBackground,
    onBackground = ColorTextPrimary,
    surface = ColorSurface1,
    onSurface = ColorTextPrimary,
    surfaceVariant = ColorSurface2,
    onSurfaceVariant = ColorTextSecondary,
    outline = ColorBorderSubtle,
    outlineVariant = ColorDividerWeak,
    error = ColorError,
    onError = Color.White,
)

// =============================================================================
// Strakk semantic colors — domain tokens. Access via LocalStrakkColors.current.
// =============================================================================

@Immutable
data class StrakkSemanticColors(
    // Surfaces
    val background: Color,
    val backgroundElevated: Color,
    val backgroundEdge: Color,
    val surface1: Color,
    val surface1GradientTop: Color,
    val surface1GradientBottom: Color,
    val surface2: Color,
    val surface3: Color,
    // Borders
    val borderSubtle: Color,
    val borderFaint: Color,
    val dividerStrong: Color,
    val dividerWeak: Color,
    // Text
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textDisabled: Color,
    // Orange
    val accentOrange: Color,
    val accentOrangeLight: Color,
    val accentOrangeGlow: Color,
    val accentOrangeFaint: Color,
    val accentOrangeBorder: Color,
    // Blue
    val accentBlue: Color,
    val accentBlueLight: Color,
    val accentBlueGlow: Color,
    val accentBlueFaint: Color,
    val accentBlueBorder: Color,
    // Yellow
    val accentYellow: Color,
    val accentYellowFaint: Color,
    val accentYellowBorder: Color,
    // Indigo
    val accentIndigo: Color,
    val accentIndigoFaint: Color,
    val accentIndigoBorder: Color,
    // Semantic
    val success: Color,
    val error: Color,
    val warning: Color,
    // Aliases (legacy)
    val water: Color,
    val protein: Color,
    val calories: Color,
    val divider: Color,
)

val StrakkSemanticColorsDark = StrakkSemanticColors(
    background = ColorBackground,
    backgroundElevated = ColorBackgroundElevated,
    backgroundEdge = ColorBackgroundEdge,
    surface1 = ColorSurface1,
    surface1GradientTop = ColorSurface1GradientTop,
    surface1GradientBottom = ColorSurface1GradientBottom,
    surface2 = ColorSurface2,
    surface3 = ColorSurface3,
    borderSubtle = ColorBorderSubtle,
    borderFaint = ColorBorderFaint,
    dividerStrong = ColorDividerStrong,
    dividerWeak = ColorDividerWeak,
    textPrimary = ColorTextPrimary,
    textSecondary = ColorTextSecondary,
    textTertiary = ColorTextTertiary,
    textDisabled = ColorTextDisabled,
    accentOrange = ColorAccentOrange,
    accentOrangeLight = ColorAccentOrangeLight,
    accentOrangeGlow = ColorAccentOrangeGlow,
    accentOrangeFaint = ColorAccentOrangeFaint,
    accentOrangeBorder = ColorAccentOrangeBorder,
    accentBlue = ColorAccentBlue,
    accentBlueLight = ColorAccentBlueLight,
    accentBlueGlow = ColorAccentBlueGlow,
    accentBlueFaint = ColorAccentBlueFaint,
    accentBlueBorder = ColorAccentBlueBorder,
    accentYellow = ColorAccentYellow,
    accentYellowFaint = ColorAccentYellowFaint,
    accentYellowBorder = ColorAccentYellowBorder,
    accentIndigo = ColorAccentIndigo,
    accentIndigoFaint = ColorAccentIndigoFaint,
    accentIndigoBorder = ColorAccentIndigoBorder,
    success = ColorSuccess,
    error = ColorError,
    warning = ColorWarning,
    water = ColorAccentBlue,
    protein = ColorAccentOrange,
    calories = ColorAccentOrangeLight,
    divider = ColorDividerWeak,
)

val LocalStrakkColors = staticCompositionLocalOf { StrakkSemanticColorsDark }
