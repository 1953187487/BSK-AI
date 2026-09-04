package com.bskai.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

private val AuraPurple = Color(0xFF6C5CE7)
private val AuraPurpleLight = Color(0xFF8E7CFF)
private val AuraCyan = Color(0xFF00CED1)
private val AuraPink = Color(0xFFFF6B9D)
private val AuraError = Color(0xFFFF4757)

private val DarkColors = darkColorScheme(
    primary = AuraPurpleLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3B2F8F),
    onPrimaryContainer = Color(0xFFEAD6FF),
    secondary = AuraCyan,
    onSecondary = Color(0xFF00383A),
    secondaryContainer = Color(0xFF004F52),
    onSecondaryContainer = Color(0xFF9DF1F3),
    tertiary = AuraPink,
    background = Color(0xFF0D0D1A),
    onBackground = Color(0xFFF0EEFF),
    surface = Color(0xFF14142A),
    onSurface = Color(0xFFF0EEFF),
    surfaceVariant = Color(0xFF1E1E3A),
    onSurfaceVariant = Color(0xFFBDB6E8),
    outline = Color(0xFF3D3C68),
    error = AuraError,
    onError = Color.White
)

private val LightColors = lightColorScheme(
    primary = AuraPurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEAD6FF),
    onPrimaryContainer = Color(0xFF2A1670),
    secondary = Color(0xFF00868A),
    onSecondary = Color.White,
    tertiary = AuraPink,
    background = Color(0xFFF8F7FF),
    onBackground = Color(0xFF17152A),
    surface = Color.White,
    onSurface = Color(0xFF17152A),
    surfaceVariant = Color(0xFFEDE9FF),
    onSurfaceVariant = Color(0xFF4B4766),
    outline = Color(0xFF78718F),
    error = Color(0xFFD32F2F),
    onError = Color.White
)

private val AuraShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

private val AuraTypography = Typography()

@Composable
fun AuraTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = AuraShapes,
        typography = AuraTypography,
        content = content
    )
}

object AuraFonts {
    val Brand: FontFamily get() = FontFamily.SansSerif
}
