@file:Suppress("NewApi")

/*
   Copyright 2025 Kyant

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

/*
   Vendored and adapted from https://github.com/Kyant0/AndroidLiquidGlass (Apache-2.0).
   Adapted for AURA 2.1.1 (Compose 1.5): applies the AGSL rounded-rect refraction
   (API 33+) and Gaussian blur (API 31+) to this node's own drawing through
   GraphicsLayerScope.renderEffect, via android.graphics.RenderEffect
   converted with asComposeRenderEffect().
 */

package com.kyant.backdrop

import android.graphics.Path
import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.effects.LensStyle
import com.kyant.backdrop.effects.createRefractionShader
import com.kyant.backdrop.internal.LiquidGlassHighlightShaderString

@Stable
private fun Shape.cornerRadiiPx(density: Density): FloatArray {
    val base = floatArrayOf(0f, 0f, 0f, 0f)
    (this as? CornerBasedShape)?.let { s ->
        base[0] = s.topStart.toPx(Size.Unspecified, density)
        base[1] = s.topEnd.toPx(Size.Unspecified, density)
        base[2] = s.bottomEnd.toPx(Size.Unspecified, density)
        base[3] = s.bottomStart.toPx(Size.Unspecified, density)
    }
    return base
}


private fun buildRenderEffect(
    width: Float,
    height: Float,
    cornerRadii: FloatArray,
    lens: LensStyle,
    blurPx: Float,
    densityValue: Float
): AndroidRenderEffect? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    return try {
        val blur = if (blurPx > 0f) {
            AndroidRenderEffect.createBlurEffect(blurPx, blurPx, Shader.TileMode.CLAMP)
        } else {
            null
        }
        val runtimeShadersOk = try {
            RuntimeShader("half4 main(float2 c) { return half4(0.0); }")
            true
        } catch (_: Throwable) {
            false
        }
        val refraction = if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            runtimeShadersOk &&
            lens.refractionAmount != 0f
        ) {
            createRefractionShader(
                cornerRadiiPx = cornerRadii,
                lens = lens,
                sizeWidthPx = width,
                sizeHeightPx = height,
                density = densityValue
            )?.let { shader ->
                AndroidRenderEffect.createRuntimeShaderEffect(shader, "content")
            }
        } else {
            null
        }
        when {
            refraction != null && blur != null -> AndroidRenderEffect.createChainEffect(refraction, blur)
            refraction != null -> refraction
            blur != null -> blur
            else -> null
        }
    } catch (_: Throwable) {
        null
    }
}

/**
 * Applies the AndroidLiquidGlass liquid-glass effect to this node's own drawing:
 * rounded-rect edge refraction with optional chromatic dispersion on API 33+,
 * and a Gaussian blur on API 31+. On older systems the content draws unchanged
 * and callers keep their gradient-glass fallback.
 *
 * The modifier must wrap the background-drawing layer only (e.g. a Box that draws
 * the glass gradient and contains no child content), so panel content stays sharp.
 */

fun Modifier.liquidGlassBackdrop(
    shape: Shape,
    lens: LensStyle = LensStyle.Default,
    blurRadius: Dp = 0.dp,
    clipToShape: Boolean = true
): Modifier = composed {
    val density = LocalDensity.current
    val densityValue = density.density
    val blurPx = with(density) { blurRadius.toPx() }
    val radii = shape.cornerRadiiPx(density)

    var androidEffect by remember { mutableStateOf<AndroidRenderEffect?>(null) }

    this
        .onSizeChanged { size ->
            androidEffect = buildRenderEffect(
                width = size.width.toFloat(),
                height = size.height.toFloat(),
                cornerRadii = radii,
                lens = lens,
                blurPx = blurPx,
                densityValue = densityValue
            )
        }
        .graphicsLayer {
            this.shape = shape
            this.clip = clipToShape
            androidEffect?.let { effect ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    try {
                        this.renderEffect = effect.asComposeRenderEffect()
                    } catch (_: Throwable) {
                    }
                }
            }
        }
}

/**
 * Draws a directional rim highlight derived from the rounded-rect signed distance
 * field using the upstream AGSL highlight shader (API 33+). Paint-level drawing,
 * so node content stays unaffected. A no-op on older systems.
 */
fun Modifier.liquidGlassRim(
    shape: Shape,
    color: Color,
    angleDegrees: Float = -45f,
    falloff: Float = 1.5f
): Modifier = composed {
    if (!isRuntimeShaderSupported()) return@composed this

    val density = LocalDensity.current
    val radii = shape.cornerRadiiPx(density)

    val shader: RuntimeShader? = remember {
        try {
            RuntimeShader(LiquidGlassHighlightShaderString)
        } catch (_: Throwable) {
            null
        }
    }
    val paint = remember {
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    }
    val path = remember { Path() }
    val colorArgb = remember(color) { color.toArgb() }
    val angleRadians = remember(angleDegrees) { angleDegrees * (Math.PI.toFloat() / 180f) }

    if (shader == null) return@composed this

    this.drawBehind {
        try {
            shader.setFloatUniform("size", size.width, size.height)
            @Suppress("NewApi")
            shader.setFloatUniform("cornerRadii", radii[0], radii[1], radii[2], radii[3])
            @Suppress("NewApi")
            shader.setFloatUniform("angle", angleRadians)
            @Suppress("NewApi")
            shader.setFloatUniform("falloff", falloff)
            @Suppress("NewApi")
            shader.setColorUniform("color", colorArgb)
            paint.shader = shader

            path.reset()
            path.addRoundRect(
                android.graphics.RectF(0f, 0f, size.width, size.height),
                floatArrayOf(radii[0], radii[0], radii[1], radii[1], radii[2], radii[2], radii[3], radii[3]),
                Path.Direction.CW
            )
            drawContext.canvas.nativeCanvas.drawPath(path, paint)
        } catch (_: Throwable) {
        }
    }
}
