package com.bskai.ui.settings

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
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
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
import com.bskai.terminal.DevTools
import com.bskai.terminal.TerminalEngine
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
    app: AuraApp
) {
    val settings by app.settings.settings.collectAsState()
    val scope = rememberCoroutineScope()

    var showUpdateDialog by remember { mutableStateOf(false) }
    var showLocalModelDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showCustomModelDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showDevToolsDialog by remember { mutableStateOf(false) }
    var showWorkspaceDialog by remember { mutableStateOf(false) }

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
                    icon = Icons.Default.Folder,
                    title = "工作区",
                    subtitle = if (settings.workspaceEnabled) "已启用" else "已禁用",
                    onClick = { showWorkspaceDialog = true }
                )
                SettingsItem(
                    icon = Icons.Default.Security,
                    title = "AI 工具调用",
                    subtitle = if (settings.agentToolsEnabled) "已启用" else "已禁用",
                    onClick = { app.settings.update { it.copy(agentToolsEnabled = !it.agentToolsEnabled) } }
                )
                SettingsItem(
                    icon = Icons.Default.Terminal,
                    title = "开发工具",
                    subtitle = "管理终端环境中的开发工具",
                    onClick = { showDevToolsDialog = true }
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

    if (showUpdateDialog) {
        UpdateCenterDialog(onDismiss = { showUpdateDialog = false })
    }

    if (showLocalModelDialog) {
        com.bskai.ui.chat.UnifiedModelDialogV2(app = app, onDismiss = { showLocalModelDialog = false })
    }

    if (showAboutDialog) {
        AboutAuraDialog(onDismiss = { showAboutDialog = false })
    }

    if (showCustomModelDialog) {
        CustomModelManagerDialog(app = app, onDismiss = { showCustomModelDialog = false })
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

    if (showDevToolsDialog) {
        DevToolsDialog(
            engine = app.terminal,
            onDismiss = { showDevToolsDialog = false }
        )
    }

    if (showWorkspaceDialog) {
        WorkspaceManageDialog(
            app = app,
            onDismiss = { showWorkspaceDialog = false }
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
fun WorkspaceManageDialog(app: AuraApp, onDismiss: () -> Unit) {
    val workspaces = app.workspace.workspaces.collectAsState().value
    val activeId = app.workspace.activeId.collectAsState().value
    val scope = rememberCoroutineScope()
    var showNewDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("工作区管理", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("工作区列表", style = MaterialTheme.typography.labelLarge)
                    IconButton(onClick = { showNewDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "新建")
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (workspaces.isEmpty()) {
                    Text("暂无工作区", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn {
                        items(workspaces) { ws ->
                            Surface(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                    .clickable { app.workspace.setActive(ws.id) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (ws.id == activeId) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Folder,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = if (ws.id == activeId) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(ws.name, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                        Text(
                                            if (ws.kind == WorkspaceEntry.Kind.INTERNAL) "内部" else "外部",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                    }
                                    if (ws.id == activeId) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("完成") }
        }
    )

    if (showNewDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewDialog = false },
            title = { Text("新建工作区") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("工作区名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            val id = name.lowercase().replace(" ", "_") + "_" + System.currentTimeMillis()
                            app.workspace.createInternal(id, name)
                            showNewDialog = false
                        }
                    },
                    enabled = name.isNotBlank()
                ) {
                    Text("创建")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewDialog = false }) { Text("取消") }
            }
        )
    }
}
