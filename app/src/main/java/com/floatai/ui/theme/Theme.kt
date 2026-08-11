package com.floatai.ui.theme

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

private val DarkColors = darkColorScheme(
    primary = BrandIndigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2C2C7A),
    onPrimaryContainer = Color(0xFFDDD6FF),
    secondary = BrandTeal,
    onSecondary = Color(0xFF00302C),
    secondaryContainer = Color(0xFF254E4A),
    onSecondaryContainer = Color(0xFFA8F2EA),
    tertiary = BrandPink,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = Color.White,
    surface = DarkSurface,
    onSurface = Color.White,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFEBEBF5),
    outline = Color(0xFF8E8E93),
    error = Color(0xFFFF453A),
    errorContainer = Color(0xFF7A1212),
)

private val LightColors = lightColorScheme(
    primary = BrandIndigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E0FF),
    onPrimaryContainer = Color(0xFF1A1A4D),
    secondary = Color(0xFF008F86),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB8F1EB),
    onSecondaryContainer = Color(0xFF00201D),
    tertiary = Color(0xFFD63384),
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = Color(0xFF1C1C1E),
    surface = LightSurface,
    onSurface = Color(0xFF1C1C1E),
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF48484A),
    outline = Color(0xFF8E8E93),
    error = Color(0xFFFF3B30),
    errorContainer = Color(0xFFFFDAD6),
)

@Composable
fun FloatAITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    accentColor: Color = BrandIndigo,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors.copy(primary = accentColor, tertiary = accentColor)
        else -> LightColors.copy(primary = accentColor, tertiary = accentColor)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
