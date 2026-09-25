package com.bskai.ui.ide

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bskai.AuraApp
import com.bskai.R
import com.bskai.terminal.AndroidDependencyManager
import com.bskai.ui.glass.GlassButton
import com.bskai.ui.glass.GlassChip
import com.bskai.ui.glass.GlassIconButton
import com.bskai.ui.glass.GlassPanel
import com.bskai.ui.glass.GlassSegmented
import com.bskai.ui.glass.rememberGlassColors
import com.bskai.workspace.WorkspaceEntry
import com.bskai.workspace.WorkspaceNode
import kotlinx.coroutines.launch
import java.io.File
import com.bskai.ui.devToolCategoryLabelRes
import com.bskai.ui.devToolDescRes
import com.bskai.ui.devToolNameRes

private val OUT_BG = Color(0xCC0A0E18)
private val OUT_TEXT = Color(0xFFE6EDF3)
private val OUT_MUTED = Color(0xFF8B949E)
private val OUT_GREEN = Color(0xFF3FB950)

@Composable
fun IdeScreen(app: AuraApp) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val glass = rememberGlassColors()
    val buildStart = stringResource(R.string.ide_build_start)
    val buildDone = stringResource(R.string.ide_build_done)
    val buildNoWorkspace = stringResource(R.string.ide_build_no_workspace)

    var currentProject by remember { mutableStateOf<WorkspaceEntry?>(null) }
    var projectFiles by remember { mutableStateOf<List<WorkspaceNode>>(emptyList()) }
    var selectedFile by remember { mutableStateOf<String?>(null) }
    var fileContent by remember { mutableStateOf("") }
    var fileDirty by remember { mutableStateOf(false) }
    var outputLog by remember { mutableStateOf("") }
    var isBuilding by remember { mutableStateOf(false) }
    var showNewProjectDialog by remember { mutableStateOf(false) }
    var showDependencyDialog by remember { mutableStateOf(false) }
    var currentTab by remember { mutableStateOf(0) }

    val activeWorkspace = app.workspace.active

    LaunchedEffect(activeWorkspace) {
        currentProject = activeWorkspace
        if (activeWorkspace != null) {
            projectFiles = app.workspace.listRoot()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        GlassPanel(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "AURA IDE",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = glass.content
                    )
                    Text(
                        currentProject?.name ?: stringResource(R.string.ide_no_project),
                        fontSize = 11.sp,
                        color = glass.contentMuted
                    )
                }
                GlassIconButton(
                    icon = Icons.Default.Add,
                    contentDescription = stringResource(R.string.ide_new_project),
                    size = 34.dp,
                    onClick = { showNewProjectDialog = true }
                )
                Spacer(Modifier.width(6.dp))
                GlassIconButton(
                    icon = Icons.Default.Download,
                    contentDescription = stringResource(R.string.ide_deps_title),
                    size = 34.dp,
                    onClick = { showDependencyDialog = true }
                )
                Spacer(Modifier.width(6.dp))
                GlassIconButton(
                    icon = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.common_refresh),
                    size = 34.dp,
                    onClick = {
                        if (currentProject != null) {
                            scope.launch { projectFiles = app.workspace.listRoot() }
                        }
                    }
                )
                Spacer(Modifier.width(6.dp))
                Box(contentAlignment = Alignment.Center) {
                    GlassIconButton(
                        icon = if (isBuilding) Icons.Default.Build else Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.ide_build_apk),
                        size = 40.dp,
                        onClick = {
                            if (isBuilding) return@GlassIconButton
                            scope.launch {
                                isBuilding = true
                                outputLog = ""
                                currentTab = 1
                                val projectDir = app.workspace.active?.let { ws ->
                                    if (ws.kind == WorkspaceEntry.Kind.INTERNAL) {
                                        File(context.filesDir, "workspaces/${ws.id}").absolutePath
                                    } else null
                                }
                                if (projectDir != null) {
                                    outputLog += buildStart
                                    val result = app.terminal.execute("cd $projectDir && ./gradlew assembleDebug 2>&1")
                                    outputLog += result.stdout + "\n" + result.stderr
                                    outputLog += "\n" + buildDone.format(result.exitCode) + "\n"
                                } else {
                                    outputLog = buildNoWorkspace
                                }
                                isBuilding = false
                            }
                        }
                    )
                    if (isBuilding) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(40.dp).padding(8.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        GlassSegmented(
            options = listOf(stringResource(R.string.ide_tab_files), stringResource(R.string.ide_tab_output)),
            selected = currentTab,
            onSelect = { currentTab = it },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(8.dp))

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (currentTab) {
                0 -> IdeFileBrowserTab(
                    app = app,
                    projectFiles = projectFiles,
                    selectedFile = selectedFile,
                    onFileSelected = { node ->
                        selectedFile = node.path
                        fileDirty = false
                        if (!node.isDirectory) {
                            scope.launch {
                                fileContent = app.workspace.readRelative(node.path) ?: ""
                            }
                        }
                    },
                    fileContent = fileContent,
                    fileDirty = fileDirty,
                    onFileContentChange = {
                        fileContent = it
                        fileDirty = true
                    },
                    onFileSave = { path, content ->
                        scope.launch {
                            app.workspace.writeRelative(path, content)
                            fileDirty = false
                            projectFiles = app.workspace.listRoot()
                        }
                    }
                )
                else -> IdeOutputTab(outputLog = outputLog)
            }
        }
    }

    if (showNewProjectDialog) {
        IdeNewProjectDialog(
            onDismiss = { showNewProjectDialog = false },
            onCreate = { name ->
                scope.launch {
                    app.workspace.createInternal(name.lowercase().replace(" ", "_"), name)
                    currentProject = app.workspace.active
                    projectFiles = app.workspace.listRoot()
                }
                showNewProjectDialog = false
            }
        )
    }

    if (showDependencyDialog) {
        IdeDependencyDialog(
            app = app,
            onDismiss = { showDependencyDialog = false }
        )
    }
}

