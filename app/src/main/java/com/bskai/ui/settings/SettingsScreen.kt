package com.bskai.ui.settings

import android.content.Context
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bskai.AuraApp
import com.bskai.BuildConfig
import com.bskai.agent.LlmClient
import com.bskai.data.ChatMode
import com.bskai.data.DefaultApiUrlPresets
import com.bskai.data.DefaultModelPresets
import com.bskai.data.Language
import com.bskai.data.ThemeStyle
import com.bskai.data.loadLanguages
import com.bskai.permission.ShizukuBridge
import com.bskai.update.DownloadStatus
import com.bskai.update.GitHubApi
import com.bskai.update.RemoteRelease
import com.bskai.update.UpdateInstaller
import com.bskai.util.Permissions
import com.bskai.workspace.WorkspaceEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun SettingsScreen(
    app: AuraApp,
    onClose: () -> Unit,
    onOpenTerminal: () -> Unit
) {
    val context = LocalContext.current
    val settings by app.settings.settings.collectAsState()
    val scope = rememberCoroutineScope()

    var showUpdateDialog by remember { mutableStateOf(false) }
    var showLocalModelDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showCustomModelDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "关闭")
                }
                Spacer(Modifier.width(8.dp))
                Text("设置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                SettingsSection(title = "AI 配置") {
                    SettingsItem(
                        icon = Icons.Default.Tune,
                        title = "模型选择",
                        subtitle = if (settings.modelSource == "local") "本地: ${settings.apiModel}" else settings.apiModel,
                        onClick = { showLocalModelDialog = true }
                    )
                    SettingsItem(
                        icon = Icons.Default.Add,
                        title = "自定义模型",
                        subtitle = "管理自定义模型列表",
                        onClick = { showCustomModelDialog = true }
                    )
                    SettingsItem(
                        icon = Icons.Default.SwapHoriz,
                        title = "思考模式",
                        subtitle = if (settings.chatMode == ChatMode.DEV) "应用开发模式" else "深度 ${settings.thinkingLevel}/3",
                        onClick = {
                            val next = if (settings.chatMode == ChatMode.THINK) ChatMode.DEV else ChatMode.THINK
                            app.settings.update { it.copy(chatMode = next) }
                        }
                    )
                }
            }

            item {
                SettingsSection(title = "外观") {
                    SettingsItem(
                        icon = Icons.Default.Palette,
                        title = "主题风格",
                        subtitle = settings.themeStyle.label,
                        onClick = { showThemeDialog = true }
                    )
                    SettingsItem(
                        icon = Icons.Default.Notifications,
                        title = "语言",
                        subtitle = settings.selectedLanguage.uppercase(),
                        onClick = { showLanguageDialog = true }
                    )
                }
            }

            item {
                SettingsSection(title = "工具") {
                    SettingsItem(
                        icon = Icons.Default.Terminal,
                        title = "内置终端",
                        subtitle = "打开终端执行命令",
                        onClick = onOpenTerminal
                    )
                    SettingsItem(
                        icon = Icons.Default.Folder,
                        title = "工作区",
                        subtitle = if (settings.workspaceEnabled) "已启用" else "已禁用",
                        onClick = { app.settings.update { it.copy(workspaceEnabled = !it.workspaceEnabled) } }
                    )
                    SettingsItem(
                        icon = Icons.Default.Security,
                        title = "AI 工具调用",
                        subtitle = if (settings.agentToolsEnabled) "已启用" else "已禁用",
                        onClick = { app.settings.update { it.copy(agentToolsEnabled = !it.agentToolsEnabled) } }
                    )
                }
            }

            item {
                SettingsSection(title = "更新") {
                    SettingsItem(
                        icon = Icons.Default.SystemUpdateAlt,
                        title = "检查更新",
                        subtitle = "当前: ${BuildConfig.APP_VERSION}",
                        onClick = { showUpdateDialog = true }
                    )
                }
            }

            item {
                SettingsSection(title = "关于") {
                    SettingsItem(
                        icon = Icons.Default.Info,
                        title = "关于 AURA",
                        subtitle = "版本 ${BuildConfig.APP_VERSION}",
                        onClick = { showAboutDialog = true }
                    )
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "AURA ${BuildConfig.APP_VERSION}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                )
            }
        }
    }

    if (showUpdateDialog) {
        UpdateCenterDialog(
            onDismiss = { showUpdateDialog = false }
        )
    }

    if (showLocalModelDialog) {
        LocalModelDownloadDialog(
            app = app,
            onDismiss = { showLocalModelDialog = false }
        )
    }

    if (showAboutDialog) {
        AboutAuraDialog(onDismiss = { showAboutDialog = false })
    }

    if (showCustomModelDialog) {
        CustomModelManagerDialog(
            app = app,
            onDismiss = { showCustomModelDialog = false }
        )
    }

    if (showLanguageDialog) {
        LanguageSelectDialog(
            current = settings.selectedLanguage,
            onSelect = { code ->
                app.settings.update { it.copy(selectedLanguage = code) }
                app.applyLocale()
            },
            onDismiss = { showLanguageDialog = false }
        )
    }

    if (showThemeDialog) {
        ThemeSelectDialog(
            current = settings.themeStyle,
            onSelect = { style -> app.settings.update { it.copy(themeStyle = style) } },
            onDismiss = { showThemeDialog = false }
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.01f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Medium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
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
            withContext(Dispatchers.IO) { releases = GitHubApi.listReleases() }
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

data class ModelDownloadInfo(val name: String, val progress: Float, val speed: String)

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
                            value = providerUrl, onValueChange = { providerUrl = it },
                            label = { Text("API 地址") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = providerKey, onValueChange = { providerKey = it },
                            label = { Text("API Key (可选)") }, singleLine = true, modifier = Modifier.fillMaxWidth()
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
                            Text(text = errorMessage!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    if (isLoadingModels) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
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
                                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
                                                val targetFile = File(context.cacheDir, "models/${modelInfo.name}.bin")
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
                                    LinearProgressIndicator(progress = { downloadProgress }, modifier = Modifier.fillMaxWidth().height(4.dp))
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

@Composable
fun CustomModelManagerDialog(app: AuraApp, onDismiss: () -> Unit) {
    val settings by app.settings.settings.collectAsState()
    var newModel by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("自定义模型", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn {
                item {
                    OutlinedTextField(
                        value = newModel, onValueChange = { newModel = it },
                        label = { Text("添加模型") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (newModel.isNotBlank()) {
                                app.settings.update { it.copy(customModelList = it.customModelList + newModel.trim()) }
                                newModel = ""
                            }
                        },
                        enabled = newModel.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("添加") }
                    Spacer(Modifier.height(12.dp))
                    Text("已添加模型:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                }
                items(settings.customModelList) { model ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(model, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                        IconButton(onClick = {
                            app.settings.update { it.copy(customModelList = it.customModelList - model) }
                        }) {
                            Icon(Icons.Default.Close, "移除", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}

@Composable
fun ThemeSelectDialog(current: ThemeStyle, onSelect: (ThemeStyle) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("主题风格", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                ThemeStyle.entries.forEach { style ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            .clickable { onSelect(style) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (current == style) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(style.label, fontWeight = FontWeight.Medium)
                                Text(style.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (current == style) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
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
fun LanguageSelectDialog(current: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val languages = remember { com.bskai.data.loadLanguages(context) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择语言", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn {
                items(languages) { lang ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                            .clickable { onSelect(lang.code) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (current == lang.code) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(lang.nativeName.ifBlank { lang.name }, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            Text(lang.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (current == lang.code) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
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

private fun formatSize(bytes: Long): String {
    if (bytes <= 0L) return "?"
    val mb = bytes.toDouble() / (1024 * 1024)
    return "%.2f MB".format(mb)
}

private fun formatSpeed(bytesRead: Long): String {
    if (bytesRead < 1024) return "$bytesRead B/s"
    if (bytesRead < 1024 * 1024) return "${bytesRead / 1024} KB/s"
    return "%.1f MB/s".format(bytesRead / (1024.0 * 1024.0))
}
