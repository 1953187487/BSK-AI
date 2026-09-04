package com.bskai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bskai.ui.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    var autoStart by remember { mutableStateOf(false) }
    var ttsEnabled by remember { mutableStateOf(true) }
    var showWave by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "设置",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = Color.White
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "v2.0.0-beta.1",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF6B6B8D)
            )
        }

        item {
            SettingsSection(title = "服务") {
                SettingsToggleRow(
                    icon = Icons.Outlined.PowerSettingsNew,
                    title = "开机自启",
                    subtitle = "系统重启后自动启动监听服务",
                    checked = autoStart,
                    onCheckedChange = { autoStart = it }
                )
                SettingsToggleRow(
                    icon = Icons.Outlined.NotificationImportant,
                    title = "后台常驻",
                    subtitle = "保持AURA在后台持续监听语音指令",
                    checked = true,
                    onCheckedChange = {}
                )
            }
        }

        item {
            SettingsSection(title = "语音") {
                SettingsToggleRow(
                    icon = Icons.Outlined.RecordVoiceOver,
                    title = "文字转语音",
                    subtitle = "AURA回应时使用语音朗读",
                    checked = ttsEnabled,
                    onCheckedChange = { ttsEnabled = it }
                )
                SettingsSliderRow(
                    icon = Icons.Outlined.Speed,
                    title = "语速",
                    subtitle = "调整语音朗读速度",
                    value = 1.0f,
                    onValueChange = {}
                )
                SettingsSliderRow(
                    icon = Icons.Outlined.Tune,
                    title = "音调",
                    subtitle = "调整语音语调",
                    value = 1.0f,
                    onValueChange = {}
                )
            }
        }

        item {
            SettingsSection(title = "外观") {
                SettingsToggleRow(
                    icon = Icons.Outlined.Waves,
                    title = "波形动画",
                    subtitle = "语音监听时显示可视化波形",
                    checked = showWave,
                    onCheckedChange = { showWave = it }
                )
            }
        }

        item {
            SettingsSection(title = "权限管理") {
                PermissionStatusRow(
                    icon = Icons.Outlined.Mic,
                    title = "录音权限",
                    permission = android.Manifest.permission.RECORD_AUDIO,
                    onGrant = { viewModel.requestPermissions() }
                )
                PermissionStatusRow(
                    icon = Icons.Outlined.Notifications,
                    title = "通知权限",
                    permission = android.Manifest.permission.POST_NOTIFICATIONS,
                    onGrant = { viewModel.requestPermissions() }
                )
                PermissionStatusRow(
                    icon = Icons.Outlined.Storage,
                    title = "存储权限",
                    permission = android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    onGrant = { viewModel.requestPermissions() }
                )
            }
        }

        item {
            SettingsSection(title = "关于") {
                SettingsInfoRow(
                    icon = Icons.Outlined.Info,
                    title = "版本",
                    value = "2.0.0-beta.1"
                )
                SettingsInfoRow(
                    icon = Icons.Outlined.Update,
                    title = "检查更新",
                    value = "已是最新",
                    onClick = {}
                )
                SettingsInfoRow(
                    icon = Icons.Outlined.Share,
                    title = "分享给好友",
                    value = "",
                    onClick = {}
                )
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = Color(0xFF6C5CE7),
            modifier = Modifier.padding(vertical = 4.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E3A))
        ) {
            content()
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF6C5CE7),
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = Color.White)
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = Color(0xFF6B6B8D))
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsSliderRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF6C5CE7), modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = Color.White)
                Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = Color(0xFF6B6B8D))
            }
            Text(text = "${(value * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = Color(0xFF6C5CE7))
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0.5f..2.0f,
            colors = SliderDefaults.colors(
                activeTrackColor = Color(0xFF6C5CE7),
                inactiveTrackColor = Color(0xFF1E1E3A),
                thumbColor = Color(0xFF6C5CE7)
            )
        )
    }
}

@Composable
private fun PermissionStatusRow(
    icon: ImageVector,
    title: String,
    permission: String,
    onGrant: () -> Unit
) {
    val granted = remember { mutableStateOf(androidx.core.content.ContextCompat.checkSelfPermission(androidx.compose.ui.platform.LocalContext.current, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clickable { if (!granted.value) onGrant() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = if (granted.value) Color(0xFF34D399) else Color(0xFFFF6B9D), modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = Color.White)
            Text(text = if (granted.value) "已授权" else "未授权，点击授予", style = MaterialTheme.typography.labelSmall, color = if (granted.value) Color(0xFF34D399) else Color(0xFFFF6B9D))
        }
        if (!granted.value) {
            TextButton(onClick = onGrant) { Text("授予", color = Color(0xFF6C5CE7)) }
        }
    }
}

@Composable
private fun SettingsInfoRow(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF6C5CE7), modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = Color.White)
        }
        if (value.isNotEmpty()) {
            Text(text = value, style = MaterialTheme.typography.labelMedium, color = Color(0xFF6B6B8D))
        }
    }
}
