package com.bskai.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BskIndigo = Color(0xFF6366F1)
val BskEmerald = Color(0xFF34D399)
val BskAmber = Color(0xFFF59E0B)
val BskRose = Color(0xFFF43F5E)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8B8FF8),
    onPrimary = Color(0xFF1E1B2E),
    primaryContainer = Color(0xFF3E3A6B),
    onPrimaryContainer = Color(0xFFE0DFFF),
    secondary = BskEmerald,
    onSecondary = Color(0xFF00231A),
    tertiary = BskAmber,
    background = Color(0xFF12101E),
    onBackground = Color(0xFFE9E7F5),
    surface = Color(0xFF1A1830),
    onSurface = Color(0xFFE9E7F5),
    surfaceVariant = Color(0xFF242140),
    onSurfaceVariant = Color(0xFFB4B0D0),
    error = BskRose,
    outline = Color(0xFF4A4670)
)

private val LightColors = lightColorScheme(
    primary = BskIndigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0DFFF),
    onPrimaryContainer = Color(0xFF1E1B2E),
    secondary = Color(0xFF0E9F6E),
    onSecondary = Color.White,
    background = Color(0xFFF7F6FD),
    onBackground = Color(0xFF1B1930),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1B1930),
    surfaceVariant = Color(0xFFE9E7F5),
    onSurfaceVariant = Color(0xFF555073),
    error = Color(0xFFD7263D)
)

@Composable
fun BskTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    accentColor: Color = BskIndigo,
    content: @Composable () -> Unit
) {
    val baseDark = if (accentColor == BskIndigo) DarkColors else DarkColors.copy(primary = accentColor)
    val baseLight = if (accentColor == BskIndigo) LightColors else LightColors.copy(primary = accentColor)
    MaterialTheme(
        colorScheme = if (darkTheme) baseDark else baseLight,
        typography = BskTypography,
        shapes = BskShapes,
        content = content
    )
}
