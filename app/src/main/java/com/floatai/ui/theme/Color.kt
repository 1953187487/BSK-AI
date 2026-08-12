package com.floatai.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// iOS 17 / visionOS 风格：低饱和、高对比、玻璃感
val BrandIndigo = Color(0xFF5E5CE6)
val BrandPink = Color(0xFFFF375F)
val BrandTeal = Color(0xFF30D1B5)
val BrandOrange = Color(0xFFFF9F0A)
val BrandPurple = Color(0xFFBF5AF2)
val BrandBlue = Color(0xFF0A84FF)

// 深色背景层级（接近 visionOS）
val DarkBackground = Color(0xFF000000)
val DarkSurface = Color(0xCC1C1C1E)
val DarkSurfaceHigh = Color(0xFF2C2C2E)
val DarkSurfaceVariant = Color(0xFF3A3A3C)

// 亮色背景层级
val LightBackground = Color(0xFFF2F2F7)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE5E5EA)

/**
 * 玻璃态颜色组合：根据深浅模式提供模糊层。
 * v1.0.6-rc.2：恢复简洁的双层结构（v1.0.5 风格），去掉过度装饰。
 */
data class GlassTokens(
    val blurBackground: Color,
    val blurBorder: Color,
    val highlight: Color
)

val DarkGlass = GlassTokens(
    blurBackground = Color(0x661C1C1E),
    blurBorder = Color(0x33FFFFFF),
    highlight = Color(0x33FFFFFF)
)

val LightGlass = GlassTokens(
    blurBackground = Color(0x99FFFFFF),
    blurBorder = Color(0x1A000000),
    highlight = Color(0x66FFFFFF)
)

/**
 * 主屏渐变背景：v1.0.6-rc.2 恢复单层渐变（v1.0.5 风格）
 */
fun liquidBackdrop(darkTheme: Boolean): Brush {
    val base = if (darkTheme) {
        listOf(
            Color(0xFF1A1A2E),
            Color(0xFF0F3460),
            Color(0xFF000000)
        )
    } else {
        listOf(
            Color(0xFFE0E7FF),
            Color(0xFFFCE7F3),
            Color(0xFFF2F2F7)
        )
    }
    return Brush.verticalGradient(base)
}

/**
 * 可选主题色列表：名称 -> 种子色。用于设置页选择主题色。
 */
data class AccentOption(val name: String, val color: Color)

val AccentOptions = listOf(
    AccentOption("靛蓝", BrandIndigo),
    AccentOption("粉色", BrandPink),
    AccentOption("青色", BrandTeal),
    AccentOption("橙色", BrandOrange),
    AccentOption("紫色", BrandPurple),
    AccentOption("蓝色", BrandBlue),
)

fun accentColorByName(name: String): Color =
    AccentOptions.firstOrNull { it.name == name }?.color ?: BrandIndigo

fun accentNameByColor(color: Color): String =
    AccentOptions.firstOrNull { it.color == color }?.name ?: "靛蓝"
