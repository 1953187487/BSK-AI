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
   Adapted for AURA 2.1.1: lens style + android.graphics.RuntimeShader factory for Compose 1.5.
 */

package com.kyant.backdrop.effects

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.internal.RoundedRectRefractionShaderString
import com.kyant.backdrop.internal.RoundedRectRefractionWithDispersionShaderString

/**
 * Lens refraction style, mirroring the upstream AndroidLiquidGlass lens effect.
 */
data class LensStyle(
    val refractionHeight: Dp = 24.dp,
    val refractionAmount: Float = 0.1f,
    val depthEffect: Float = 0.02f,
    val chromaticAberration: Float = 0f
) {

    companion object {

        val Default: LensStyle = LensStyle()
    }
}

/**
 * Creates a rounded-rect refraction [RuntimeShader] for the given [shape] on API 33+.
 * Returns null when runtime shaders are unavailable (API < 33) or the shape is unsupported.
 */
fun createRefractionShader(
    cornerRadiiPx: FloatArray,
    lens: LensStyle,
    sizeWidthPx: Float,
    sizeHeightPx: Float,
    density: Float
): RuntimeShader? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
    val shaderString =
        if (lens.chromaticAberration > 0f) {
            RoundedRectRefractionWithDispersionShaderString
        } else {
            RoundedRectRefractionShaderString
        }
    return try {
        @Suppress("NewApi")
        RuntimeShader(shaderString).apply {
            setFloatUniform("size", sizeWidthPx, sizeHeightPx)
            setFloatUniform("offset", 0f, 0f)
            setFloatUniform("cornerRadii", cornerRadiiPx[0], cornerRadiiPx[1], cornerRadiiPx[2], cornerRadiiPx[3])
            setFloatUniform("refractionHeight", lens.refractionHeight.value * density)
            setFloatUniform("refractionAmount", lens.refractionAmount)
            setFloatUniform("depthEffect", lens.depthEffect)
            if (lens.chromaticAberration > 0f) {
                setFloatUniform("chromaticAberration", lens.chromaticAberration)
            }
        }
    } catch (_: Throwable) {
        null
    }
}
