package com.bskai.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bskai.AuraApp
import com.bskai.BuildConfig
import com.bskai.data.Agreements
import com.bskai.data.DefaultModelPresets
import com.bskai.data.ThemeStyle
import com.bskai.data.loadLanguages
import com.bskai.terminal.DevTools
import com.bskai.terminal.TerminalEngine
import com.bskai.update.UpdateCheckResult
import kotlinx.coroutines.launch

@Composable
fun UpdateCenterDialog(onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var result by remember { mutableStateOf<UpdateCheckResult?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        loading = true
        val releases = com.bskai.update.GitHubApi.listReleases()
        val latest = releases.firstOrNull()
        val hasUpdate = latest?.let { it.versionCode > BuildConfig.VERSION_CODE } ?: false
        result = UpdateCheckResult(releases = releases, latestRelease = latest, hasUpdate = hasUpdate)
        loading = false
    }

    if (loading) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("检查更新") },
            text = {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        )
        return
    }

    result?.let { r ->
        com.bskai.ui.update.CombinedUpdateDialog(
            result = r,
            currentVersionSigned = "signed",
            onAgreementNeeded = { false },
            onAgreementRequested = {},
            onDismiss = onDismiss
        )
    }
}

@Composable
fun LocalModelDownloadDialog(app: AuraApp, onDismiss: () -> Unit) {
    com.bskai.ui.chat.UnifiedModelDialog(app = app, onDismiss = onDismiss)
}

@Composable
fun AboutAuraDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("关于 AURA", fontWeight = FontWeight.SemiBold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("AURA ${BuildConfig.APP_VERSION}", fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Build ${BuildConfig.BUILD_NUMBER}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "AURA 是一款集成 AI 对话、终端、音乐播放的 Android 应用。",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "开源地址: github.com/1953187487/BSK-AI",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "反馈邮箱: 1953187487@qq.com",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    Agreements.renderPrivacy(BuildConfig.APP_VERSION),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

@Composable
fun CustomModelManagerDialog(app: AuraApp, onDismiss: () -> Unit) {
    val settings by app.settings.settings.collectAsState()
    val scope = rememberCoroutineScope()
    var newModel by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("自定义模型", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newModel,
                        onValueChange = { newModel = it },
                        label = { Text("模型名称") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            if (newModel.isNotBlank()) {
                                val updated = settings.customModelList + newModel
                                app.settings.update { it.copy(customModelList = updated) }
                                newModel = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "添加")
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (settings.customModelList.isEmpty()) {
                    Text("暂无自定义模型", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        items(settings.customModelList) { model ->
                            val updated = settings.customModelList.filter { it != model }
                            Surface(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(model, modifier = Modifier.weight(1f))
                                    IconButton(
                                        onClick = {
                                            app.settings.update { it.copy(customModelList = updated) }
                                        }
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("预设模型", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                DefaultModelPresets.forEach { model ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                            .clickable {
                                app.settings.update { it.copy(apiModel = model) }
                            },
                        shape = RoundedCornerShape(8.dp),
                        color = if (settings.apiModel == model) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ) {
                        Text(model, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("完成") }
        }
    )
}

@Composable
fun LanguageSelectDialog(current: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val languages = remember { loadLanguages(context) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择语言", fontWeight = FontWeight.SemiBold) },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                items(languages) { lang ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                            .clickable { onSelect(lang.code) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (current == lang.code) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                lang.nativeName,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                lang.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
fun ThemeSelectDialog(current: ThemeStyle, onSelect: (ThemeStyle) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("主题风格", fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                ThemeStyle.entries.forEach { style ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            .clickable { onSelect(style) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (current == style) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(style.label, fontWeight = FontWeight.Medium)
                                Text(
                                    style.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (current == style) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
fun DevToolsDialog(engine: TerminalEngine, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var toolStatus by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }
    var installing by remember { mutableStateOf<String?>(null) }
    var installOutput by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        loading = true
        toolStatus = DevTools.checkAll(engine)
        loading = false
    }

    AlertDialog(
        onDismissRequest = { if (installing == null) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Terminal, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("开发工具", fontWeight = FontWeight.SemiBold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (installing != null) {
                    Text("正在安装: $installing", fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Text(
                            installOutput.ifEmpty { "安装中..." },
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(8.dp).heightIn(max = 100.dp)
                        )
                    }
                } else if (loading) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    Text(
                        "管理终端环境中的开发工具",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(DevTools.commonTools) { tool ->
                            val installed = toolStatus[tool.command] == true
                            Surface(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = if (installed) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        if (installed) Icons.Default.CheckCircle else Icons.Default.Download,
                                        contentDescription = null,
                                        tint = if (installed) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(tool.name + " (" + tool.command + ")", fontWeight = FontWeight.Medium, fontSize = 12.sp)
                                        Text(tool.description + " · " + tool.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                    }
                                    if (!installed) {
                                        OutlinedButton(
                                            onClick = {
                                                installing = tool.name
                                                installOutput = ""
                                                scope.launch {
                                                    val backend = engine.backend.value.name.lowercase()
                                                    val cmds = DevTools.getInstallCommand(tool, backend)
                                                    for (cmd in cmds) {
                                                        val r = engine.execute(cmd)
                                                        installOutput += r.stdout + "\n" + r.stderr
                                                        if (r.exitCode != 0) break
                                                    }
                                                    installing = null
                                                    toolStatus = DevTools.checkAll(engine)
                                                }
                                            }
                                        ) {
                                            Text("安装", fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (installing == null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            loading = true
                            scope.launch {
                                toolStatus = DevTools.checkAll(engine)
                                loading = false
                            }
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("刷新")
                    }
                    TextButton(onClick = onDismiss) { Text("关闭") }
                }
            }
        }
    )
}
