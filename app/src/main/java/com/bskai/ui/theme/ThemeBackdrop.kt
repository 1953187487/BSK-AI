package com.bskai.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.bskai.data.ThemeStyle

private fun backdropColors(style: ThemeStyle, dark: Boolean): List<Color> = when (style) {
    ThemeStyle.AURORA -> if (dark) listOf(
        Color(0xFF0D0D1A), Color(0xFF1A1340), Color(0xFF14142A)
    ) else listOf(
        Color(0xFFF1EEFF), Color(0xFFE8E1FF), Color(0xFFFFF1F8)
    )
    ThemeStyle.NEON -> listOf(
        Color(0xFF060014), Color(0xFF140032), Color(0xFF060014)
    )
    ThemeStyle.GLASS -> if (dark) listOf(
        Color(0xFF08060F), Color(0xFF18113A), Color(0xFF08060F)
    ) else listOf(
        Color(0xFFEFEEF8), Color(0xFFE2DDFF), Color(0xFFF6F2FF)
    )
    ThemeStyle.LIQUID -> if (dark) listOf(
        Color(0xFF0A0F1A), Color(0xFF0F1A2E), Color(0xFF0A1628)
    ) else listOf(
        Color(0xFFF0F7FF), Color(0xFFE6F3FF), Color(0xFFF5F9FF)
    )
}

@Composable
fun ThemeBackdrop(
    style: ThemeStyle,
    dark: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(backdropColors(style, dark)))
    )
}
