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
import com.bskai.data.ThemeStyle

// ───── Aurora (default) ─────
private val AuroraDark = darkColorScheme(
    primary = Color(0xFF8E7CFF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3B2F8F),
    onPrimaryContainer = Color(0xFFEAD6FF),
    secondary = Color(0xFF00CED1),
    onSecondary = Color(0xFF00383A),
    secondaryContainer = Color(0xFF004F52),
    onSecondaryContainer = Color(0xFF9DF1F3),
    tertiary = Color(0xFFFF6B9D),
    background = Color(0xFF0D0D1A),
    onBackground = Color(0xFFF0EEFF),
    surface = Color(0xFF14142A),
    onSurface = Color(0xFFF0EEFF),
    surfaceVariant = Color(0xFF1E1E3A),
    onSurfaceVariant = Color(0xFFBDB6E8),
    outline = Color(0xFF3D3C68),
    error = Color(0xFFFF4757),
    onError = Color.White
)
private val AuroraLight = lightColorScheme(
    primary = Color(0xFF6C5CE7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEAD6FF),
    onPrimaryContainer = Color(0xFF2A1670),
    secondary = Color(0xFF00868A),
    onSecondary = Color.White,
    tertiary = Color(0xFFFF6B9D),
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
val AuroraGradient = listOf(Color(0xFF6C5CE7), Color(0xFF00CED1), Color(0xFFFF6B9D))

// ───── Neon ─────
private val NeonDark = darkColorScheme(
    primary = Color(0xFFFF2D95),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF5A0030),
    onPrimaryContainer = Color(0xFFFFD0E6),
    secondary = Color(0xFF00F0FF),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF003E45),
    onSecondaryContainer = Color(0xFFA8F8FF),
    tertiary = Color(0xFFE6FF00),
    background = Color(0xFF060014),
    onBackground = Color(0xFFE8F8FF),
    surface = Color(0xFF0F0028),
    onSurface = Color(0xFFE8F8FF),
    surfaceVariant = Color(0xFF1A0040),
    onSurfaceVariant = Color(0xFFA6B5FF),
    outline = Color(0xFF3D0080),
    error = Color(0xFFFF1744),
    onError = Color.Black
)
val NeonGlow = listOf(Color(0xFFFF2D95), Color(0xFF00F0FF))

// ───── Glass ─────
private val GlassDark = darkColorScheme(
    primary = Color(0xFFB8B0FF),
    onPrimary = Color(0xFF18103B),
    primaryContainer = Color(0xFF3A2E80),
    onPrimaryContainer = Color(0xFFEBE4FF),
    secondary = Color(0xFF80E1FF),
    onSecondary = Color(0xFF002A3A),
    secondaryContainer = Color(0xFF00405A),
    onSecondaryContainer = Color(0xFFCDECFF),
    tertiary = Color(0xFFFFB8DD),
    background = Color(0xFF08060F),
    onBackground = Color(0xFFE8E5FF),
    surface = Color(0xCC14122A),
    onSurface = Color(0xFFE8E5FF),
    surfaceVariant = Color(0xCC1F1B3A),
    onSurfaceVariant = Color(0xFFB6AFD8),
    outline = Color(0xFF4F4682),
    error = Color(0xFFFF6E8A),
    onError = Color.White
)
private val GlassLight = lightColorScheme(
    primary = Color(0xFF7B6FE6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDD5FF),
    onPrimaryContainer = Color(0xFF221258),
    secondary = Color(0xFF1AA9D6),
    onSecondary = Color.White,
    tertiary = Color(0xFFE9579C),
    background = Color(0xFFEFEEF8),
    onBackground = Color(0xFF1B1730),
    surface = Color(0xCCFFFFFF),
    onSurface = Color(0xFF1B1730),
    surfaceVariant = Color(0xCCE7E2F8),
    onSurfaceVariant = Color(0xFF4D466C),
    outline = Color(0xFF8E87AE),
    error = Color(0xFFD32F2F),
    onError = Color.White
)
val GlassTint = Color(0x66FFFFFF)

// ───── Voice (large button, single-purpose) ─────
private val VoiceDark = darkColorScheme(
    primary = Color(0xFFFF5252),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF6F1010),
    onPrimaryContainer = Color(0xFFFFD6D6),
    secondary = Color(0xFFFFD740),
    onSecondary = Color(0xFF1A1000),
    secondaryContainer = Color(0xFF665000),
    onSecondaryContainer = Color(0xFFFFF0B0),
    tertiary = Color(0xFF80CBC4),
    background = Color(0xFF0A0A12),
    onBackground = Color(0xFFFFEBEE),
    surface = Color(0xFF12121E),
    onSurface = Color(0xFFFFEBEE),
    surfaceVariant = Color(0xFF1E1E2A),
    onSurfaceVariant = Color(0xFFCFC4C8),
    outline = Color(0xFF5C2A2A),
    error = Color(0xFFFF5252),
    onError = Color.White
)
private val VoiceLight = lightColorScheme(
    primary = Color(0xFFE53935),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD6D6),
    onPrimaryContainer = Color(0xFF5C0000),
    secondary = Color(0xFFF9A825),
    onSecondary = Color.White,
    tertiary = Color(0xFF00695C),
    background = Color(0xFFFFF8F8),
    onBackground = Color(0xFF221010),
    surface = Color.White,
    onSurface = Color(0xFF221010),
    surfaceVariant = Color(0xFFF8E5E5),
    onSurfaceVariant = Color(0xFF5C4242),
    outline = Color(0xFFA78686),
    error = Color(0xFFD32F2F),
    onError = Color.White
)

private val SoftShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

private val SharpShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(20.dp)
)

private val PillShapes = Shapes(
    extraSmall = RoundedCornerShape(20.dp),
    small = RoundedCornerShape(40.dp),
    medium = RoundedCornerShape(60.dp),
    large = RoundedCornerShape(80.dp),
    extraLarge = RoundedCornerShape(120.dp)
)

private val SimpleTypography = Typography()

@Composable
fun AuraTheme(
    darkTheme: Boolean = true,
    themeStyle: ThemeStyle = ThemeStyle.AURORA,
    content: @Composable () -> Unit
) {
    val (colors, shapes) = when (themeStyle) {
        ThemeStyle.AURORA -> (if (darkTheme) AuroraDark else AuroraLight) to SoftShapes
        ThemeStyle.NEON -> NeonDark to SharpShapes
        ThemeStyle.GLASS -> (if (darkTheme) GlassDark else GlassLight) to SoftShapes
        ThemeStyle.VOICE -> (if (darkTheme) VoiceDark else VoiceLight) to PillShapes
    }
    MaterialTheme(
        colorScheme = colors,
        shapes = shapes,
        typography = SimpleTypography,
        content = content
    )
}

object AuraFonts {
    val Brand: FontFamily get() = FontFamily.SansSerif
}
