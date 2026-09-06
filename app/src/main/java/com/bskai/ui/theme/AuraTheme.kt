package com.bskai.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Modern Material 3 color palette with glassmorphism support
private val DarkColors = darkColorScheme(
    primary = Color(0xFF8E7CFF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3B2F8F),
    onPrimaryContainer = Color(0xFFEAD6FF),
    secondary = Color(0xFF00CED1),
    onSecondary = Color(0xFF00383A),
    secondaryContainer = Color(0xFF004F52),
    onSecondaryContainer = Color(0xFF9DF1F3),
    tertiary = Color(0xFFFF6B9D),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF5A0030),
    onTertiaryContainer = Color(0xFFFFD0E6),
    background = Color(0xFF0A0A1A),
    onBackground = Color(0xFFF0EEFF),
    surface = Color(0xFF14142A),
    onSurface = Color(0xFFF0EEFF),
    surfaceVariant = Color(0xFF1E1E3A),
    onSurfaceVariant = Color(0xFFBDB6E8),
    outline = Color(0xFF3D3C68),
    outlineVariant = Color(0xFF2D2C58),
    error = Color(0xFFFF4757),
    onError = Color.White,
    surfaceContainerHighest = Color(0xFF2A2A4A),
    surfaceContainerHigh = Color(0xFF1E1E3A),
    surfaceContainer = Color(0xFF161628),
    surfaceContainerLow = Color(0xFF121220),
    surfaceContainerLowest = Color(0xFF080818),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF6C5CE7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEAD6FF),
    onPrimaryContainer = Color(0xFF2A1670),
    secondary = Color(0xFF00868A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB2F0F2),
    onSecondaryContainer = Color(0xFF00383A),
    tertiary = Color(0xFFE91E63),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFCE4EC),
    onTertiaryContainer = Color(0xFF4A0020),
    background = Color(0xFFF8F7FF),
    onBackground = Color(0xFF17152A),
    surface = Color.White,
    onSurface = Color(0xFF17152A),
    surfaceVariant = Color(0xFFEDE9FF),
    onSurfaceVariant = Color(0xFF4B4766),
    outline = Color(0xFF78718F),
    outlineVariant = Color(0xFFBDB6E8),
    error = Color(0xFFD32F2F),
    onError = Color.White,
)

@Composable
fun AuraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