@Composable
private fun IdeFileBrowserTab(
    app: AuraApp,
    projectFiles: List<WorkspaceNode>,
    selectedFile: String?,
    onFileSelected: (WorkspaceNode) -> Unit,
    fileContent: String,
    fileDirty: Boolean,
    onFileContentChange: (String) -> Unit,
    onFileSave: (String, String) -> Unit
) {
    val glass = rememberGlassColors()
    Row(modifier = Modifier.fillMaxSize()) {
        GlassPanel(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.weight(0.42f).fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(projectFiles) { node ->
                    val isSelected = selectedFile == node.path
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isSelected) glass.accent.copy(alpha = 0.18f) else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onFileSelected(node) }
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (node.isDirectory) Icons.Default.Folder else Icons.Default.Code,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (node.isDirectory) glass.accent else glass.contentMuted
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            node.name,
                            fontSize = 12.sp,
                            color = glass.content,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.width(8.dp))

        GlassPanel(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.weight(0.58f).fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                if (selectedFile != null && selectedFile!!.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            selectedFile!!.substringAfterLast('/') + if (fileDirty) " •" else "",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = glass.content
                        )
                        GlassButton(
                            text = stringResource(R.string.common_save),
                            onClick = { onFileSave(selectedFile!!, fileContent) }
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    androidx.compose.foundation.text.BasicTextField(
                        value = fileContent,
                        onValueChange = onFileContentChange,
                        modifier = Modifier.fillMaxSize(),
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = glass.content,
                            lineHeight = 16.sp
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(R.string.ide_empty_editor),
                            color = glass.contentMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IdeOutputTab(outputLog: String) {
    GlassPanel(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(listOf(OUT_BG, Color(0xE60A0E18))),
                    RoundedCornerShape(20.dp)
                )
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                Text(
                    stringResource(R.string.ide_build_output),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OUT_GREEN,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        Text(
                            outputLog.ifEmpty { stringResource(R.string.ide_no_output) },
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = if (outputLog.isEmpty()) OUT_MUTED else OUT_TEXT,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IdeNewProjectDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ide_new_project)) },
        text = {
            Column {
                Text(stringResource(R.string.ide_project_name_label), style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.ide_project_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onCreate(name) },
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(R.string.workspace_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}

@Composable
private fun IdeDependencyDialog(app: AuraApp, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var installing by remember { mutableStateOf(false) }
    var installOutput by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("全部") }
    val currentBackend = app.terminal.backend.value.name.lowercase()

    val categories = listOf("全部", "基础", "Android", "语言", "编译", "网络", "编辑")
    val filteredDeps = if (selectedCategory == "全部") AndroidDependencyManager.allDependencies
    else AndroidDependencyManager.allDependencies.filter { it.category == selectedCategory }

    AlertDialog(
        onDismissRequest = { if (!installing) onDismiss() },
        title = { Text(stringResource(R.string.ide_deps_title)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categories.forEach { cat ->
                        GlassChip(
                            text = stringResource(devToolCategoryLabelRes(cat)),
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))

                if (installing) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(OUT_BG, RoundedCornerShape(8.dp))
                    ) {
                        Text(
                            installOutput.ifEmpty { stringResource(R.string.ide_deps_installing) },
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = OUT_TEXT,
                            modifier = Modifier.padding(8.dp).heightIn(max = 100.dp)
                        )
                    }
                } else {
                    if (currentBackend == "local") {
                        Box(
                            modifier = Modifier.fillMaxWidth().background(OUT_BG, RoundedCornerShape(8.dp)).padding(8.dp)
                        ) {
                            Text(
                                stringResource(R.string.devtools_local_hint_long),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = OUT_TEXT,
                                modifier = Modifier.padding(8.dp).heightIn(max = 100.dp)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    LazyColumn {
                        item {
                            Button(
                                onClick = {
                                    installing = true
                                    installOutput = ""
                                    scope.launch {
                                        val target = if (currentBackend == "local") "shizuku" else currentBackend
                                        val cmds = AndroidDependencyManager.getInstallAllCommands(target)
                                        for (cmd in cmds) {
                                            val r = app.terminal.execute(cmd)
                                            installOutput += r.stdout + "\n" + r.stderr
                                            if (r.exitCode != 0) break
                                        }
                                        installing = false
                                    }
                                },
                                enabled = currentBackend != "local",
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.ide_deps_install_all))
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                        items(filteredDeps) { dep ->
                            androidx.compose.material3.Surface(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(stringResource(devToolNameRes(dep)), fontWeight = FontWeight.Medium, fontSize = 12.sp)
                                        Text(
                                            "${stringResource(devToolDescRes(dep))} · ${stringResource(devToolCategoryLabelRes(dep.category))}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 10.sp
                                        )
                                    }
                                    OutlinedButton(
                                        enabled = currentBackend != "local",
                                        onClick = {
                                            installing = true
                                            installOutput = ""
                                            scope.launch {
                                                val target = if (currentBackend == "local") "shizuku" else currentBackend
                                                val cmds = AndroidDependencyManager.getInstallCommands(dep, target)
                                                for (cmd in cmds) {
                                                    val r = app.terminal.execute(cmd)
                                                    installOutput += r.stdout + "\n" + r.stderr
                                                    if (r.exitCode != 0) break
                                                }
                                                installing = false
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
        },
        confirmButton = {
            if (!installing) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_close)) }
            }
        }
    )
}
