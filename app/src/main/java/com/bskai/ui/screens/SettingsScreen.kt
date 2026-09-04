package com.bskai.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.bskai.settings.AppSettings
import com.bskai.settings.SettingsStore
import com.bskai.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val settings by viewModel.settingsStore.settings.collectAsState()
    var showVoiceCloning by remember { mutableStateOf(false) }
    var showApiConfig by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "设置",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF16162A).copy(alpha = 0.95f)
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "v2.0.0-beta.2",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF6B6B8D)
                )
            }

            item {
                SettingsSection(title = "服务", icon = Icons.Outlined.Storage) {
                    SettingsToggleRow(
                        icon = Icons.Outlined.PowerSettingsNew,
                        title = "开机自启",
                        subtitle = "系统重启后自动启动监听服务",
                        checked = settings.autoStartService,
                        onCheckedChange = {}
                    )
                    SettingsToggleRow(
                        icon = Icons.Outlined.NotificationImportant,
                        title = "后台常驻",
                        subtitle = "保持AURA在后台持续监听语音指令",
                        checked = true,
                        onCheckedChange = {}
                    )
                    SettingsInfoRow(
                        icon = Icons.Outlined.Headset,
                        title = "音量键唤醒",
                        subtitle = "长按音量键可快速唤醒语音交互（需系统权限）",
                        value = "已启用"
                    )
                }
            }

            item {
                SettingsSection(title = "语音", icon = Icons.Outlined.Mic) {
                    SettingsToggleRow(
                        icon = Icons.Outlined.RecordVoiceOver,
                        title = "文字转语音",
                        subtitle = "AURA回应时使用语音朗读",
                        checked = settings.ttsEnabled,
                        onCheckedChange = {}
                    )
                    SettingsSliderRow(
                        icon = Icons.Outlined.Speed,
                        title = "语速",
                        subtitle = "调整语音朗读速度",
                        value = settings.ttsSpeed,
                        onValueChange = {}
                    )
                    SettingsSliderRow(
                        icon = Icons.Outlined.Tune,
                        title = "音调",
                        subtitle = "调整语音语调",
                        value = settings.ttsPitch,
                        onValueChange = {}
                    )
                    SettingsInfoRow(
                        icon = Icons.Outlined.RecordVoiceOver,
                        title = "声音克隆",
                        subtitle = "录制并克隆您的声音，让AURA用您的声音回答",
                        value = if (settings.ttsLanguage != "zh") "已配置" else "未配置",
                        onClick = { showVoiceCloning = true }
                    )
                }
            }

            item {
                SettingsSection(title = "AI 服务", icon = Icons.Outlined.Cloud) {
                    SettingsInfoRow(
                        icon = Icons.Outlined.Api,
                        title = "自定义API服务商",
                        subtitle = "配置OpenAI兼容格式的API端点和密钥",
                        value = if (settings.apiProviderUrl.isNotEmpty()) "已配置" else "未配置",
                        onClick = { showApiConfig = true }
                    )
                    if (settings.apiModel.isNotEmpty()) {
                        SettingsInfoRow(
                            icon = Icons.Outlined.Storage,
                            title = "当前模型",
                            subtitle = settings.apiModel,
                            value = ""
                        )
                    }
                }
            }

            item {
                SettingsSection(title = "外观", icon = Icons.Outlined.Palette) {
                    SettingsToggleRow(
                        icon = Icons.Outlined.Waves,
                        title = "波形动画",
                        subtitle = "语音监听时显示可视化波形",
                        checked = settings.showWaveAnimation,
                        onCheckedChange = {}
                    )
                    SettingsToggleRow(
                        icon = Icons.Outlined.Vibration,
                        title = "振动反馈",
                        subtitle = "收到回复时振动提醒",
                        checked = settings.vibrateOnResponse,
                        onCheckedChange = {}
                    )
                }
            }

            item {
                SettingsSection(title = "权限管理", icon = Icons.Outlined.Shield) {
                    val context = LocalContext.current
                    PermissionStatusRow(
                        icon = Icons.Outlined.Mic,
                        title = "录音权限",
                        subtitle = "用于语音识别",
                        permission = Manifest.permission.RECORD_AUDIO,
                        context = context,
                        onGrant = { viewModel.requestPermissions() }
                    )
                    PermissionStatusRow(
                        icon = Icons.Outlined.Notifications,
                        title = "通知权限",
                        subtitle = "用于显示监听状态通知",
                        permission = Manifest.permission.POST_NOTIFICATIONS,
                        context = context,
                        onGrant = { viewModel.requestPermissions() }
                    )
                    PermissionStatusRow(
                        icon = Icons.Outlined.Storage,
                        title = "存储权限",
                        subtitle = "用于文件管理功能",
                        permission = if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.P)
                            Manifest.permission.READ_EXTERNAL_STORAGE else Manifest.permission.MANAGE_EXTERNAL_STORAGE,
                        context = context,
                        onGrant = { viewModel.requestPermissions() }
                    )
                }
            }

            item {
                SettingsSection(title = "关于", icon = Icons.Outlined.Info) {
                    SettingsInfoRow(
                        icon = Icons.Outlined.Info,
                        title = "版本",
                        value = "2.0.0-beta.2"
                    )
                    SettingsInfoRow(
                        icon = Icons.Outlined.Description,
                        title = "开源协议",
                        subtitle = "MIT + Apache 2.0",
                        value = ""
                    )
                    SettingsInfoRow(
                        icon = Icons.Outlined.Share,
                        title = "分享给好友",
                        value = ""
                    )
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (showApiConfig) {
        ApiConfigDialog(
            currentUrl = settings.apiProviderUrl,
            currentKey = settings.apiProviderKey,
            currentModel = settings.apiModel,
            onDismiss = { showApiConfig = false },
            onSave = { url, key, model ->
                viewModel.settingsStore.update {
                    it.copy(apiProviderUrl = url, apiProviderKey = key, apiModel = model)
                }
                showApiConfig = false
            }
        )
    }

    if (showVoiceCloning) {
        VoiceCloningDialog(
            onDismiss = { showVoiceCloning = false },
            onClone = { voiceId ->
                viewModel.settingsStore.update { it.copy(ttsLanguage = voiceId) }
                showVoiceCloning = false
            }
        )
    }
}

@Composable
private fun SettingsSection(title: String, icon: ImageVector, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF6C5CE7), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFF6C5CE7)
            )
        }
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
        Icon(icon, contentDescription = null, tint = Color(0xFF6C5CE7), modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
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
            Icon(icon, contentDescription = null, tint = Color(0xFF6C5CE7), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
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
private fun SettingsInfoRow(
    icon: ImageVector,
    title: String,
    subtitle: String = "",
    value: String = "",
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFF6C5CE7), modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = Color.White)
            if (subtitle.isNotEmpty()) {
                Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = Color(0xFF6B6B8D))
            }
        }
        if (value.isNotEmpty()) {
            Text(text = value, style = MaterialTheme.typography.labelMedium, color = Color(0xFF6B6B8D))
        }
        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = Color(0xFF6B6B8D), modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun PermissionStatusRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    permission: String,
    context: android.content.Context,
    onGrant: () -> Unit
) {
    val granted = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clickable { if (!granted.value) onGrant() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = if (granted.value) Color(0xFF34D399) else Color(0xFFFF6B9D), modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = Color.White)
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = Color(0xFF6B6B8D))
        }
        Text(
            text = if (granted.value) "已授权" else "未授权",
            style = MaterialTheme.typography.labelMedium,
            color = if (granted.value) Color(0xFF34D399) else Color(0xFFFF6B9D)
        )
    }
}

