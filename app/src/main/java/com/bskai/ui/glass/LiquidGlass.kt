package com.bskai.ui.glass

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.annotation.StringRes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bskai.R
import com.kyant.backdrop.liquidGlassBackdrop
import com.kyant.backdrop.liquidGlassRim
import com.kyant.backdrop.effects.LensStyle

data class GlassColors(
    val fillTop: Color,
    val fillBottom: Color,
    val border: Color,
    val highlight: Color,
    val content: Color,
    val contentMuted: Color,
    val accent: Color,
    val onAccent: Color
)

@Composable
fun rememberGlassColors(accent: Color = MaterialTheme.colorScheme.primary): GlassColors {
    val dark = isSystemInDarkTheme()
    val onSurface = MaterialTheme.colorScheme.onSurface
    return remember(dark, accent, onSurface) {
        if (dark) GlassColors(
            fillTop = Color.White.copy(alpha = 0.13f),
            fillBottom = Color.White.copy(alpha = 0.05f),
            border = Color.White.copy(alpha = 0.20f),
            highlight = Color.White.copy(alpha = 0.28f),
            content = onSurface,
            contentMuted = onSurface.copy(alpha = 0.62f),
            accent = accent,
            onAccent = Color.White
        ) else GlassColors(
            fillTop = Color.White.copy(alpha = 0.82f),
            fillBottom = Color.White.copy(alpha = 0.48f),
            border = Color.Black.copy(alpha = 0.08f),
            highlight = Color.White.copy(alpha = 0.95f),
            content = onSurface,
            contentMuted = onSurface.copy(alpha = 0.55f),
            accent = accent,
            onAccent = Color.White
        )
    }
}

/**
 * Core glass surface visual: translucent gradient fill, AGSL rim highlight
 * (AndroidLiquidGlass highlight shader, API 33+) and gradient border.
 */
fun Modifier.glassSurface(
    shape: Shape,
    colors: GlassColors,
    borderWidth: Dp = 1.dp
): Modifier = this
    .clip(shape)
    .drawBehind {
        val h = size.height
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(colors.fillTop, colors.fillBottom),
                startY = 0f,
                endY = h
            )
        )
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(colors.highlight.copy(alpha = colors.highlight.alpha * 0.35f), Color.Transparent),
                startY = 0f,
                endY = h * 0.5f
            )
        )
    }
    .liquidGlassRim(
        shape = shape,
        color = colors.highlight.copy(alpha = colors.highlight.alpha * 0.45f),
        angleDegrees = -45f,
        falloff = 1.5f
    )
    .border(borderWidth, Brush.verticalGradient(listOf(colors.border, colors.border.copy(alpha = colors.border.alpha * 0.4f))), shape)

fun Modifier.liquidGlass(
    shape: Shape,
    colors: GlassColors,
    alpha: Float = 1f,
    borderWidth: Dp = 1.dp
): Modifier = this
    .glassSurface(shape, colors, borderWidth)
    .graphicsLayer(alpha = alpha)

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    colors: GlassColors = rememberGlassColors(),
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .liquidGlassBackdrop(
                    shape = shape,
                    lens = LensStyle(
                        refractionHeight = 24.dp,
                        refractionAmount = 0.08f,
                        depthEffect = 0.02f,
                        chromaticAberration = 0.3f
                    )
                )
                .glassSurface(shape, colors)
        )
        content()
    }
}

enum class GlassNavItem(@StringRes val labelRes: Int, val icon: ImageVector) {
    CHAT(R.string.nav_chat, Icons.Outlined.Forum),
    SETTINGS(R.string.nav_settings, Icons.Outlined.Settings)
}

