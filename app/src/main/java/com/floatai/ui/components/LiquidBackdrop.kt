package com.floatai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import com.floatai.ui.theme.isLiquidGlassDark
import com.floatai.ui.theme.liquidBackdrop
import com.floatai.ui.theme.liquidOrbs

/**
 * 液态玻璃背景 v1.0.6：
 *  - 主背景渐变（4 色 + 5 段纵向渐变）
 *  - 4 个半透明 radial gradient 光斑（visionOS 弥散光）
 *  - 用 Plus BlendMode 叠加（在 Compose 1.6+ 中通过 Background Modifier 实现）
 *
 * 用法：在屏幕最外层 Box 内 `.background(liquidBackdrop)` 上叠多个 Box `.background(liquidOrbs[i])`
 */
@Composable
fun LiquidBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val dark = isLiquidGlassDark()
    Box(modifier = modifier.fillMaxSize()) {
        // 主背景渐变
        Box(modifier = Modifier.fillMaxSize().background(liquidBackdrop(dark)))
        // 弥散光斑（4 个 radial gradient）
        liquidOrbs(dark).forEach { orb ->
            Box(modifier = Modifier.fillMaxSize().background(orb))
        }
        // 内容层
        Box(modifier = Modifier.fillMaxSize()) { content() }
    }
}
