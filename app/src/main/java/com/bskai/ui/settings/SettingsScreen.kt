package com.bskai.ui.settings

import android.Manifest
import android.content.Context
import java.io.File
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bskai.AuraApp
import com.bskai.BuildConfig
import com.bskai.agent.LlmClient
import com.bskai.data.ChatMode
import com.bskai.data.DefaultApiUrlPresets
import com.bskai.data.DefaultModelPresets
import com.bskai.data.ThemeStyle
import com.bskai.permission.ShizukuBridge
import com.bskai.update.DownloadStatus
import com.bskai.update.GitHubApi
import com.bskai.update.RemoteRelease
import com.bskai.update.UpdateInstaller
import com.bskai.util.Permissions
import com.bskai.workspace.WorkspaceEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    var showLocalModelDialog by rememberSaveable { mutableStateOf(false) }
    var showWorkspaceDialog by rememberSaveable { mutableStateOf(false) }
    var showLanguageDialog by rememberSaveable { mutableStateOf(false) }
    var showAboutDialog by rememberSaveable { mutableStateOf(false) }
    var showUpdateDialog by rememberSaveable { mutableStateOf(false) }

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
                SectionHeader("对话模式", Icons.Default.Tune)
                SettingRowButton(
                    "当前模式",
                    if (settings.chatMode == ChatMode.DEV) "应用开发模式" else "思考模式 (深度 ${settings.thinkingLevel}/3)",
                    Icons.Default.Tune,
                    "切换"
                ) {
                    val next = if (settings.chatMode == ChatMode.THINK) ChatMode.DEV else ChatMode.THINK
                    app.settings.update { it.copy(chatMode = next) }
                }
                if (settings.chatMode == ChatMode.THINK) {
                    Text("思考深度: ${settings.thinkingLevel}/3", fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    Slider(
                        value = settings.thinkingLevel.toFloat(),
                        onValueChange = { },
                        onValueChangeFinished = {
                            val next = if (settings.thinkingLevel >= 3) 1 else settings.thinkingLevel + 1
                            app.settings.update { it.copy(thinkingLevel = next) }
                        },
                        valueRange = 1f..3f,
                        steps = 1
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("简要", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("标准", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("深入", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Text("应用开发模式：AI 辅助开发 Android 应用，支持构建 APK", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    SettingRowButton(
                        "开发依赖",
                        if (settings.devDependenciesDownloaded) "已下载构建工具" else "需要下载构建依赖",
                        Icons.Default.Code,
                        if (settings.devDependenciesDownloaded) "已就绪" else "下载"
                    ) {
                        scope.launch {
                            app.settings.update { it.copy(devDependenciesDownloaded = true) }
                        }
                    }
                }
                SectionDivider()
            }
            item {
                SectionHeader("模型管理", Icons.Default.Tune)
                SettingRowButton("本地模型 AI", if (settings.localModels.isEmpty()) "选择本地 AI 提供商" else "已下载 ${settings.localModels.size} 个本地模型", Icons.Default.CloudDownload, "配置") { showLocalModelDialog = true }
                SettingRowButton("外接模型（API）", if (settings.apiConfigured) "已配置 ${settings.apiModel}" else "未配置外接模型", Icons.Default.SwapHoriz, "配置") { showProviderDialog = true }
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
                SectionHeader("权限", Icons.Default.Security)
                PermissionRow("通知权限", "用于后台通知", Permissions.hasNotification(context), Icons.Default.Notifications) { notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                PermissionRow("Shizuku", "提权终端命令（无需 root）", shizukuState == ShizukuBridge.State.GRANTED, Icons.Default.Security) { if (shizukuState == ShizukuBridge.State.NEED_PERMISSION) app.shizuku.requestPermission() }
                SectionDivider()
            }
            item {
                SectionHeader("更新", Icons.Default.SystemUpdateAlt)
                SettingRowButton("检查更新", "当前 ${BuildConfig.APP_VERSION}", Icons.Default.SystemUpdateAlt, "检查") { showUpdateDialog = true }
                SettingRowButton("历史版本", "浏览与下载历史版本", Icons.Default.History, "查看") { showUpdateDialog = true }
                ApkCleanerCard(context)
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
    if (showLocalModelDialog) LocalModelDownloadDialog(app) { showLocalModelDialog = false }
    if (showWorkspaceDialog) WorkspaceManageDialog(app.workspace, { showWorkspaceDialog = false })
    if (showLanguageDialog) LanguageSelectDialog(settings.selectedLanguage, { lang -> app.settings.update { s -> s.copy(selectedLanguage = lang) } }, { showLanguageDialog = false })
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
        TextButton(onClick = onClick) { Text(buttonText) }
    }
}

@Composable
fun PermissionRow(title: String, description: String, granted: Boolean, icon: ImageVector, onRequest: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (granted) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        } else {
            TextButton(onClick = onRequest) { Text("授权") }
        }
    }
}

@Composable
fun ThemePicker(current: ThemeStyle, onSelect: (ThemeStyle) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ThemeStyle.entries.forEach { style ->
            Surface(
                modifier = Modifier.weight(1f).height(56.dp).clickable { onSelect(style) },
                shape = RoundedCornerShape(12.dp),
                color = if (current == style) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(style.label, fontSize = 12.sp, fontWeight = if (current == style) FontWeight.Bold else FontWeight.Normal)
                        Text(style.description, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
fun ProviderConfigDialog(app: AuraApp, onDismiss: () -> Unit, onSaved: () -> Unit) {
    val settings by app.settings.settings.collectAsState()
    val scope = rememberCoroutineScope()
    var url by rememberSaveable { mutableStateOf(settings.apiProviderUrl) }
    var key by rememberSaveable { mutableStateOf(settings.apiProviderKey) }
    var model by rememberSaveable { mutableStateOf(settings.apiModel) }
    var testing by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var models by remember { mutableStateOf<List<String>>(emptyList()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("配置外接模型", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn {
                item {
                    OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("API 地址") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = key, onValueChange = { key = it }, label = { Text("API Key") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DefaultApiUrlPresets.forEach { preset ->
                            Surface(
                                modifier = Modifier.clickable { url = preset },
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            ) {
                                Text(preset.removePrefix("https://").removeSuffix("/v1"), fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                scope.launch {
                                    testing = true
                                    testResult = null
                                    models = emptyList()
                                    try {
                                        val result = LlmClient(app).listModels(url, key)
                                        models = result
                                        testResult = "连接成功！找到 ${result.size} 个模型"
                                    } catch (e: Exception) {
                                        testResult = "连接失败: ${e.message}"
                                    }
                                    testing = false
                                }
                            },
                            enabled = url.isNotBlank() && !testing
                        ) {
                            Text(if (testing) "测试中…" else "测试连接")
                        }
                        if (models.isNotEmpty()) {
                            Text("${models.size} 个模型可用", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (testResult != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(testResult!!, style = MaterialTheme.typography.bodySmall, color = if (testResult!!.startsWith("连接成功")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                    }
                    if (models.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text("可用模型", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                    }
                }
                items(models) { m ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).clickable { model = m },
                        shape = RoundedCornerShape(8.dp),
                        color = if (m == model) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ) {
                        Text(m, modifier = Modifier.padding(10.dp), fontWeight = if (m == model) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                app.settings.update { it.copy(apiProviderUrl = url, apiProviderKey = key, apiModel = model) }
                onSaved()
            }, enabled = url.isNotBlank() && key.isNotBlank() && model.isNotBlank()) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun LocalModelDownloadDialog(app: AuraApp, onDismiss: () -> Unit) {
    val settings by app.settings.settings.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var selectedProvider by rememberSaveable { mutableStateOf("") }
    var providerUrl by rememberSaveable { mutableStateOf("") }
    var providerKey by rememberSaveable { mutableStateOf("") }
    var availableModels by remember { mutableStateOf<List<ModelDownloadInfo>>(emptyList()) }
    var isLoadingModels by remember { mutableStateOf(false) }
    var downloading by remember { mutableStateOf("") }
    var downloadProgress by remember { mutableStateOf(0f) }
    var downloadSpeed by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val providers = listOf(
        "Ollama" to "http://localhost:11434/v1",
        "LM Studio" to "http://localhost:1234/v1",
        "vLLM" to "http://localhost:8000/v1",
        "Jan" to "http://localhost:1337/v1",
        "Custom" to ""
    )

    // Auto-load models when provider is selected
    LaunchedEffect(selectedProvider, providerUrl) {
        if (selectedProvider.isNotEmpty() && providerUrl.isNotBlank() && selectedProvider != "Custom") {
            isLoadingModels = true
            errorMessage = null
            try {
                val models = LlmClient(app).listModels(providerUrl, providerKey)
                availableModels = models.map { ModelDownloadInfo(it, 0f, "") }
                app.settings.update { it.copy(apiProviderUrl = providerUrl, apiProviderKey = providerKey, modelSource = "local") }
            } catch (e: Exception) {
                errorMessage = "加载失败: ${e.message}"
                availableModels = emptyList()
            }
            isLoadingModels = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("本地模型 AI", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn {
                item {
                    Text("选择提供商", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        providers.chunked(2).forEach { row ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                row.forEach { (name, url) ->
                                    Surface(
                                        modifier = Modifier.weight(1f).height(48.dp)
                                            .clickable {
                                                selectedProvider = name
                                                providerUrl = url
                                                availableModels = emptyList()
                                                errorMessage = null
                                            },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (selectedProvider == name) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text(name, fontSize = 13.sp, fontWeight = if (selectedProvider == name) FontWeight.Bold else FontWeight.Normal)
                                        }
                                    }
                                }
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
                if (selectedProvider.isNotEmpty()) {
                    item {
                        OutlinedTextField(
                            value = providerUrl,
                            onValueChange = { providerUrl = it },
                            label = { Text("API 地址") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = providerKey,
                            onValueChange = { providerKey = it },
                            label = { Text("API Key (可选)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    isLoadingModels = true
                                    errorMessage = null
                                    try {
                                        val models = LlmClient(app).listModels(providerUrl, providerKey)
                                        availableModels = models.map { ModelDownloadInfo(it, 0f, "") }
                                        app.settings.update { it.copy(apiProviderUrl = providerUrl, apiProviderKey = providerKey, modelSource = "local") }
                                    } catch (e: Exception) {
                                        errorMessage = "加载失败: ${e.message}"
                                        availableModels = emptyList()
                                    }
                                    isLoadingModels = false
                                }
                            },
                            enabled = providerUrl.isNotBlank() && !isLoadingModels,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (isLoadingModels) "加载中…" else "刷新模型列表")
                        }
                        if (errorMessage != null) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = errorMessage!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    if (isLoadingModels) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(80.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    items(availableModels) { modelInfo ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = if (modelInfo.name == settings.apiModel) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(modelInfo.name, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                    if (downloading == modelInfo.name) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    } else if (modelInfo.name == settings.apiModel) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    } else {
                                        TextButton(onClick = {
                                            scope.launch {
                                                downloading = modelInfo.name
                                                downloadProgress = 0f
                                                downloadSpeed = "计算中..."
                                                val targetFile = java.io.File(context.cacheDir, "models/${modelInfo.name}.bin")
                                                targetFile.parentFile?.mkdirs()
                                                try {
                                                    GitHubApi.downloadApk("$providerUrl/models/${modelInfo.name}", targetFile).collect { status ->
                                                        when (status) {
                                                            is DownloadStatus.Downloading -> {
                                                                downloadProgress = if (status.total > 0) status.bytesRead.toFloat() / status.total else 0f
                                                                downloadSpeed = formatSpeed(status.bytesRead)
                                                            }
                                                            is DownloadStatus.Done -> {
                                                                downloading = ""
                                                                app.settings.update { it.copy(apiModel = modelInfo.name) }
                                                            }
                                                            is DownloadStatus.Failed -> { downloading = "" }
                                                            else -> {}
                                                        }
                                                    }
                                                } catch (_: Exception) { downloading = "" }
                                            }
                                        }) {
                                            Text("下载", fontSize = 12.sp)
                                        }
                                    }
                                }
                                if (downloading == modelInfo.name) {
                                    Spacer(Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { downloadProgress },
                                        modifier = Modifier.fillMaxWidth().height(4.dp)
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "${(downloadProgress * 100).toInt()}% · $downloadSpeed",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
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

data class ModelDownloadInfo(
    val name: String,
    val progress: Float,
    val speed: String
)

private fun formatSpeed(bytesRead: Long): String {
    if (bytesRead < 1024) return "$bytesRead B/s"
    if (bytesRead < 1024 * 1024) return "${bytesRead / 1024} KB/s"
    return "%.1f MB/s".format(bytesRead / (1024.0 * 1024.0))
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0L) return "?"
    val mb = bytes.toDouble() / (1024 * 1024)
    return "%.2f MB".format(mb)
}

@Composable
fun LanguageSelectDialog(current: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    val languageList = listOf(
        "zh" to "简体中文", "zh-TW" to "繁體中文", "en" to "English",
        "ja" to "日本語", "ko" to "한국어", "es" to "Español",
        "fr" to "Français", "de" to "Deutsch", "ru" to "Русский"
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择语言", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn {
                items(languageList) { (code, name) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(code); onDismiss() }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = code == current, onClick = { onSelect(code); onDismiss() })
                        Spacer(Modifier.width(8.dp))
                        Text(name)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}

@Composable
fun WorkspaceManageDialog(workspaceManager: com.bskai.workspace.WorkspaceManager, onDismiss: () -> Unit) {
    val workspaces by workspaceManager.workspaces.collectAsState()
    val activeId by workspaceManager.activeId.collectAsState()
    var showCreate by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("工作区管理", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn {
                items(workspaces) { ws ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            .clickable { workspaceManager.setActive(ws.id); onDismiss() },
                        colors = CardDefaults.cardColors(
                            containerColor = if (ws.id == activeId) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(ws.name, fontWeight = FontWeight.Medium)
                                Text(if (ws.kind == WorkspaceEntry.Kind.INTERNAL) "内部存储" else "外部存储", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (ws.id == activeId) Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                item {
                    TextButton(onClick = { showCreate = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("新建工作区")
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )

    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("新建工作区") },
            text = { OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("名称") }) },
            confirmButton = {
                Button(onClick = { workspaceManager.createInternal(newName, newName); showCreate = false; newName = "" }, enabled = newName.isNotBlank()) { Text("创建") }
            },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text("取消") } }
        )
    }
}

@Composable
fun UpdateCenterDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var releases by remember { mutableStateOf<List<RemoteRelease>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var downloading by remember { mutableStateOf<String?>(null) }
    var downloadStatus by remember { mutableStateOf<Map<String, DownloadStatus>>(emptyMap()) }

    LaunchedEffect(Unit) {
        scope.launch {
            withContext(Dispatchers.IO) {
                releases = GitHubApi.listReleases()
            }
            loading = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("更新中心", fontWeight = FontWeight.Bold) },
        text = {
            if (loading) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn {
                    item {
                        Text(
                            text = "当前版本: ${BuildConfig.APP_VERSION}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items(releases) { release ->
                        val status = downloadStatus[release.tagName]
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = release.tagName,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (release.isPrerelease) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                                        ) {
                                            Text(
                                                text = "测试版",
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = release.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${release.publishedAtLabel()} · ${formatSize(release.sizeBytes)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(8.dp))
                                when (val s = status) {
                                    is DownloadStatus.Downloading -> {
                                        LinearProgressIndicator(
                                            progress = { s.percent.coerceIn(0, 100) / 100f },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text("${s.percent}%", style = MaterialTheme.typography.labelSmall)
                                    }
                                    is DownloadStatus.Done -> {
                                        OutlinedButton(onClick = {
                                            UpdateInstaller.install(context, File(s.localPath))
                                        }) {
                                            Icon(Icons.Default.InstallMobile, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("安装")
                                        }
                                    }
                                    is DownloadStatus.Failed -> {
                                        Text(
                                            text = "下载失败: ${s.message}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        TextButton(onClick = {
                                            if (release.apkUrl.isBlank()) return@TextButton
                                            downloading = release.tagName
                                            scope.launch {
                                                val targetFile = File(context.cacheDir, "aura-${release.tagName}.apk")
                                                GitHubApi.downloadApk(release.apkUrl, targetFile).collect { status ->
                                                    downloadStatus = downloadStatus + (release.tagName to status)
                                                    if (status is DownloadStatus.Done || status is DownloadStatus.Failed) {
                                                        downloading = null
                                                    }
                                                }
                                            }
                                        }) {
                                            Text("重试")
                                        }
                                    }
                                    else -> {
                                        TextButton(
                                            onClick = {
                                                if (release.apkUrl.isBlank()) return@TextButton
                                                downloading = release.tagName
                                                scope.launch {
                                                    val targetFile = File(context.cacheDir, "aura-${release.tagName}.apk")
                                                    GitHubApi.downloadApk(release.apkUrl, targetFile).collect { status ->
                                                        downloadStatus = downloadStatus + (release.tagName to status)
                                                        if (status is DownloadStatus.Done || status is DownloadStatus.Failed) {
                                                            downloading = null
                                                        }
                                                    }
                                                }
                                            },
                                            enabled = downloading != release.tagName
                                        ) {
                                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(if (downloading == release.tagName) "下载中…" else "下载 APK")
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

private fun startDownload(
    context: Context,
    scope: kotlinx.coroutines.CoroutineScope,
    release: RemoteRelease,
    downloading: androidx.compose.runtime.MutableState<String?>,
    downloadStatus: androidx.compose.runtime.MutableState<Map<String, DownloadStatus>>
) {
    if (release.apkUrl.isBlank()) return
    downloading.value = release.tagName
    scope.launch {
        val targetFile = File(context.cacheDir, "aura-${release.tagName}.apk")
        GitHubApi.downloadApk(release.apkUrl, targetFile).collect { status ->
            downloadStatus.value = downloadStatus.value + (release.tagName to status)
            if (status is DownloadStatus.Done || status is DownloadStatus.Failed) {
                downloading.value = null
            }
        }
    }
}

@Composable
fun AboutAuraDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("关于 AURA", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("AURA ${BuildConfig.APP_VERSION}", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text("AURA 是一款运行在 Android 设备上的 AI 助手，支持多种 AI 模型接入，提供智能对话、工具调用、工作区管理等功能。", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(12.dp))
                Text("功能特性:", fontWeight = FontWeight.Medium)
                Text("• 多模型支持（OpenAI / DeepSeek / 本地 AI）", style = MaterialTheme.typography.bodySmall)
                Text("• 思考模式（3 级深度调节）", style = MaterialTheme.typography.bodySmall)
                Text("• 应用开发模式（AI 辅助开发 Android 应用）", style = MaterialTheme.typography.bodySmall)
                Text("• AI 工具调用（终端 / 文件读写）", style = MaterialTheme.typography.bodySmall)
                Text("• 斜杠命令（/ws /model /clear /help）", style = MaterialTheme.typography.bodySmall)
                Text("• 工作区管理", style = MaterialTheme.typography.bodySmall)
                Text("• 应用内更新", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}

@Composable
fun ApkCleanerCard(context: Context) {
    val scope = rememberCoroutineScope()
    var cleaning by remember { mutableStateOf(false) }
    var cleaned by remember { mutableStateOf(0) }

    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("清除旧安装包", fontWeight = FontWeight.Medium)
                Text("自动扫描并删除旧版本 APK", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (cleaned > 0) {
                Text("已清理 $cleaned 个", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            } else {
                TextButton(onClick = {
                    scope.launch {
                        cleaning = true
                        val cacheDir = context.cacheDir
                        val apks = cacheDir.listFiles { f -> f.name.endsWith(".apk") && !f.name.contains(BuildConfig.APP_VERSION) }
                        apks?.forEach { it.delete() }
                        cleaned = apks?.size ?: 0
                        cleaning = false
                    }
                }, enabled = !cleaning) {
                    Text(if (cleaning) "清理中…" else "清理")
                }
            }
        }
    }
}
