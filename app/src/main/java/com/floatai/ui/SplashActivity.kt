package com.floatai.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GlassTheme {
                LiquidGlassSplash()
            }
        }
        startActivity(Intent(this, MainActivity::class.java))
        finish()
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF5B2A86), Color(0xFF3E1F6E), Color(0xFF1B0E3A))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width * 0.3f
            val cy = size.height * 0.35f
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0x55FFFFFF), Color(0x00FFFFFF)),
                    center = Offset(cx, cy),
                    radius = size.width * (0.35f + phase * 0.1f)
                ),
                radius = size.width * (0.35f + phase * 0.1f),
                center = Offset(cx, cy)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0x4490CDFF), Color(0x004ECDC4)),
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
                text = "液态玻璃 AI 悬浮助手",
                fontSize = 18.sp,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 12.dp)
            )
            Text(
                text = "v0.1",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 32.dp)
            )
        }
    }
}
