package com.bskai.ui.settings

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bskai.AuraApp
import com.bskai.BuildConfig
import com.bskai.agent.LlmClient
import com.bskai.data.DefaultApiUrlPresets
import com.bskai.data.DefaultModelPresets
import com.bskai.data.ThemeStyle
import com.bskai.data.languageList
import com.bskai.permission.ShizukuBridge
import com.bskai.update.DownloadStatus
import com.bskai.update.GitHubApi
import com.bskai.update.RemoteRelease
import com.bskai.update.UpdateInstaller
import com.bskai.util.Permissions
import com.bskai.workspace.WorkspaceEntry
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    app: AuraApp,
    onClose: () -> Unit,
    onOpenTerminal: () -> Unit = {}
) {
    val settings by app.settings.settings.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val workspaceManager = app.workspace
    val workspaces by workspaceManager.workspaces.collectAsState()
    val activeWorkspaceId by workspaceManager.activeId.collectAsState()
    val activeWorkspace = workspaces.firstOrNull { it.id == activeWorkspaceId } ?: workspaces.firstOrNull()
    val shizukuState by app.shizuku.state.collectAsState()

    var showProviderDialog by rememberSaveable { mutableStateOf(false) }
    var showWorkspaceDialog by rememberSaveable { mutableStateOf(false) }
    var showLanguageDialog by rememberSaveable { mutableStateOf(false) }
    var showAboutDialog by rememberSaveable { mutableStateOf(false) }
    var showUpdateDialog by rememberSaveable { mutableStateOf(false) }

    val recordAudioLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val notifPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("设置", modifier = Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "关闭") }
                }
                Spacer(Modifier.height(12.dp))
            }
            item {
                SectionHeader("外观", Icons.Default.Palette)
                ThemePicker(current = settings.themeStyle) { app.settings.update { s -> s.copy(themeStyle = it) } }
                Spacer(Modifier.height(6.dp))
                SettingRowSwitch("深色模式", "使用深色配色", settings.darkTheme, Icons.Default.Palette) { app.settings.update { s -> s.copy(darkTheme = it) } }
                SectionDivider()
            }
            item {
                SectionHeader("语音与反馈", Icons.Default.Speaker)
                SettingRowSwitch("语音播报", "关闭后 AURA 只在屏幕上回答", settings.ttsEnabled, Icons.Default.Speaker) { app.settings.update { s -> s.copy(ttsEnabled = it) } }
                SettingRowButton("播报语言", "当前：${languageList.find { p -> p.first == settings.ttsLanguage }?.second ?: settings.ttsLanguage}", Icons.Default.Language, "选择") { showLanguageDialog = true }
                SliderRow("语速", settings.ttsSpeed, 0.5f..2.0f) { app.settings.update { s -> s.copy(ttsSpeed = it) } }
                SliderRow("音调", settings.ttsPitch, 0.5f..2.0f) { app.settings.update { s -> s.copy(ttsPitch = it) } }
                SettingRowSwitch("交互反馈", "说话时震动与波形动画", settings.vibrateOnResponse, Icons.Default.Vibration) { app.settings.update { s -> s.copy(vibrateOnResponse = it, showWaveAnimation = it) } }
                SectionDivider()
            }
            item {
                SectionHeader("自定义服务商", Icons.Default.Tune)
                if (settings.apiConfigured) {
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(settings.apiModel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(settings.apiProviderUrl, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            TextButton(onClick = { showProviderDialog = true }) { Text("编辑") }
                        }
                    }
                } else {
                    OutlinedButton(onClick = { showProviderDialog = true }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("配置 AI 服务商")
                    }
                }
                if (settings.customModelList.isNotEmpty()) {
                    Text("已保存 ${settings.customModelList.size} 个自定义模型", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                SectionDivider()
            }
            item {
                SectionHeader("工具与工作区", Icons.Default.Code)
                SettingRowSwitch("AI 工具调用", if (settings.agentToolsEnabled) "已启用 · 可使用终端、文件读写" else "已关闭", settings.agentToolsEnabled, Icons.Default.Code) { app.settings.update { s -> s.copy(agentToolsEnabled = it) } }
                SettingRowButton("工作区", "当前：${activeWorkspace?.name ?: "默认"} · 共 ${workspaces.size} 个", Icons.Default.Folder, "管理") { showWorkspaceDialog = true }
                SettingRowButton("内置终端", "打开 AURA 内置终端", Icons.Default.Terminal, "打开", onOpenTerminal)
                SectionDivider()
            }
            item {
                SectionHeader("后台服务", Icons.Default.Vibration)
                SettingRowSwitch("开机自启后台监听", "手机开机后保持后台语音服务", settings.autoStartService, Icons.Default.Vibration) { app.settings.update { s -> s.copy(autoStartService = it) } }
                SectionDivider()
            }
            item {
                SectionHeader("权限", Icons.Default.Security)
                PermissionRow("录音权限", "用于语音输入", Permissions.hasRecordAudio(context), Icons.Default.Mic) { recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO) }
                PermissionRow("通知权限", "用于播报与后台服务", Permissions.hasNotification(context), Icons.Default.Notifications) { notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                PermissionRow("Shizuku", "提权终端命令（无需 root）", shizukuState == ShizukuBridge.State.GRANTED, Icons.Default.Security) { if (shizukuState == ShizukuBridge.State.NEED_PERMISSION) app.shizuku.requestPermission() }
                SectionDivider()
            }
            item {
                SectionHeader("更新", Icons.Default.SystemUpdateAlt)
                SettingRowButton("检查更新", "当前 ${BuildConfig.APP_VERSION} (${BuildConfig.BUILD_NUMBER})", Icons.Default.SystemUpdateAlt, "检查") { showUpdateDialog = true }
                SettingRowButton("历史版本", "浏览与下载历史版本", Icons.Default.History, "查看") { showUpdateDialog = true }
                SectionDivider()
            }
            item {
                SectionHeader("关于", Icons.Default.Info)
                SettingRowButton("AURA ${BuildConfig.APP_VERSION}", "查看完整介绍与升级指南", Icons.Default.Info, "关于") { showAboutDialog = true }
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    if (showProviderDialog) ProviderConfigDialog(app, { showProviderDialog = false }, { showProviderDialog = false })
    if (showWorkspaceDialog) WorkspaceManageDialog(app.workspace, { showWorkspaceDialog = false })
    if (showLanguageDialog) LanguageSelectDialog(settings.ttsLanguage, { lang -> app.settings.update { s -> s.copy(ttsLanguage = lang) }; app.voice.applyTtsSettings(settings.copy(ttsLanguage = lang)) }, { showLanguageDialog = false })
    if (showUpdateDialog) UpdateCenterDialog({ showUpdateDialog = false })
    if (showAboutDialog) AboutAuraDialog({ showAboutDialog = false })
}

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
}

@Composable
fun SettingRowSwitch(title: String, description: String, checked: Boolean, icon: ImageVector, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingRowButton(title: String, description: String, icon: ImageVector, buttonText: String, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        OutlinedButton(onClick = onClick) { Text(buttonText) }
    }
}

@Composable
private fun PermissionRow(name: String, description: String, granted: Boolean, icon: ImageVector, onRequest: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(enabled = !granted, onClick = onRequest),
        shape = RoundedCornerShape(12.dp),
        color = if (granted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (granted) Icons.Default.CheckCircle else icon, contentDescription = null, tint = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.Medium)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!granted) Button(onClick = onRequest, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("去授权") }
            else Text("已授权", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun SliderRow(title: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
            Text(String.format("%.2f", value), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}

@Composable
private fun ThemePicker(current: ThemeStyle, onSelect: (ThemeStyle) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text("界面主题", fontWeight = FontWeight.Medium)
        Text("切换后立即生效", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeStyle.entries.forEach { style ->
                ThemeOptionRow(style = style, selected = style == current, onClick = { onSelect(style) })
            }
        }
    }
}

@Composable
private fun ThemeOptionRow(style: ThemeStyle, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ThemeSwatch(style)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(style.label, fontWeight = FontWeight.SemiBold)
                Text(style.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (selected) Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun ThemeSwatch(style: ThemeStyle) {
    val brush = when (style) {
        ThemeStyle.AURORA -> Brush.linearGradient(com.bskai.ui.theme.AuroraGradient)
        ThemeStyle.NEON -> Brush.linearGradient(com.bskai.ui.theme.NeonGlow)
        ThemeStyle.GLASS -> Brush.linearGradient(listOf(com.bskai.ui.theme.GlassTint, com.bskai.ui.theme.GlassTint, MaterialTheme.colorScheme.surface))
        ThemeStyle.VOICE -> Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))
    }
    Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(brush))
}

@Composable
private fun ProviderConfigDialog(app: AuraApp, onDismiss: () -> Unit, onSaved: () -> Unit) {
    val settings by app.settings.settings.collectAsState()
    var url by rememberSaveable { mutableStateOf(settings.apiProviderUrl) }
    var key by rememberSaveable { mutableStateOf(settings.apiProviderKey) }
    var model by rememberSaveable { mutableStateOf(settings.apiModel) }
    var showPresets by rememberSaveable { mutableStateOf(false) }
    var showModelPresets by rememberSaveable { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var testing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val customModels = remember { mutableStateListOf<String>().apply { addAll(settings.customModelList) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("自定义服务商", fontWeight = FontWeight.SemiBold) },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp)) {
                item {
                    Text("服务商地址", style = MaterialTheme.typography.labelMedium)
                    OutlinedTextField(value = url, onValueChange = { url = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("https://api.openai.com/v1") })
                    OutlinedButton(onClick = { showPresets = !showPresets }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                        Text("选择预设服务商"); Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                    }
                    if (showPresets) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            DefaultApiUrlPresets.forEach { presetUrl ->
                                OutlinedButton(onClick = { url = presetUrl }, modifier = Modifier.fillMaxWidth()) {
                                    Text(presetUrl, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                item {
                    Text("API Key", style = MaterialTheme.typography.labelMedium)
                    OutlinedTextField(value = key, onValueChange = { key = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("sk-...") })
                    Spacer(Modifier.height(8.dp))
                }
                item {
                    Text("模型名称", style = MaterialTheme.typography.labelMedium)
                    OutlinedTextField(value = model, onValueChange = { model = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("gpt-4o-mini") })
                    OutlinedButton(onClick = { showModelPresets = !showModelPresets }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                        Text("选择预设模型"); Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                    }
                    if (showModelPresets) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            DefaultModelPresets.forEach { presetModel ->
                                OutlinedButton(onClick = { model = presetModel }, modifier = Modifier.fillMaxWidth()) {
                                    Text(presetModel, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("自定义模型列表", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                        TextButton(onClick = { customModels.add("") }) { Text("添加") }
                    }
                    customModels.forEachIndexed { index, m ->
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(value = m, onValueChange = { customModels[index] = it }, modifier = Modifier.weight(1f), singleLine = true, placeholder = { Text("模型 ID") })
                            IconButton(onClick = { customModels.removeAt(index) }) { Icon(Icons.Default.Delete, contentDescription = "删除") }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Button(onClick = {
                        scope.launch {
                            testing = true; testResult = null
                            try {
                                val models = LlmClient(app).listModels(url, key)
                                testResult = if (models.isNotEmpty()) "成功 · 找到 ${models.size} 个模型" else "成功 · 但未返回模型列表"
                            } catch (e: Exception) {
                                testResult = "失败: ${e.message}"
                            }
                            testing = false
                        }
                    }, modifier = Modifier.fillMaxWidth(), enabled = !testing && url.isNotBlank() && key.isNotBlank()) {
                        if (testing) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp) else Icon(Icons.Default.SwapHoriz, contentDescription = null)
                        Spacer(Modifier.width(8.dp)); Text("测试连接")
                    }
                    testResult?.let { result ->
                        Text(result, color = if (result.startsWith("成功")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                app.settings.update { s -> s.copy(apiProviderUrl = url, apiProviderKey = key, apiModel = model, customModelList = customModels.filter { it.isNotBlank() }) }
                onSaved()
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun WorkspaceManageDialog(workspaceManager: com.bskai.workspace.WorkspaceManager, onDismiss: () -> Unit) {
    val workspaces by workspaceManager.workspaces.collectAsState()
    val activeId by workspaceManager.activeId.collectAsState()
    var showCreate by rememberSaveable { mutableStateOf(false) }
    var newPath by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("工作区管理", fontWeight = FontWeight.SemiBold) },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                items(workspaces) { ws ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { workspaceManager.setActive(ws.id) },
                        colors = CardDefaults.cardColors(containerColor = if (ws.id == activeId) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = if (ws.id == activeId) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(ws.name, fontWeight = FontWeight.Medium)
                                Text(ws.treeUri ?: "内部工作区", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            if (ws.id == activeId) Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            else TextButton(onClick = { workspaceManager.setActive(ws.id) }) { Text("选择") }
                            IconButton(onClick = { workspaceManager.remove(ws.id) }) { Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
                if (showCreate) {
                    item {
                        OutlinedTextField(value = newPath, onValueChange = { newPath = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("工作区路径") })
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                            TextButton(onClick = {
                                if (newPath.isNotBlank()) {
                                    workspaceManager.createInternal(newPath, newPath)
                                    newPath = ""; showCreate = false
                                }
                            }) { Text("确认") }
                            TextButton(onClick = { showCreate = false; newPath = "" }) { Text("取消") }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = { showCreate = true }) { Text("新建") }
                TextButton(onClick = {
                    val intent = android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT_TREE)
                    (context as? android.app.Activity)?.startActivityForResult(intent, 1001)
                }) { Text("导入") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}

@Composable
private fun LanguageSelectDialog(current: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择播报语言", fontWeight = FontWeight.SemiBold) },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                items(languageList) { (code, label) ->
                    Row(modifier = Modifier.fillMaxWidth().clickable { onSelect(code); onDismiss() }.padding(vertical = 8.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = code == current, onClick = { onSelect(code); onDismiss() })
                        Spacer(Modifier.width(8.dp))
                        Text(label)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}

@Composable
private fun UpdateCenterDialog(onDismiss: () -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var releases by remember { mutableStateOf<List<RemoteRelease>>(emptyList()) }
    var loading by rememberSaveable { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var downloadStatus by remember { mutableStateOf<Map<String, DownloadStatus>>(emptyMap()) }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                releases = GitHubApi.listReleases()
            } catch (e: Exception) {
                error = e.message
            }
            loading = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("更新中心", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp)) {
                TabRow(selectedTabIndex = tab) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("最新版本") })
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("历史版本") })
                }
                Spacer(Modifier.height(8.dp))
                if (loading) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else if (error != null) {
                    Text("加载失败: $error", color = MaterialTheme.colorScheme.error)
                } else {
                    val current = releases.firstOrNull()
                    if (tab == 0 && current != null) {
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(current.name, fontWeight = FontWeight.SemiBold)
                                Text("版本: ${current.versionName}", style = MaterialTheme.typography.bodySmall)
                                Text("发布: ${current.publishedAtLabel()}", style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.height(8.dp))
                                val status = downloadStatus[current.tagName]
                                when (status) {
                                    is DownloadStatus.Downloading -> LinearProgressIndicator(progress = { status.percent / 100f }, modifier = Modifier.fillMaxWidth())
                                    is DownloadStatus.Done -> {
                                        Button(onClick = { UpdateInstaller.install(context, java.io.File(status.localPath)) }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.InstallMobile, contentDescription = null); Spacer(Modifier.width(4.dp)); Text("安装") }
                                    }
                                    is DownloadStatus.Failed -> Text("下载失败: ${status.message}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                    else -> Button(onClick = {
                                        scope.launch {
                                            val target = java.io.File(context.cacheDir, "${current.tagName}.apk")
                                            GitHubApi.downloadApk(current.apkUrl, target).collect { st ->
                                                downloadStatus = downloadStatus + (current.tagName to st)
                                            }
                                        }
                                    }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.CloudDownload, contentDescription = null); Spacer(Modifier.width(4.dp)); Text("下载") }
                                }
                            }
                        }
                    } else if (tab == 1) {
                        LazyColumn {
                            items(releases) { release ->
                                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(release.name, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${release.versionName} · ${release.publishedAtLabel()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        val status = downloadStatus[release.tagName]
                                        if (status is DownloadStatus.Done) {
                                            TextButton(onClick = { UpdateInstaller.install(context, java.io.File(status.localPath)) }) { Text("安装") }
                                        } else {
                                            TextButton(onClick = {
                                                scope.launch {
                                                    val target = java.io.File(context.cacheDir, "${release.tagName}.apk")
                                                    GitHubApi.downloadApk(release.apkUrl, target).collect { st ->
                                                        downloadStatus = downloadStatus + (release.tagName to st)
                                                    }
                                                }
                                            }) { Text("下载") }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}

@Composable
private fun AboutAuraDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("关于 AURA", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("AURA ${BuildConfig.APP_VERSION}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("构建号: ${BuildConfig.BUILD_NUMBER}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Text("AURA 是一款 AI 语音助手，支持语音交互、AI 工具调用、内置终端、工作区管理等功能。", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Text("升级指南: 下载最新 APK 后直接安装即可，数据会自动保留。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}
