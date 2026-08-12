package com.floatai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.floatai.ui.theme.DarkGlass
import com.floatai.ui.theme.LightGlass
import com.floatai.ui.theme.isLiquidGlassDark

/**
 * 液态玻璃卡片 v1.0.6 高级版：
 *  - 三层背景：垂直渐变主体 + 左上→右下折射光 + 顶部高光
 *  - 1px 高光内描边（顶部稍亮，底部稍暗）
 *  - 1px 底部阴影描边
 *  - 圆角默认 24dp
 *  - 全部渐变用半透明叠加，呈现真实"玻璃 + 折射"质感
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 24,
    content: @Composable () -> Unit
) {
    val glass = if (isLiquidGlassDark()) DarkGlass else LightGlass
    val shape = RoundedCornerShape(cornerRadius.dp)
    val rPx = cornerRadius.dp.value * 3f  // 折射光的圆角略小
    Box(
        modifier = modifier
            .clip(shape)
            // 第 1 层：垂直渐变主体（顶部稍亮，底部稍暗）
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        lerp(glass.blurBackground, Color.White, if (isLiquidGlassDark()) 0.10f else 0.30f),
                        glass.blurBackground,
                        lerp(glass.blurBackground, Color.Black, if (isLiquidGlassDark()) 0.20f else 0.06f)
                    )
                ),
                shape = shape
            )
            // 第 2 层：左上→右下折射光（叠加混合）
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        glass.refraction,
                        Color.Transparent,
                        Color.Transparent,
                        glass.refraction.copy(alpha = glass.refraction.alpha * 0.4f)
                    ),
                    start = Offset.Zero,
                    end = Offset.Infinite
                ),
                shape = shape
            )
            // 第 3 层：顶部高光（1px）
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        glass.highlight,
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = 80f
                ),
                shape = shape
            )
            // 第 4 层：外描边（顶部亮、底部暗）
            .border(
                width = 0.7.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        glass.blurBorder,
                        glass.shadowEdge
                    )
                ),
                shape = shape
            )
            // 内容层
            .padding(20.dp)
    ) {
        content()
    }
}

/**
 * 液态玻璃：按钮专用（按下态更明显 + 边光强化）
 */
@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: Int = 18,
    content: @Composable () -> Unit
) {
    val glass = if (isLiquidGlassDark()) DarkGlass else LightGlass
    val shape = RoundedCornerShape(cornerRadius.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        lerp(glass.blurBackground, Color.White, 0.18f),
                        glass.blurBackground
                    )
                ),
                shape = shape
            )
            .background(
                Brush.verticalGradient(
                    colors = listOf(glass.highlight, Color.Transparent),
                    startY = 0f,
                    endY = 60f
                ),
                shape = shape
            )
            .border(
                width = 0.6.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(glass.blurBorder, glass.shadowEdge)
                ),
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * 设置项行：左侧标题/副标题，右侧可自定义内容。
 */
@Composable
fun SettingsRow(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickableSafe(onClick)
            .padding(vertical = 14.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        modifier = modifier.padding(top = 24.dp, bottom = 10.dp)
    )
}

private fun Modifier.clickableSafe(onClick: (() -> Unit)?): Modifier =
    if (onClick != null) this.then(Modifier.clickable(onClick = onClick)) else this
