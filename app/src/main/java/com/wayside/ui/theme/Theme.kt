package com.wayside.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class ThemeMode(val label: String) {
    Light("Light"),
    Dark("Dark"),
    System("System"),
}

/**
 * Wayside-specific tokens that Material 3's [ColorScheme] has no slot for.
 */
@Immutable
data class WaysideColors(
    val accent: Color,
    val onAccent: Color,
    val textMuted: Color,
    val success: Color,
    val hairline: Color,
    val mapLand: Color,
    val mapPark: Color,
    val mapWater: Color,
    val mapRoad: Color,
    val isDark: Boolean,
)

private val LightWaysideColors = WaysideColors(
    accent = HoneyAmber,
    onAccent = InkPetrol,
    textMuted = InkPetrol.copy(alpha = 0.60f),
    success = SageSuccess,
    hairline = InkPetrol.copy(alpha = 0.12f),
    mapLand = MapLandLight,
    mapPark = MapParkLight,
    mapWater = MapWaterLight,
    mapRoad = WarmWhite,
    isDark = false,
)

private val DarkWaysideColors = WaysideColors(
    accent = HoneyAmberDark,
    onAccent = InkPetrol,
    textMuted = SandCream.copy(alpha = 0.62f),
    success = Color(0xFF6BA98A),
    hairline = SandCream.copy(alpha = 0.14f),
    mapLand = MapLandDark,
    mapPark = MapParkDark,
    mapWater = MapWaterDark,
    // Surface family, one step up from `surface` — plain SurfaceDark is too close to the
    // dark land colour for the road network to read.
    mapRoad = Color(0xFF2C464D),
    isDark = true,
)

val LocalWaysideColors = staticCompositionLocalOf { LightWaysideColors }

private val LightScheme = lightColorScheme(
    primary = PetrolTeal,
    onPrimary = WarmWhite,
    primaryContainer = Color(0xFFD9E7E9),
    onPrimaryContainer = Color(0xFF0B3B43),
    inversePrimary = PetrolTealDark,
    secondary = HoneyAmber,
    onSecondary = InkPetrol,
    secondaryContainer = Color(0xFFF6E4CB),
    onSecondaryContainer = Color(0xFF4A3410),
    tertiary = CategoryCulture,
    onTertiary = WarmWhite,
    tertiaryContainer = Color(0xFFEEE0F0),
    onTertiaryContainer = Color(0xFF3B1F3F),
    background = SandCream,
    onBackground = InkPetrol,
    surface = WarmWhite,
    onSurface = InkPetrol,
    surfaceVariant = Color(0xFFEDE4D5),
    onSurfaceVariant = InkPetrol.copy(alpha = 0.60f),
    surfaceTint = PetrolTeal,
    inverseSurface = InkPetrol,
    inverseOnSurface = SandCream,
    error = ClayError,
    onError = WarmWhite,
    errorContainer = Color(0xFFF7DED8),
    onErrorContainer = Color(0xFF5C231A),
    outline = InkPetrol.copy(alpha = 0.12f),
    outlineVariant = InkPetrol.copy(alpha = 0.08f),
    scrim = InkPetrol,
    surfaceBright = WarmWhite,
    surfaceDim = Color(0xFFE7DECE),
    surfaceContainerLowest = WarmWhite,
    surfaceContainerLow = Color(0xFFFBF5EA),
    surfaceContainer = Color(0xFFF7F0E3),
    surfaceContainerHigh = Color(0xFFF2EADB),
    surfaceContainerHighest = Color(0xFFEDE4D5),
)

private val DarkScheme = darkColorScheme(
    primary = PetrolTealDark,
    onPrimary = SandCream,
    primaryContainer = Color(0xFF20505A),
    onPrimaryContainer = SandCream,
    inversePrimary = PetrolTeal,
    secondary = HoneyAmberDark,
    onSecondary = InkPetrol,
    secondaryContainer = Color(0xFF4A3410),
    onSecondaryContainer = Color(0xFFF6E4CB),
    tertiary = Color(0xFFB18AB5),
    onTertiary = InkPetrol,
    tertiaryContainer = Color(0xFF3B1F3F),
    onTertiaryContainer = Color(0xFFEEE0F0),
    background = InkPetrol,
    onBackground = SandCream,
    surface = SurfaceDark,
    onSurface = SandCream,
    surfaceVariant = Color(0xFF2A4249),
    onSurfaceVariant = SandCream.copy(alpha = 0.62f),
    surfaceTint = PetrolTealDark,
    inverseSurface = SandCream,
    inverseOnSurface = InkPetrol,
    error = Color(0xFFD87A66),
    onError = InkPetrol,
    errorContainer = Color(0xFF5C231A),
    onErrorContainer = Color(0xFFF7DED8),
    outline = SandCream.copy(alpha = 0.14f),
    outlineVariant = SandCream.copy(alpha = 0.10f),
    scrim = Color(0xFF060F12),
    surfaceBright = Color(0xFF2A4249),
    surfaceDim = InkPetrol,
    surfaceContainerLowest = Color(0xFF112126),
    surfaceContainerLow = Color(0xFF1A2E34),
    surfaceContainer = SurfaceDark,
    surfaceContainerHigh = Color(0xFF243C43),
    surfaceContainerHighest = Color(0xFF2A4249),
)

@Composable
fun WaysideTheme(
    themeMode: ThemeMode = ThemeMode.System,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
        ThemeMode.System -> isSystemInDarkTheme()
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    CompositionLocalProvider(
        LocalWaysideColors provides if (dark) DarkWaysideColors else LightWaysideColors,
    ) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = WaysideTypography,
            shapes = WaysideShapes,
            content = content,
        )
    }
}
