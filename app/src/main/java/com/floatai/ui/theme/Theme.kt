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
    primary = BrandPurple,
    onPrimary = Color.White,
    primaryContainer = BrandDeepPurple,
    onPrimaryContainer = Color(0xFFEDE3FF),
    secondary = BrandCyan,
    onSecondary = Color(0xFF00302C),
    secondaryContainer = Color(0xFF254E4A),
    onSecondaryContainer = Color(0xFFA8F2EA),
    tertiary = BrandPink,
    background = DarkBackground,
    onBackground = Color(0xFFE8E2F4),
    surface = DarkSurface,
    onSurface = Color(0xFFE8E2F4),
    surfaceVariant = DarkSurfaceHigh,
    onSurfaceVariant = Color(0xFFC7BEDB),
    outline = Color(0xFF8D83A5),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
)

private val LightColors = lightColorScheme(
    primary = BrandPurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE0FF),
    onPrimaryContainer = Color(0xFF250045),
    secondary = Color(0xFF3B6E69),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFBFF4EC),
    onSecondaryContainer = Color(0xFF00201D),
    tertiary = Color(0xFFB3457B),
    background = LightBackground,
    onBackground = Color(0xFF1C1B20),
    surface = LightSurface,
    onSurface = Color(0xFF1C1B20),
    surfaceVariant = Color(0xFFE7E1F0),
    onSurfaceVariant = Color(0xFF494353),
    outline = Color(0xFF7B7386),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
)

/**
 * 应用主题入口。
 *
 * @param darkTheme 是否使用深色主题
 * @param dynamicColor 是否启用 Android 12+ 动态取色（基于壁纸）
 * @param accentColor 自定义主题色（动态取色关闭时作为主色 seed）
 */
@Composable
fun FloatAITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    accentColor: Color = BrandPurple,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors.copy(primary = accentColor)
        else -> LightColors.copy(primary = accentColor)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
