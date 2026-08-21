package com.bskai.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BskViolet = Color(0xFF818CF8)
val BskVioletLight = Color(0xFFA5B4FC)
val BskVioletDark = Color(0xFF4F46E5)
val BskEmerald = Color(0xFF34D399)
val BskAmber = Color(0xFFF59E0B)
val BskRose = Color(0xFFF43F5E)
val BskCyan = Color(0xFF22D3EE)
val BskGlassSurface = Color(0x40FFFFFF)
val BskGlassBorder = Color(0x20FFFFFF)

private val DarkColors = darkColorScheme(
    primary = BskViolet,
    onPrimary = Color(0xFF0A0A1A),
    primaryContainer = Color(0xFF2A2550),
    onPrimaryContainer = Color(0xFFE0DDFF),
    secondary = BskEmerald,
    onSecondary = Color(0xFF00231A),
    tertiary = BskAmber,
    background = Color(0xFF0A0914),
    onBackground = Color(0xFFE4E1F7),
    surface = Color(0xFF12101E),
    onSurface = Color(0xFFE4E1F7),
    surfaceVariant = Color(0xFF1A1730),
    onSurfaceVariant = Color(0xFF9B96C0),
    outline = Color(0xFF3D3860),
    error = BskRose,
    surfaceDim = Color(0xFF0D0C18),
    surfaceBright = Color(0xFF2A2548)
)

private val LightColors = lightColorScheme(
    primary = BskViolet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8E6FF),
    onPrimaryContainer = Color(0xFF1A1040),
    secondary = Color(0xFF0E9F6E),
    onSecondary = Color.White,
    tertiary = Color(0xFFF97316),
    background = Color(0xFFF5F3FF),
    onBackground = Color(0xFF1A1830),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1830),
    surfaceVariant = Color(0xFFE8E6F0),
    onSurfaceVariant = Color(0xFF4A4668),
    outline = Color(0xFF7C7899),
    error = Color(0xFFDC2626)
)

@Composable
fun BskTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = BskTypography,
        shapes = BskShapes,
        content = content
    )
}
