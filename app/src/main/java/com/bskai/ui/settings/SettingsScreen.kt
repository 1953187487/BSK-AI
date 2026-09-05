package com.bskai.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bskai.AuraApp
import com.bskai.BuildConfig
import com.bskai.data.DefaultApiUrlPresets
import com.bskai.data.DefaultModelPresets
import com.bskai.data.ThemeStyle
import com.bskai.update.GitHubApi
import com.bskai.update.RemoteRelease
import com.bskai.ui.theme.AuroraGradient
import com.bskai.ui.theme.GlassTint
import com.bskai.ui.theme.NeonGlow
import com.bskai.util.Permissions
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    app: AuraApp,
    onClose: () -> Unit
) {
    val settings by app.settings.settings.collectAsState()
    var showApiDialog by rememberSaveable { mutableStateOf(false) }
    var checking by remember { mutableStateOf(false) }
    var historyReleases by remember { mutableStateOf<List<RemoteRelease>?>(null) }
    var historyLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
        ) {
            item {
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "设置",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            item {
                SectionHeader("外观")
                ThemePicker(
                    current = settings.themeStyle,
                    onSelect = { style ->
                        app.settings.update { it.copy(themeStyle = style) }
                    }
                )
                Spacer(Modifier.height(6.dp))
                SettingRow(title = "深色模式", description = "使用深色配色") {
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
                SettingRow(title = "语音播报", description = "关闭后 AURA 只在屏幕上回答") {
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
                SettingRow(title = "振动反馈", description = "播报时震动一下") {
                    Switch(
                        checked = settings.vibrateOnResponse,
                        onCheckedChange = { v ->
                            app.settings.update { it.copy(vibrateOnResponse = v) }
                        }
                    )
                }
                SettingRow(title = "波形动画", description = "说话时显示波动") {
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
                    description = if (settings.apiConfigured) "已连接：${settings.apiModel}"
                    else "未配置 · 可在对话页选择预设模型"
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
                PermissionRow(
                    name = "录音权限",
                    granted = Permissions.hasRecordAudio(context),
                    onRequest = { }
                )
                PermissionRow(
                    name = "通知权限",
                    granted = Permissions.hasNotification(context),
                    onRequest = { }
                )
            }
            item {
                SectionHeader("版本")
                SettingRow(
                    title = "检查更新",
                    description = "当前 ${BuildConfig.APP_VERSION} (${BuildConfig.BUILD_NUMBER})"
                ) {
                    OutlinedButton(
                        onClick = {
                            if (checking) return@OutlinedButton
                            checking = true
                            scope.launch {
                                val releases = GitHubApi.listReleases()
                                checking = false
                                val r = releases
                                    .filter { it.versionCode > 0 }
                                    .sortedByDescending { it.versionCode }
                                val latest = r.firstOrNull()
                                val hasUpdate = latest != null && latest.versionCode > BuildConfig.BUILD_NUMBER
                                android.widget.Toast.makeText(
                                    context,
                                    if (hasUpdate) "发现新版本 ${latest?.versionName}" else "已是最新",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        enabled = !checking
                    ) {
                        Icon(Icons.Default.SystemUpdateAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (checking) "检查中…" else "检查")
                    }
                }
                SettingRow(title = "历史版本", description = "查看所有已发布的 AURA 版本") {
                    OutlinedButton(onClick = {
                        historyReleases = emptyList()
                        historyLoading = true
                        scope.launch {
                            val r = GitHubApi.listReleases()
                                .filter { it.versionCode > 0 }
                                .sortedByDescending { it.versionCode }
                            historyReleases = r
                            historyLoading = false
                        }
                    }) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("查看")
                    }
                }
                SettingRow(
                    title = "关于 AURA",
                    description = "${BuildConfig.APP_NAME} ${BuildConfig.APP_VERSION} · Kotlin + Jetpack Compose"
                ) {}
            }
        }
    }

    if (showApiDialog) {
        ApiConfigDialog(
            initialUrl = settings.apiProviderUrl,
            initialKey = settings.apiProviderKey,
            initialModel = settings.apiModel,
            urlPresets = DefaultApiUrlPresets,
            modelPresets = DefaultModelPresets,
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

    historyReleases?.let { list ->
        com.bskai.ui.update.HistoryDialog(
            releases = list,
            loading = historyLoading && list.isEmpty(),
            onDismiss = { historyReleases = null }
        )
    }
}

@Composable
private fun ThemePicker(
    current: ThemeStyle,
    onSelect: (ThemeStyle) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(
            text = "界面主题",
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "切换后立即生效，影响对话页与设置页",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeStyle.entries.forEach { style ->
                ThemeOptionRow(
                    style = style,
                    selected = style == current,
                    onClick = { onSelect(style) }
                )
            }
        }
    }
}

@Composable
private fun ThemeOptionRow(
    style: ThemeStyle,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ThemeSwatch(style)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = style.label,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = style.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (selected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ThemeSwatch(style: ThemeStyle) {
    val brush = when (style) {
        ThemeStyle.AURORA -> androidx.compose.ui.graphics.Brush.linearGradient(AuroraGradient)
        ThemeStyle.NEON -> androidx.compose.ui.graphics.Brush.linearGradient(NeonGlow)
        ThemeStyle.GLASS -> androidx.compose.ui.graphics.Brush.linearGradient(
            listOf(GlassTint, GlassTint, MaterialTheme.colorScheme.surface)
        )
        ThemeStyle.VOICE -> androidx.compose.ui.graphics.Brush.linearGradient(
            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
        )
    }
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(brush)
    )
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
private fun PermissionRow(
    name: String,
    granted: Boolean,
    onRequest: () -> Unit
) {
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
        TextButton(onClick = onRequest) {
            Text(if (granted) "已授权" else "去授权")
        }
    }
}

@Composable
private fun ApiConfigDialog(
    initialUrl: String,
    initialKey: String,
    initialModel: String,
    urlPresets: List<String>,
    modelPresets: List<String>,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var url by remember { mutableStateOf(initialUrl) }
    var key by remember { mutableStateOf(initialKey) }
    var model by remember { mutableStateOf(initialModel) }
    var showUrlMenu by remember { mutableStateOf(false) }
    var showModelMenu by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("自定义 AI 服务") },
        text = {
            Column {
                Text(
                    text = "填入 OpenAI 兼容服务的地址、API Key 与模型名，模型可从下拉预设选择。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))

                Box {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("服务地址") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { showUrlMenu = !showUrlMenu }) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = showUrlMenu,
                        onDismissRequest = { showUrlMenu = false }
                    ) {
                        urlPresets.forEach { preset ->
                            DropdownMenuItem(
                                text = { Text(preset) },
                                onClick = {
                                    url = preset
                                    showUrlMenu = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = key, onValueChange = { key = it }, label = { Text("API Key") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))

                Box {
                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = it },
                        label = { Text("模型名") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { showModelMenu = !showModelMenu }) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = showModelMenu,
                        onDismissRequest = { showModelMenu = false }
                    ) {
                        modelPresets.forEach { preset ->
                            DropdownMenuItem(
                                text = { Text(preset) },
                                onClick = {
                                    model = preset
                                    showModelMenu = false
                                }
                            )
                        }
                    }
                }

                if (testResult != null) {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = testResult!!,
                            modifier = Modifier.padding(10.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(url.trim(), key.trim(), model.trim()) }) { Text("保存") }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = {
                        if (testing) return@TextButton
                        if (url.isBlank() || key.isBlank() || model.isBlank()) {
                            testResult = "请先填写完整的服务地址、API Key 与模型名"
                            return@TextButton
                        }
                        testing = true
                        testResult = "正在测试模型…"
                        scope.launch {
                            val test = com.bskai.agent.LlmClient(context)
                            val res = runCatching {
                                test.chat(
                                    com.bskai.data.AppSettings(
                                        apiProviderUrl = url.trim(),
                                        apiProviderKey = key.trim(),
                                        apiModel = model.trim()
                                    ),
                                    listOf(
                                        com.bskai.agent.ChatMsg(
                                            "system",
                                            "请用一句话用简体中文回复自我介绍"
                                        ),
                                        com.bskai.agent.ChatMsg("user", "测试请求")
                                    )
                                )
                            }
                            testing = false
                            testResult = res.fold(
                                onSuccess = { "✓ 测试成功：$it".take(180) },
                                onFailure = { "× 测试失败：${it.message ?: "未知错误"}" }
                            )
                        }
                    },
                    enabled = !testing
                ) {
                    Text(if (testing) "测试中…" else "测试模型")
                }
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        }
    )
}

@Composable
private fun HorizontalDivider(alpha: Float) {
    androidx.compose.material3.HorizontalDivider(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = alpha)
    )
}
