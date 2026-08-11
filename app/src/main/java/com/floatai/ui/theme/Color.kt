package com.floatai.ui.theme

import androidx.compose.ui.graphics.Color

// 品牌基础色
val BrandPurple = Color(0xFF6C3BA8)
val BrandDeepPurple = Color(0xFF2A1B4D)
val BrandCyan = Color(0xFF4ECDC4)
val BrandPink = Color(0xFFFF6B6B)
val BrandBlue = Color(0xFF5B8DEF)
val BrandGreen = Color(0xFF43C59E)

// 深色背景层级
val DarkBackground = Color(0xFF0E0A1B)
val DarkSurface = Color(0xFF181226)
val DarkSurfaceHigh = Color(0xFF221A36)

// 亮色背景层级
val LightBackground = Color(0xFFF8F6FC)
val LightSurface = Color(0xFFFFFFFF)

/**
 * 可选主题色列表：名称 -> 种子色。用于设置页选择主题色。
 */
data class AccentOption(val name: String, val color: Color)

val AccentOptions = listOf(
    AccentOption("紫色", BrandPurple),
    AccentOption("粉色", BrandPink),
    AccentOption("青色", BrandCyan),
    AccentOption("蓝色", BrandBlue),
    AccentOption("绿色", BrandGreen),
    AccentOption("深紫", BrandDeepPurple),
)

fun accentColorByName(name: String): Color =
    AccentOptions.firstOrNull { it.name == name }?.color ?: BrandPurple

fun accentNameByColor(color: Color): String =
    AccentOptions.firstOrNull { it.color == color }?.name ?: "紫色"
