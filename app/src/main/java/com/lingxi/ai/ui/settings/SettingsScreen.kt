package com.lingxi.ai.ui.settings

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
import androidx.compose.material.icons.filled.VideoLibrary
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lingxi.ai.LingXiApp
import com.lingxi.ai.BuildConfig
import com.lingxi.ai.R
import com.lingxi.ai.agent.LlmClient
import com.lingxi.ai.data.ChatMode
import com.lingxi.ai.data.DefaultApiUrlPresets
import com.lingxi.ai.data.DefaultModelPresets
import com.lingxi.ai.data.Language
import com.lingxi.ai.data.ThemeStyle
import com.lingxi.ai.data.loadLanguages
import com.lingxi.ai.permission.ShizukuBridge
import com.lingxi.ai.terminal.DevTools
import com.lingxi.ai.terminal.TerminalEngine
import com.lingxi.ai.update.DownloadStatus
import com.lingxi.ai.update.GitHubApi
import com.lingxi.ai.update.RemoteRelease
import com.lingxi.ai.update.UpdateInstaller
import com.lingxi.ai.util.Permissions
import com.lingxi.ai.workspace.WorkspaceEntry
import com.lingxi.ai.ui.glass.GlassPanel
import com.lingxi.ai.ui.glass.rememberGlassColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun SettingsScreen(
    app: LingXiApp
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

    val glass = rememberGlassColors()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            SettingsSection(title = stringResource(R.string.settings_section_ai)) {
                SettingsItem(
                    icon = Icons.Default.Tune,
                    title = stringResource(R.string.settings_model_select),
                    subtitle = if (settings.modelSource == "local")
                        stringResource(R.string.settings_model_local_prefix, settings.apiModel)
                    else settings.apiModel,
                    onClick = { showLocalModelDialog = true }
                )
                SettingsItem(
                    icon = Icons.Default.Add,
                    title = stringResource(R.string.settings_custom_model),
                    subtitle = stringResource(R.string.settings_custom_model_desc),
                    onClick = { showCustomModelDialog = true }
                )
                SettingsItem(
                    icon = Icons.Default.SwapHoriz,
                    title = stringResource(R.string.settings_thinking_mode),
                    subtitle = if (settings.chatMode == ChatMode.DEV)
                        stringResource(R.string.settings_dev_mode)
                    else stringResource(R.string.settings_thinking_level, settings.thinkingLevel),
                    onClick = {
                        val next = if (settings.chatMode == ChatMode.THINK) ChatMode.DEV else ChatMode.THINK
                        app.settings.update { it.copy(chatMode = next) }
                    }
                )
            }
        }

        item {
            SettingsSection(title = stringResource(R.string.settings_section_appearance)) {
                SettingsItem(
                    icon = Icons.Default.Palette,
                    title = stringResource(R.string.settings_theme),
                    subtitle = settings.themeStyle.label,
                    onClick = { showThemeDialog = true }
                )
                SettingsItem(
                    icon = Icons.Default.Notifications,
                    title = stringResource(R.string.settings_language_app),
                    subtitle = settings.selectedLanguage.uppercase(),
                    onClick = { showLanguageDialog = true }
                )
            }
        }

        item {
            SettingsSection(title = stringResource(R.string.settings_section_tools)) {
                SettingsItem(
                    icon = Icons.Default.Folder,
                    title = stringResource(R.string.settings_section_workspace),
                    subtitle = stringResource(
                        if (settings.workspaceEnabled) R.string.settings_enabled else R.string.settings_disabled
                    ),
                    onClick = { showWorkspaceDialog = true }
                )
                SettingsItem(
                    icon = Icons.Default.Security,
                    title = stringResource(R.string.settings_agent_tools_title),
                    subtitle = stringResource(
                        if (settings.agentToolsEnabled) R.string.settings_enabled else R.string.settings_disabled
                    ),
                    onClick = { app.settings.update { it.copy(agentToolsEnabled = !it.agentToolsEnabled) } }
                )
                SettingsItem(
                    icon = Icons.Default.Terminal,
                    title = stringResource(R.string.settings_dev_tools),
                    subtitle = stringResource(R.string.settings_dev_tools_desc),
                    onClick = { showDevToolsDialog = true }
                )
            }
        }

        item {
            SettingsSection(title = stringResource(R.string.settings_section_video)) {
                SettingsItem(
                    icon = Icons.Default.VideoLibrary,
                    title = stringResource(R.string.settings_video_dual),
                    subtitle = if (settings.videoGen.enabled) stringResource(
                        R.string.settings_video_on_desc,
                        settings.videoGen.scriptModel.ifBlank { settings.apiModel.ifEmpty { stringResource(R.string.common_unknown) } },
                        settings.videoGen.videoModel.ifEmpty { stringResource(R.string.common_unknown) }
                    ) else stringResource(R.string.settings_video_off_desc),
                    onClick = { app.settings.update { it.copy(videoGen = it.videoGen.copy(enabled = !it.videoGen.enabled)) } }
                )
            }
        }

        item {
            SettingsSection(title = stringResource(R.string.settings_section_update)) {
                SettingsItem(
                    icon = Icons.Default.SystemUpdateAlt,
                    title = stringResource(R.string.settings_check_update),
                    subtitle = stringResource(R.string.settings_current_version, BuildConfig.APP_VERSION),
                    onClick = { showUpdateDialog = true }
                )
            }
        }

        item {
            SettingsSection(title = stringResource(R.string.settings_section_about)) {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = stringResource(R.string.settings_about_lingxi),
                    subtitle = stringResource(R.string.settings_version_label, BuildConfig.APP_VERSION),
                    onClick = { showAboutDialog = true }
                )
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.about_version_line, BuildConfig.APP_VERSION),
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
        com.lingxi.ai.ui.chat.UnifiedModelDialogV2(app = app, onDismiss = { showLocalModelDialog = false })
    }

    if (showAboutDialog) {
        AboutLingXiDialog(onDismiss = { showAboutDialog = false })
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
    val glass = rememberGlassColors()
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = glass.accent,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        GlassPanel(shape = RoundedCornerShape(18.dp)) {
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
    val glass = rememberGlassColors()
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
            tint = glass.accent,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium, color = glass.content)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = glass.contentMuted
            )
        }
    }
}

@Composable
fun WorkspaceManageDialog(app: LingXiApp, onDismiss: () -> Unit) {
    val workspaces = app.workspace.workspaces.collectAsState().value
    val activeId = app.workspace.activeId.collectAsState().value
    val scope = rememberCoroutineScope()
    var showNewDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.workspace_manage), fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.workspace_list), style = MaterialTheme.typography.labelLarge)
                    IconButton(onClick = { showNewDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.workspace_new))
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (workspaces.isEmpty()) {
                    Text(
                        stringResource(R.string.workspace_none),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                                            stringResource(
                                                if (ws.kind == WorkspaceEntry.Kind.INTERNAL)
                                                    R.string.workspace_kind_internal
                                                else R.string.workspace_kind_external
                                            ),
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
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_done)) }
        }
    )

    if (showNewDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewDialog = false },
            title = { Text(stringResource(R.string.workspace_new_title)) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.workspace_name)) },
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
                    Text(stringResource(R.string.workspace_create))
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewDialog = false }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }
}
