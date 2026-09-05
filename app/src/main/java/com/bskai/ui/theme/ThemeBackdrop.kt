package com.bskai.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.bskai.data.ThemeStyle
import kotlin.math.cos
import kotlin.math.sin

data class ThemeBackdropSpec(
    val baseGradient: List<Color>,
    val blobColors: List<Color>,
    val blobSizeDp: Int = 260,
    val blobAlpha: Float = 0.55f,
    val animate: Boolean = true
)

private fun backdropFor(style: ThemeStyle, dark: Boolean): ThemeBackdropSpec = when (style) {
    ThemeStyle.AURORA -> ThemeBackdropSpec(
        baseGradient = if (dark) listOf(
            Color(0xFF0D0D1A), Color(0xFF1A1340), Color(0xFF14142A)
        ) else listOf(
            Color(0xFFF1EEFF), Color(0xFFE8E1FF), Color(0xFFFFF1F8)
        ),
        blobColors = listOf(
            Color(0xFF8E7CFF), Color(0xFF00CED1), Color(0xFFFF6B9D)
        ),
        blobSizeDp = 320,
        blobAlpha = if (dark) 0.55f else 0.32f
    )
    ThemeStyle.NEON -> ThemeBackdropSpec(
        baseGradient = listOf(
            Color(0xFF060014), Color(0xFF140032), Color(0xFF060014)
        ),
        blobColors = listOf(
            Color(0xFFFF2D95), Color(0xFF00F0FF), Color(0xFFE6FF00)
        ),
        blobSizeDp = 280,
        blobAlpha = 0.45f
    )
    ThemeStyle.GLASS -> ThemeBackdropSpec(
        baseGradient = if (dark) listOf(
            Color(0xFF08060F), Color(0xFF18113A), Color(0xFF08060F)
        ) else listOf(
            Color(0xFFEFEEF8), Color(0xFFE2DDFF), Color(0xFFF6F2FF)
        ),
        blobColors = listOf(
            Color(0xFF7B6FE6), Color(0xFF80E1FF), Color(0xFFFFB8DD)
        ),
        blobSizeDp = 360,
        blobAlpha = if (dark) 0.40f else 0.28f
    )
    ThemeStyle.VOICE -> ThemeBackdropSpec(
        baseGradient = listOf(
            Color(0xFF0A0A12), Color(0xFF1A0A14), Color(0xFF0A0A12)
        ),
        blobColors = listOf(
            Color(0xFFFF5252), Color(0xFFFFD740), Color(0xFF80CBC4)
        ),
        blobSizeDp = 340,
        blobAlpha = 0.32f
    )
}

@Composable
fun ThemeBackdrop(
    style: ThemeStyle,
    dark: Boolean,
    modifier: Modifier = Modifier
) {
    val spec = backdropFor(style, dark)
    val transition = rememberInfiniteTransition(label = "backdrop")
    val phase by (if (spec.animate) transition else null)?.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "phase"
    ) ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(spec.baseGradient))
    ) {
        spec.blobColors.forEachIndexed { i, color ->
            val angle = (phase + i / spec.blobColors.size.toFloat()) * 2f * Math.PI.toFloat()
            val radius = 0.35f
            val cx = 0.5f + radius * cos(angle)
            val cy = 0.5f + radius * sin(angle * 1.3f) * 0.6f
            val offsetX = ((cx - 0.5f) * 1000).dp
            val offsetY = ((cy - 0.5f) * 1400).dp
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = offsetX.value.toInt().coerceIn(-600, 600),
                            y = offsetY.value.toInt().coerceIn(-800, 800)
                        )
                    }
                    .size(spec.blobSizeDp.dp)
                    .clip(CircleShape)
                    .blur(80.dp)
                    .background(color.copy(alpha = spec.blobAlpha))
            )
        }
    }
}
