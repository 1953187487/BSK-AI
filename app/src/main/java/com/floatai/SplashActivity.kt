package com.floatai

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.floatai.ui.i18n.localStrings
import com.floatai.ui.theme.FloatAITheme
import com.floatai.ui.theme.accentColorByName
import com.floatai.ui.theme.liquidBackdrop
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as App
        setContent {
            val settings = app.settingsRepository.settings
            FloatAITheme(
                darkTheme = settings.value.darkTheme,
                dynamicColor = settings.value.dynamicColor,
                accentColor = accentColorByName(settings.value.accentColor)
            ) {
                LiquidGlassSplash()
            }
        }
        lifecycleScope.launch {
            delay(1400)
            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            finish()
        }
    }
}

@Composable
fun LiquidGlassSplash() {
    val strings = localStrings()
    val primary = MaterialTheme.colorScheme.primary
    val dark = MaterialTheme.colorScheme.background == Color.Black

    val transition = rememberInfiniteTransition(label = "splash")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "phase"
    )
    val tilt by transition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tilt"
    )

    val scale = remember { Animatable(0.7f) }
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
        alpha.animateTo(1f, tween(700))
    }

    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(liquidBackdrop(dark)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(primary.copy(alpha = 0.55f), Color.Transparent),
                    center = Offset(size.width * (0.25f + phase * 0.1f), size.height * 0.3f),
                    radius = size.width * (0.55f + phase * 0.1f)
                ),
                radius = size.width * (0.55f + phase * 0.1f),
                center = Offset(size.width * (0.25f + phase * 0.1f), size.height * 0.3f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(tertiaryColor.copy(alpha = 0.45f), Color.Transparent),
                    center = Offset(size.width * (0.75f - phase * 0.05f), size.height * 0.7f),
                    radius = size.width * (0.45f + phase * 0.08f)
                ),
                radius = size.width * (0.45f + phase * 0.08f),
                center = Offset(size.width * (0.75f - phase * 0.05f), size.height * 0.7f)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer { scaleX = scale.value; scaleY = scale.value; rotationZ = tilt / 6f },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = strings.app_name,
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.96f),
                modifier = Modifier.graphicsLayer { this.alpha = alpha.value }
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = strings.settings_about_desc,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier
                    .padding(top = 6.dp)
                    .graphicsLayer { this.alpha = alpha.value }
            )
            Text(
                text = "v1.0.1",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.55f),
                modifier = Modifier
                    .padding(top = 28.dp)
                    .graphicsLayer { this.alpha = alpha.value }
            )
        }
    }
}
