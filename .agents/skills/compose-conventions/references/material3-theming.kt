package com.strakk.androidApp.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// =============================================================================
// Color tokens — NEVER hardcode hex in composables
// =============================================================================

private val StrakkGreen = Color(0xFF4CAF50)
private val StrakkGreenDark = Color(0xFF81C784)
private val StrakkOrange = Color(0xFFFF9800)
private val StrakkOrangeDark = Color(0xFFFFB74D)
private val StrakkRed = Color(0xFFE53935)
private val StrakkRedDark = Color(0xFFEF5350)

private val LightColorScheme = lightColorScheme(
    primary = StrakkGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC8E6C9),
    onPrimaryContainer = Color(0xFF1B5E20),
    secondary = StrakkOrange,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE0B2),
    onSecondaryContainer = Color(0xFFE65100),
    error = StrakkRed,
    onError = Color.White,
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
)

private val DarkColorScheme = darkColorScheme(
    primary = StrakkGreenDark,
    onPrimary = Color(0xFF1B5E20),
    primaryContainer = Color(0xFF2E7D32),
    onPrimaryContainer = Color(0xFFC8E6C9),
    secondary = StrakkOrangeDark,
    onSecondary = Color(0xFFE65100),
    secondaryContainer = Color(0xFFEF6C00),
    onSecondaryContainer = Color(0xFFFFE0B2),
    error = StrakkRedDark,
    onError = Color(0xFF601410),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
)

// =============================================================================
// Typography scale
// =============================================================================

private val StrakkTypography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp,
    ),
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
)

// =============================================================================
// Custom semantic colors — fitness app specific
// =============================================================================

/**
 * Semantic colors beyond Material 3 defaults.
 *
 * Use these for domain-specific coloring (workout intensity, macros, etc.).
 * Access via `LocalStrakkColors.current`.
 */
@Immutable
data class StrakkColors(
    val caloriesBurned: Color,
    val protein: Color,
    val carbs: Color,
    val fat: Color,
    val intensityLow: Color,
    val intensityMedium: Color,
    val intensityHigh: Color,
    val restTimer: Color,
    val personalRecord: Color,
    val streakActive: Color,
    val streakInactive: Color,
)

private val LightStrakkColors = StrakkColors(
    caloriesBurned = Color(0xFFFF5722),
    protein = Color(0xFF2196F3),
    carbs = Color(0xFFFF9800),
    fat = Color(0xFF9C27B0),
    intensityLow = Color(0xFF4CAF50),
    intensityMedium = Color(0xFFFF9800),
    intensityHigh = Color(0xFFE53935),
    restTimer = Color(0xFF00BCD4),
    personalRecord = Color(0xFFFFD600),
    streakActive = Color(0xFFFF9800),
    streakInactive = Color(0xFFBDBDBD),
)

private val DarkStrakkColors = StrakkColors(
    caloriesBurned = Color(0xFFFF8A65),
    protein = Color(0xFF64B5F6),
    carbs = Color(0xFFFFB74D),
    fat = Color(0xFFCE93D8),
    intensityLow = Color(0xFF81C784),
    intensityMedium = Color(0xFFFFB74D),
    intensityHigh = Color(0xFFEF5350),
    restTimer = Color(0xFF4DD0E1),
    personalRecord = Color(0xFFFFEA00),
    streakActive = Color(0xFFFFB74D),
    streakInactive = Color(0xFF757575),
)

val LocalStrakkColors = staticCompositionLocalOf { LightStrakkColors }

// =============================================================================
// Theme composable
// =============================================================================

/**
 * Strakk app theme.
 *
 * - Supports dynamic color on Android 12+ (API 31)
 * - Falls back to custom color scheme on older devices
 * - Provides custom [StrakkColors] via [LocalStrakkColors]
 */
@Composable
fun StrakkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val strakkColors = if (darkTheme) DarkStrakkColors else LightStrakkColors

    androidx.compose.runtime.CompositionLocalProvider(
        LocalStrakkColors provides strakkColors,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = StrakkTypography,
            content = content,
        )
    }
}

// =============================================================================
// Usage example in composables
// =============================================================================

// @Composable
// fun MacroBar(protein: Float, carbs: Float, fat: Float) {
//     val colors = LocalStrakkColors.current
//     Row {
//         MacroSegment(value = protein, color = colors.protein, label = "Protein")
//         MacroSegment(value = carbs, color = colors.carbs, label = "Carbs")
//         MacroSegment(value = fat, color = colors.fat, label = "Fat")
//     }
// }
//
// @Composable
// fun IntensityBadge(level: IntensityLevel) {
//     val colors = LocalStrakkColors.current
//     val badgeColor = when (level) {
//         IntensityLevel.LOW -> colors.intensityLow
//         IntensityLevel.MEDIUM -> colors.intensityMedium
//         IntensityLevel.HIGH -> colors.intensityHigh
//     }
//     Surface(color = badgeColor) { Text(level.name) }
// }
