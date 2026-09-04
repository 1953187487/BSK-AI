package com.bskai.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val AuraPrimary = Color(0xFF6C5CE7)
val AuraSecondary = Color(0xFF00CED1)
val AuraTertiary = Color(0xFFFF6B9D)
val AuraError = Color(0xFF5729)
val AuraSurfaceGlow = Color(0xFF6C5CE740)

private val DarkColorScheme = darkColorScheme(
    primary = AuraPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEAD6FF),
    onPrimaryContainer = Color(0xFF1A0D3E),
    secondary = AuraSecondary,
    onSecondary = Color(0xFF003336),
    tertiary = AuraTertiary,
    onTertiary = Color.White,
    background = Color(0xFF0D0D1A),
    onBackground = Color(0xFFF0EEFF),
    surface = Color(0xFF16162A),
    onSurface = Color(0xFFF0EEFF),
    surfaceVariant = Color(0xFF1E1E3A),
    onSurfaceVariant = Color(0xFFB8B6D0),
    error = AuraError,
    outline = Color(0xFF3D3C68)
)

private val LightColorScheme = lightColorScheme(
    primary = AuraPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEAD6FF),
    onPrimaryContainer = Color(0xFF1A0D3E),
    secondary = Color(0xFF00838A),
    onSecondary = Color.White,
    tertiary = Color(0xFFD44277),
    onTertiary = Color.White,
    background = Color(0xFFF5F3FF),
    onBackground = Color(0xFF1B1930),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1B1930),
    surfaceVariant = Color(0xFFE6E0F5),
    onSurfaceVariant = Color(0xFF484460),
    error = Color(0xFFB3261E)
)

@Composable
fun AuraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AuraTypography,
        shapes = AuraShapes,
        content = content
    )
}
