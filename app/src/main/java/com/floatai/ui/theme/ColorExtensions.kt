package com.floatai.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 计算合成颜色的相对亮度，用于判断深浅主题。
 * 仅在 Composition 局部（如 backdrop 渐变）使用；不替代 isSystemInDarkTheme。
 */
fun Color.relativeLuminance(): Float {
    return 0.2126f * red + 0.7152f * green + 0.0722f * blue
}
