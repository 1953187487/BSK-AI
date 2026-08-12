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
 *  v1.0.6 升级：增加多层折射 / 顶部高光 / 底部阴影边 / 内部浮雕
 */
data class GlassTokens(
    /** 主体背景：半透明 + 顶部稍亮 */
    val blurBackground: Color,
    /** 边框：1px 高光描边 */
    val blurBorder: Color,
    /** 顶部高光：1px 高光内描边（亮） */
    val highlight: Color,
    /** 折射色：左上 → 右下的渐变叠加层 */
    val refraction: Color,
    /** 底部阴影：底部 1px 暗边 */
    val shadowEdge: Color,
    /** 内部浮雕（可选） */
    val innerGlow: Color
)

val DarkGlass = GlassTokens(
    blurBackground = Color(0xAA1C1C2E),
    blurBorder = Color(0x55FFFFFF),
    highlight = Color(0xAAFFFFFF),
    refraction = Color(0x33FFFFFF),
    shadowEdge = Color(0x66000000),
    innerGlow = Color(0x22FFFFFF)
)

val LightGlass = GlassTokens(
    blurBackground = Color(0xCCFFFFFF),
    blurBorder = Color(0x33000000),
    highlight = Color(0xFFFFFFFF),
    refraction = Color(0x33FFFFFF),
    shadowEdge = Color(0x33000000),
    innerGlow = Color(0x22FFFFFF)
)

/**
 * 主屏渐变背景（液态光斑）。
 * v1.0.6 升级：5 个色块 + 柔和弥散光斑（visionOS 风格）。
 */
fun liquidBackdrop(darkTheme: Boolean): Brush {
    val base = if (darkTheme) {
        listOf(
            Color(0xFF0A0A18),
            Color(0xFF1A1A2E),
            Color(0xFF16213E),
            Color(0xFF0F3460),
            Color(0xFF000000)
        )
    } else {
        listOf(
            Color(0xFFE0E7FF),
            Color(0xFFFCE7F3),
            Color(0xFFDCFCE7),
            Color(0xFFFFE5C7),
            Color(0xFFF2F2F7)
        )
    }
    return Brush.verticalGradient(base)
}

/**
 * 背景光斑层：在主背景上叠加几个模糊圆形光斑（Compose 不能直接 blur，用大半透明 radial gradient 模拟）。
 */
fun liquidOrbs(darkTheme: Boolean): List<Brush> {
    return if (darkTheme) {
        listOf(
            // 左上 紫色光斑
            Brush.radialGradient(
                colors = listOf(Color(0x66BF5AF2), Color(0x00BF5AF2)),
                center = androidx.compose.ui.geometry.Offset(0.15f, 0.15f),
                radius = 600f
            ),
            // 右上 青色光斑
            Brush.radialGradient(
                colors = listOf(Color(0x5530D1B5), Color(0x0030D1B5)),
                center = androidx.compose.ui.geometry.Offset(0.9f, 0.25f),
                radius = 500f
            ),
            // 中下 粉色光斑
            Brush.radialGradient(
                colors = listOf(Color(0x44FF375F), Color(0x00FF375F)),
                center = androidx.compose.ui.geometry.Offset(0.5f, 0.85f),
                radius = 700f
            ),
            // 左下 蓝色光斑
            Brush.radialGradient(
                colors = listOf(Color(0x440A84FF), Color(0x000A84FF)),
                center = androidx.compose.ui.geometry.Offset(0.1f, 0.9f),
                radius = 550f
            )
        )
    } else {
        listOf(
            // 浅色版：更柔和高亮光斑
            Brush.radialGradient(
                colors = listOf(Color(0x99DDD6FF), Color(0x00DDD6FF)),
                center = androidx.compose.ui.geometry.Offset(0.15f, 0.15f),
                radius = 600f
            ),
            Brush.radialGradient(
                colors = listOf(Color(0x99FFD6E0), Color(0x00FFD6E0)),
                center = androidx.compose.ui.geometry.Offset(0.9f, 0.25f),
                radius = 500f
            ),
            Brush.radialGradient(
                colors = listOf(Color(0x99FFE9C7), Color(0x00FFE9C7)),
                center = androidx.compose.ui.geometry.Offset(0.5f, 0.85f),
                radius = 700f
            ),
            Brush.radialGradient(
                colors = listOf(Color(0x99C7E9FF), Color(0x00C7E9FF)),
                center = androidx.compose.ui.geometry.Offset(0.1f, 0.9f),
                radius = 550f
            )
        )
    }
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
