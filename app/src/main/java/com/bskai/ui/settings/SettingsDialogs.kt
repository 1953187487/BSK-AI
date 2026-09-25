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
import androidx.compose.ui.graphics.Color
import androidx.annotation.StringRes
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bskai.AuraApp
import com.bskai.BuildConfig
import com.bskai.R
import com.bskai.data.Agreements
import com.bskai.data.DefaultModelPresets
import com.bskai.data.ThemeStyle
import com.bskai.data.loadLanguages
import com.bskai.terminal.DevTools
import com.bskai.terminal.TerminalEngine
import com.bskai.update.UpdateCheckResult
import kotlinx.coroutines.launch
import com.bskai.ui.devToolCategoryLabelRes
import com.bskai.ui.devToolDescRes
import com.bskai.ui.devToolNameRes
import com.bskai.ui.themeDescRes
import com.bskai.ui.themeLabelRes

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
            title = { Text(stringResource(R.string.update_check)) },
            text = {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
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
fun AboutAuraDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.settings_about_aura), fontWeight = FontWeight.SemiBold)
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
                    stringResource(R.string.about_description),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "github.com/1953187487/BSK-AI",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.about_feedback),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    Agreements.renderUserNotice(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_close)) }
        }
    )
}

@Composable
fun CustomModelManagerDialog(app: AuraApp, onDismiss: () -> Unit) {
    val settings by app.settings.settings.collectAsState()
    var newModel by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.custom_model_title), fontWeight = FontWeight.SemiBold) },
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
                        label = { Text(stringResource(R.string.custom_model_name)) },
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
                        Icon(Icons.Default.CheckCircle, contentDescription = stringResource(R.string.common_add))
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (settings.customModelList.isEmpty()) {
                    Text(stringResource(R.string.custom_model_none), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.common_delete), tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.custom_model_presets), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
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
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_done)) }
        }
    )
}

@Composable
fun LanguageSelectDialog(current: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val languages = remember { loadLanguages(context) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_language), fontWeight = FontWeight.SemiBold) },
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
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}

@Composable
fun ThemeSelectDialog(current: ThemeStyle, onSelect: (ThemeStyle) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_theme_select), fontWeight = FontWeight.SemiBold) },
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
                                Text(stringResource(themeLabelRes(style)), fontWeight = FontWeight.Medium)
                                Text(
                                    stringResource(themeDescRes(style)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (current == style) {
                                Icon(
                                    Icons.Default.CheckCircle,
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
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
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
    var selectedCategory by remember { mutableStateOf("全部") }

    val categories = listOf("全部", "基础", "Android", "语言", "编译", "网络", "编辑")
    val filteredTools = if (selectedCategory == "全部") DevTools.commonTools
    else DevTools.commonTools.filter { it.category == selectedCategory }

    val allLabel = stringResource(R.string.devtools_all)
    val installingLabel = stringResource(R.string.devtools_installing)
    val installingTitle = stringResource(R.string.devtools_install_now)
    val localUnsupported = stringResource(R.string.devtools_local_unsupported)

    LaunchedEffect(Unit) {
        loading = true
        toolStatus = DevTools.checkAll(engine)
        loading = false
    }

    val currentBackend = engine.backend.value.name.lowercase()

    AlertDialog(
        onDismissRequest = { if (installing == null) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Terminal, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.devtools_title), fontWeight = FontWeight.SemiBold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (currentBackend == "local") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            stringResource(R.string.devtools_local_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (installing != null) {
                    Text(stringResource(R.string.devtools_install_now, installing.orEmpty()), fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0D1117)
                    ) {
                        Text(
                            installOutput.ifEmpty { installingLabel },
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFE6EDF3),
                            modifier = Modifier.padding(8.dp).heightIn(max = 100.dp)
                        )
                    }
                } else if (loading) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        categories.forEach { cat ->
                            Surface(
                                modifier = Modifier.clickable { selectedCategory = cat },
                                shape = RoundedCornerShape(16.dp),
                                color = if (selectedCategory == cat) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ) {
                                Text(
                                    stringResource(devToolCategoryLabelRes(cat)),
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.devtools_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    if (currentBackend != "local") {
                        Button(
                            onClick = {
                                installing = allLabel
                                installOutput = ""
                                scope.launch {
                                    val target = currentBackend
                                    for (tool in DevTools.commonTools) {
                                        val cmds = DevTools.getInstallCommand(tool, target)
                                        for (cmd in cmds) {
                                            val r = engine.execute(cmd)
                                            installOutput += r.stdout + "\n" + r.stderr
                                            if (r.exitCode != 0) break
                                        }
                                    }
                                    installing = null
                                    toolStatus = DevTools.checkAll(engine)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.devtools_install_all))
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(filteredTools) { tool ->
                            val installed = toolStatus[tool.command] == true
                            val toolLabel = stringResource(devToolNameRes(tool))
                            val toolDesc = stringResource(devToolDescRes(tool))
                            val catLabel = stringResource(devToolCategoryLabelRes(tool.category))
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
                                        Text(
                                            stringResource(R.string.devtools_tool_name, toolLabel, tool.command),
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            "$toolDesc · $catLabel",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 10.sp
                                        )
                                    }
                                    if (installed) {
                                        OutlinedButton(onClick = { }, enabled = false) {
                                            Text(stringResource(R.string.common_installed), fontSize = 10.sp)
                                        }
                                    } else {
                                        OutlinedButton(
                                            enabled = currentBackend != "local",
                                            onClick = {
                                                installing = toolLabel
                                                installOutput = ""
                                                scope.launch {
                                                    val target = currentBackend
                                                    val cmds = DevTools.getInstallCommand(tool, target)
                                                    if (cmds.isEmpty()) {
                                                        installOutput = localUnsupported
                                                    }
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
                                            Text(stringResource(R.string.devtools_install), fontSize = 10.sp)
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
                            scope.launch {
                                toolStatus = DevTools.checkAll(engine)
                            }
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(stringResource(R.string.common_refresh))
                    }
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_close)) }
                }
            }
        }
    )
}