@Composable
private fun ApiConfigDialog(
    currentUrl: String,
    currentKey: String,
    currentModel: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var url by remember { mutableStateOf(currentUrl) }
    var key by remember { mutableStateOf(currentKey) }
    var model by remember { mutableStateOf(currentModel) }
    var showKey by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E3A),
        title = {
            Text("配置API服务商", color = Color.White, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("API地址", color = Color(0xFF6C5CE7)) },
                    placeholder = { Text("https://api.openai.com/v1", color = Color(0xFF6B6B8D)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6C5CE7),
                        unfocusedBorderColor = Color(0xFF3D3C68),
                        focusedContainerColor = Color(0xFF16162A),
                        unfocusedContainerColor = Color(0xFF16162A)
                    )
                )
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("API密钥", color = Color(0xFF6C5CE7)) },
                    placeholder = { Text("sk-xxxxxxxxxxxxxxxx") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (showKey) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(onClick = { showKey = !showKey }) {
                            Text(if (showKey) "隐藏" else "显示", color = Color(0xFF6C5CE7))
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6C5CE7),
                        unfocusedBorderColor = Color(0xFF3D3C68),
                        focusedContainerColor = Color(0xFF16162A),
                        unfocusedContainerColor = Color(0xFF16162A)
                    )
                )
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("模型名称", color = Color(0xFF6C5CE7)) },
                    placeholder = { Text("gpt-4o-mini 或 deepseek-chat") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6C5CE7),
                        unfocusedBorderColor = Color(0xFF3D3C68),
                        focusedContainerColor = Color(0xFF16162A),
                        unfocusedContainerColor = Color(0xFF16162A)
                    )
                )
                Text(
                    text = "支持OpenAI兼容格式的API（如DeepSeek、ChatGLM、通义千问等）",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF6B6B8D)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(url.trim(), key.trim(), model.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C5CE7))
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = Color(0xFF6B6B8D))
            }
        }
    )
}

@Composable
private fun VoiceCloningDialog(
    onDismiss: () -> Unit,
    onClone: (String) -> Unit
) {
    var cloningStep by remember { mutableStateOf(0) }
    var recordingTime by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E3A),
        title = {
            Text("声音克隆", color = Color.White, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold))
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (cloningStep) {
                    0 -> {
                        Icon(Icons.Outlined.RecordVoiceOver, contentDescription = null, tint = Color(0xFF6C5CE7), modifier = Modifier.size(64.dp))
                        Text("录制您的声音样本", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Text(
                            "请找一个安静的环境，清晰朗读以下文字：\n\"你好AURA，我是你的主人，请帮我打开手电筒。\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFB8B6D0),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                    1 -> {
                        Text(
                            text = String.format("%02d:%02d", recordingTime / 60, recordingTime % 60),
                            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = Color(0xFF6C5CE7)
                        )
                        Text("正在录制...请开始朗读", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFB8B6D0))
                    }
                    2 -> {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(64.dp))
                        Text("声音克隆完成！", style = MaterialTheme.typography.titleMedium, color = Color(0xFF34D399))
                        Text("AURA将使用您的声音进行回答", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFB8B6D0))
                    }
                }
            }
        },
        confirmButton = {
            when (cloningStep) {
                0 -> Button(onClick = { cloningStep = 1 }) { Text("开始录制") }
                1 -> Button(
                    onClick = {
                        scope.launch {
                            while (recordingTime < 10) {
                                delay(1000L)
                                recordingTime++
                            }
                            cloningStep = 2
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4757))
                ) { Text("停止") }
                2 -> Button(onClick = { onClone("cloned_voice_001") }) { Text("完成") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = Color(0xFF6B6B8D))
            }
        }
    )
}