@Composable
fun GlassTopNav(
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    items: List<GlassNavItem> = GlassNavItem.entries.toList()
) {
    val colors = rememberGlassColors()
    Row(
        modifier = modifier
            .liquidGlass(RoundedCornerShape(22.dp), colors)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items.forEachIndexed { index, item ->
            val isSelected = selected == index
            val bgAlpha by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "glassNavBg"
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(colors.accent.copy(alpha = 0.90f * bgAlpha), colors.accent.copy(alpha = 0.65f * bgAlpha))
                        )
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSelect(index) }
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = stringResource(item.labelRes),
                    tint = if (isSelected) colors.onAccent else colors.contentMuted,
                    modifier = Modifier.size(17.dp)
                )
                Text(
                    text = stringResource(item.labelRes),
                    modifier = Modifier.padding(start = 7.dp),
                    color = if (isSelected) colors.onAccent else colors.contentMuted,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun GlassBottomNav(
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    items: List<GlassNavItem> = GlassNavItem.entries.toList()
) {
    val colors = rememberGlassColors()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(RoundedCornerShape(26.dp), colors)
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        items.forEachIndexed { index, item ->
            val isSelected = selected == index
            val bgAlpha by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "glassBottomNavBg"
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(21.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(colors.accent.copy(alpha = 0.90f * bgAlpha), colors.accent.copy(alpha = 0.65f * bgAlpha))
                        )
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSelect(index) }
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = stringResource(item.labelRes),
                    tint = if (isSelected) colors.onAccent else colors.contentMuted,
                    modifier = Modifier.size(19.dp)
                )
                Text(
                    text = stringResource(item.labelRes),
                    modifier = Modifier.padding(start = 8.dp),
                    color = if (isSelected) colors.onAccent else colors.contentMuted,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun GlassSegmented(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = rememberGlassColors()
    Row(
        modifier = modifier
            .liquidGlass(RoundedCornerShape(16.dp), colors)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        options.forEachIndexed { index, option ->
            val isSelected = selected == index
            val bgAlpha by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "glassSegBg"
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(13.dp))
                    .background(
                        if (isSelected) colors.accent.copy(alpha = 0.85f)
                        else Color.Transparent
                    )
                    .graphicsLayer(alpha = 0.55f + 0.45f * bgAlpha)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSelect(index) }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    text = option,
                    color = if (isSelected) colors.onAccent else colors.contentMuted,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun GlassChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Color? = null
) {
    val colors = rememberGlassColors(accent ?: MaterialTheme.colorScheme.primary)
    val contentColor by animateColorAsState(
        targetValue = when {
            !enabled -> colors.contentMuted.copy(alpha = 0.4f)
            selected -> colors.onAccent
            else -> colors.contentMuted
        },
        label = "glassChipContent"
    )
    Box(
        modifier = modifier
            .liquidGlass(
                RoundedCornerShape(12.dp),
                colors,
                alpha = if (enabled) 1f else 0.45f
            )
            .then(
                if (selected) Modifier.background(
                    Brush.verticalGradient(
                        listOf(colors.accent.copy(alpha = 0.85f), colors.accent.copy(alpha = 0.6f))
                    ),
                    RoundedCornerShape(12.dp)
                ) else Modifier
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = text,
            color = contentColor,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    size: Dp = 38.dp
) {
    val colors = rememberGlassColors()
    Box(
        modifier = modifier
            .size(size)
            .liquidGlass(CircleShape, colors)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint ?: colors.content,
            modifier = Modifier.size(size * 0.5f)
        )
    }
}

@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Color? = null
) {
    val colors = rememberGlassColors(accent ?: MaterialTheme.colorScheme.primary)
    val btnAlpha by animateFloatAsState(if (enabled) 1f else 0.45f, label = "glassBtnAlpha")
    Box(
        modifier = modifier
            .graphicsLayer(alpha = btnAlpha)
            .liquidGlass(RoundedCornerShape(16.dp), colors)
            .background(
                Brush.verticalGradient(
                    listOf(colors.accent.copy(alpha = 0.9f), colors.accent.copy(alpha = 0.65f))
                ),
                RoundedCornerShape(16.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = colors.onAccent,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    textStyle: TextStyle = TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface),
    placeholderStyle: TextStyle = TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)),
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    maxLines: Int = 1,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    val colors = rememberGlassColors()
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        textStyle = textStyle,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        maxLines = maxLines,
        visualTransformation = visualTransformation,
        decorationBox = { inner ->
            Row(
                modifier = Modifier
                    .liquidGlass(RoundedCornerShape(22.dp), colors)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(placeholder, style = placeholderStyle)
                }
                inner()
            }
        }
    )
}

@Composable
fun GlassBubble(
    accent: Boolean,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.CenterStart,
    content: @Composable () -> Unit
) {
    val colors = rememberGlassColors()
    Box(
        modifier = modifier
            .liquidGlass(RoundedCornerShape(22.dp), colors)
            .then(
                if (accent) Modifier.background(
                    Brush.verticalGradient(
                        listOf(colors.accent.copy(alpha = 0.92f), colors.accent.copy(alpha = 0.68f))
                    ),
                    RoundedCornerShape(22.dp)
                ) else Modifier
            ),
        contentAlignment = contentAlignment
    ) {
        content()
    }
}

@Composable
fun GlassCardRow(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    val colors = rememberGlassColors()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(RoundedCornerShape(18.dp), colors)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}
