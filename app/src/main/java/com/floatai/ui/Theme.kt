package com.floatai.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val GlassPurple = Color(0xFF5B2A86)
val GlassDeepPurple = Color(0xFF3E1F6E)
val GlassCyan = Color(0xFF4ECDC4)
val GlassPink = Color(0xFFFF6B6B)
val GlassBackground = Color(0xFF0F0A1E)

private val LightColors = lightColorScheme(
    primary = GlassPurple,
    onPrimary = Color.White,
    secondary = GlassCyan,
    background = Color(0xFFF5F2FA),
    surface = Color(0xFFFFFFFF),
)

private val DarkColors = darkColorScheme(
    primary = GlassPink,
    onPrimary = Color.White,
    secondary = GlassCyan,
    background = GlassBackground,
    surface = Color(0xFF1A1330),
)

@Composable
fun GlassTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
