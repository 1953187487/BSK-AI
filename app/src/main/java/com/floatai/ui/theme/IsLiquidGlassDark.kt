package com.floatai.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable

/**
 * UI 是否处于深色主题（用于选择玻璃色调色板）。
 */
@Composable
fun isLiquidGlassDark(): Boolean = isSystemInDarkTheme()
