package com.bskai.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bskai.ui.viewmodel.MainViewModel

@Composable
fun VoiceScreen(viewModel: MainViewModel) {
    val isListening by viewModel.isListening.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val response by viewModel.currentResponse.collectAsState()
    val history by viewModel.conversationHistory.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))

        Text(
            text = "对 AURA 说话",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White
        )
        Text(
            text = if (isListening) "正在聆听..." else if (isSpeaking) "AURA 正在回应..." else "点击下方按钮开始",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFB8B6D0)
        )

        Spacer(Modifier.height(40.dp))

        // Wave visualization
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = if (isListening) listOf(
                            Color(0xFF6C5CE7).copy(alpha = 0.6f),
                            Color(0xFF00CED1).copy(alpha = 0.3f),
                            Color.Transparent
                        ) else listOf(
                            Color(0xFF1E1E3A),
                            Color(0xFF16162A)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isListening) {
                for (i in 0..2) {
                    val scale = 1f + kotlin.math.sin((waveOffset + i * 120f) * Math.PI / 180f) * 0.15f
                    Box(
                        modifier = Modifier
                            .size((120 * scale).dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF6C5CE7).copy(alpha = 0.3f - i * 0.08f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }
            }
            Icon(
                imageVector = if (isListening) Icons.Filled.Mic else Icons.Outlined.Mic,
                contentDescription = null,
                tint = if (isListening) Color(0xFF6C5CE7) else Color(0xFF6B6B8D),
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(Modifier.height(40.dp))

        // Response display
        if (response.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E3A))
            ) {
                Text(
                    text = response,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFE0DFFF),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        // History
        if (history.isNotEmpty()) {
            Text(
                text = "历史记录",
                style = MaterialTheme.typography.titleSmall,
                color = Color(0xFFB8B6D0),
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(history) { msg ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E3A).copy(alpha = 0.8f))
                    ) {
                        Text(
                            text = msg,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB8B6D0)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Main button
        Button(
            onClick = {
                if (isListening) viewModel.stopListening() else viewModel.startListening()
            },
            modifier = Modifier
                .height(56.dp)
                .fillMaxWidth(0.6f)
                .clip(CircleShape),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isListening) Color(0xFFFF4757) else Color(0xFF6C5CE7)
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
        ) {
            Icon(
                imageVector = if (isListening) Icons.Filled.MicOff else Icons.Filled.Mic,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (isListening) "停止" else "按住说话",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        }
    }
}
