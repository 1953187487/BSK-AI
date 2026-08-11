package com.floatai

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.floatai.ui.theme.FloatAITheme
import com.floatai.ui.theme.accentColorByName
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
            delay(1200)
            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            finish()
        }
    }
}

@Composable
fun LiquidGlassSplash() {
    val transition = rememberInfiniteTransition(label = "splash")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
        ),
        label = "phase"
    )

    val primary = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        primary,
                        primary.copy(alpha = 0.6f),
                        MaterialTheme.colorScheme.background
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color.White.copy(alpha = 0.35f), Color.Transparent),
                    center = Offset(size.width * 0.3f, size.height * 0.35f),
                    radius = size.width * (0.35f + phase * 0.1f)
                ),
                radius = size.width * (0.35f + phase * 0.1f),
                center = Offset(size.width * 0.3f, size.height * 0.35f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color.White.copy(alpha = 0.28f), Color.Transparent),
                    center = Offset(size.width * 0.7f, size.height * 0.6f),
                    radius = size.width * (0.3f + phase * 0.08f)
                ),
                radius = size.width * (0.3f + phase * 0.08f),
                center = Offset(size.width * 0.7f, size.height * 0.6f)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "FloatAI",
                fontSize = 52.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.95f)
            )
            Text(
                text = "AI 悬浮助手",
                fontSize = 18.sp,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 12.dp)
            )
            Text(
                text = "v1.0.0",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 32.dp)
            )
        }
    }
}
