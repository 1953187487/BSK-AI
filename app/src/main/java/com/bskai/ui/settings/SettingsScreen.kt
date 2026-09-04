package com.bskai.ui.settings

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bskai.AuraApp
import com.bskai.BuildConfig
import com.bskai.util.Permissions

@Composable
fun SettingsScreen(app: AuraApp) {
    val settings by app.settings.settings.collectAsState()
    var showApiDialog by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
    ) {
        item {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "设置",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))
        }
        item {
            SectionHeader("外观")
            SettingRow(
                title = "深色模式",
                description = "使用深色配色"
            ) {
                Switch(
                    checked = settings.darkTheme,
                    onCheckedChange = { v ->
                        app.settings.update { it.copy(darkTheme = v) }
                    }
                )
            }
            HorizontalDivider(alpha = 0.2f)
        }
        item {
            SectionHeader("语音")
            SettingRow(
                title = "语音播报",
                description = "关闭后 AURA 只在屏幕上回答"
            ) {
                Switch(
                    checked = settings.ttsEnabled,
                    onCheckedChange = { v ->
                        app.settings.update { it.copy(ttsEnabled = v) }
                    }
                )
            }
            HorizontalDivider(alpha = 0.2f)
            SettingRow(
                title = "播报语言",
                description = "当前：${settings.ttsLanguage}"
            ) {
                TextButton(onClick = {
                    val next = if (settings.ttsLanguage.startsWith("zh")) "en" else "zh"
                    app.settings.update { it.copy(ttsLanguage = next) }
                    app.voice.applyTtsSettings(settings.copy(ttsLanguage = next))
                }) { Text("切换") }
            }
            SliderRow(
                title = "语速",
                value = settings.ttsSpeed,
                range = 0.5f..2.0f,
                onChange = { v ->
                    app.settings.update { it.copy(ttsSpeed = v) }
                    app.voice.applyTtsSettings(settings.copy(ttsSpeed = v))
                }
            )
            SliderRow(
                title = "音调",
                value = settings.ttsPitch,
                range = 0.5f..2.0f,
                onChange = { v ->
                    app.settings.update { it.copy(ttsPitch = v) }
                    app.voice.applyTtsSettings(settings.copy(ttsPitch = v))
                }
            )
        }
        item {
            SectionHeader("反馈")
            SettingRow(
                title = "振动反馈",
                description = "播报时震动一下"
            ) {
                Switch(
                    checked = settings.vibrateOnResponse,
                    onCheckedChange = { v ->
                        app.settings.update { it.copy(vibrateOnResponse = v) }
                    }
                )
            }
            SettingRow(
                title = "波形动画",
                description = "说话时显示波动"
            ) {
                Switch(
                    checked = settings.showWaveAnimation,
                    onCheckedChange = { v ->
                        app.settings.update { it.copy(showWaveAnimation = v) }
                    }
                )
            }
        }
        item {
            SectionHeader("AI 服务")
            SettingRow(
                title = "自定义 AI",
                description = if (settings.apiConfigured) "已连接：${settings.apiModel}" else "未配置"
            ) {
                OutlinedButton(onClick = { showApiDialog = true }) {
                    Text("配置")
                }
            }
        }
        item {
            SectionHeader("后台服务")
            SettingRow(
                title = "开机自启后台监听",
                description = "开启后 AURA 在手机开机后保持后台语音服务"
            ) {
                Switch(
                    checked = settings.autoStartService,
                    onCheckedChange = { v ->
                        app.settings.update { it.copy(autoStartService = v) }
                    }
                )
            }
        }
        item {
            SectionHeader("权限")
            PermissionRow(name = "录音权限", granted = Permissions.hasRecordAudio(LocalContextCurrent()), required = Manifest.permission.RECORD_AUDIO)
            PermissionRow(name = "通知权限", granted = Permissions.hasNotification(LocalContextCurrent()), required = Manifest.permission.POST_NOTIFICATIONS)
        }
        item {
            SectionHeader("关于")
            SettingRow(
                title = "AURA",
                description = "版本 ${BuildConfig.APP_NAME} ${BuildConfig.APP_VERSION} (${BuildConfig.BUILD_NUMBER})"
            ) {}
        }
    }

    if (showApiDialog) {
        ApiConfigDialog(
            initialUrl = settings.apiProviderUrl,
            initialKey = settings.apiProviderKey,
            initialModel = settings.apiModel,
            onDismiss = { showApiDialog = false },
            onSave = { url, key, model ->
                app.settings.update {
                    it.copy(
                        apiProviderUrl = url,
                        apiProviderKey = key,
                        apiModel = model,
                        apiConnected = url.isNotBlank() && key.isNotBlank() && model.isNotBlank()
                    )
                }
                showApiDialog = false
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Spacer(Modifier.height(12.dp))
    Text(
        text = title,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        style = MaterialTheme.typography.titleSmall
    )
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun SettingRow(
    title: String,
    description: String? = null,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.Medium)
            if (!description.isNullOrBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        content()
    }
}

@Composable
private fun SliderRow(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
            Text(String.format("%.2f", value), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}

@Composable
private fun PermissionRow(name: String, granted: Boolean, required: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (granted) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = null,
            tint = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.width(8.dp))
        Text(name, modifier = Modifier.weight(1f))
        TextButton(onClick = {
            // UI 层不直接处理；仅显示状态。
        }) {
            Text(if (granted) "已授权" else "去授权")
        }
    }
}

@Composable
private fun ApiConfigDialog(
    initialUrl: String,
    initialKey: String,
    initialModel: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var url by remember { mutableStateOf(initialUrl) }
    var key by remember { mutableStateOf(initialKey) }
    var model by remember { mutableStateOf(initialModel) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("自定义 AI 服务") },
        text = {
            Column {
                Text(
                    text = "填入 OpenAI 兼容服务的地址、API Key 与模型名，例如 DeepSeek / 自建 OpenAI 网关。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("服务地址") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = key, onValueChange = { key = it }, label = { Text("API Key") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("模型名") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = { onSave(url.trim(), key.trim(), model.trim()) }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun LocalContextCurrent(): android.content.Context =
    androidx.compose.ui.platform.LocalContext.current

@Composable
private fun HorizontalDivider(alpha: Float) {
    androidx.compose.material3.HorizontalDivider(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = alpha)
    )
}
