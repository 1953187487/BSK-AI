package com.bskai.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import kotlin.math.sin

@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val isListening by viewModel.isListening.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val history by viewModel.conversationHistory.collectAsState()

    var expanded by remember { mutableStateOf(false) }

    val animate = remember {
        AnimationSpec
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))

        // Logo
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF6C5CE7).copy(alpha = 0.8f),
                            Color(0xFF00CED1).copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Mic,
                contentDescription = "AURA Logo",
                tint = Color.White,
                modifier = Modifier.size(60.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "AURA",
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 4.sp
            ),
            color = Color.White
        )

        Text(
            text = "智能语音助手",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFB8B6D0)
        )

        Spacer(Modifier.height(32.dp))

        // Quick actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(
                icon = Icons.Outlined.MusicNote,
                label = "音乐控制",
                onClick = { /* TODO */ }
            )
            QuickActionCard(
                icon = Icons.Outlined.Folder,
                label = "文件管理",
                onClick = { /* TODO */ }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(
                icon = Icons.Outlined.Settings,
                label = "系统设置",
                onClick = { /* TODO */ }
            )
            QuickActionCard(
                icon = Icons.Outlined.Call,
                label = "拨打电话",
                onClick = { /* TODO */ }
            )
        }

        Spacer(Modifier.height(32.dp))

        // Recent activity
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E3A))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "最近对话",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFB8B6D0)
                )
                Spacer(Modifier.height(8.dp))
                if (history.isEmpty()) {
                    Text(
                        text = "还没有对话记录，点击下方按钮开始对话",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B6B8D)
                    )
                } else {
                    history.takeLast(5).reversed().forEach { msg ->
                        Text(
                            text = "• $msg",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB8B6D0)
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Listening button
        Button(
            onClick = {
                if (isListening) viewModel.stopListening() else viewModel.startListening()
            },
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isListening) Color(0xFFFF4757) else Color(0xFF6C5CE7)
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
        ) {
            Icon(
                imageVector = if (isListening) Icons.Filled.MicOff else Icons.Filled.Mic,
                contentDescription = if (isListening) "停止监听" else "开始监听",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }

        Text(
            text = if (isListening) "正在监听..." else "点击开始对话",
            style = MaterialTheme.typography.labelMedium,
            color = if (isListening) Color(0xFFFF4757) else Color(0xFFB8B6D0),
            modifier = Modifier.padding(top = 8.dp)
        )

        if (isSpeaking) {
            Text(
                text = "AURA 正在说话...",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF00CED1),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E3A)),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color(0xFF6C5CE7),
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFB8B6D0),
                textAlign = TextAlign.Center
            )
        }
    }
}
