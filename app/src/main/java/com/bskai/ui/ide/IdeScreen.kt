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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bskai.AuraApp
import com.bskai.terminal.AndroidDependencyManager
import com.bskai.workspace.WorkspaceEntry
import com.bskai.workspace.WorkspaceManager
import com.bskai.workspace.WorkspaceNode
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdeScreen(app: AuraApp) {
    val settings by app.settings.settings.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var currentProject by remember { mutableStateOf<WorkspaceEntry?>(null) }
    var projectFiles by remember { mutableStateOf<List<WorkspaceNode>>(emptyList()) }
    var selectedFile by remember { mutableStateOf<String?>(null) }
    var fileContent by remember { mutableStateOf("") }
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

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("AURA IDE", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(
                                currentProject?.name ?: "未选择项目",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showNewProjectDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "新建项目")
                    }
                    IconButton(onClick = { showDependencyDialog = true }) {
                        Icon(Icons.Default.Download, contentDescription = "依赖管理")
                    }
                    IconButton(onClick = {
                        if (currentProject != null) {
                            scope.launch {
                                projectFiles = app.workspace.listRoot()
                            }
                        }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )

            when (currentTab) {
                0 -> IdeFileBrowserTab(
                    app = app,
                    projectFiles = projectFiles,
                    onFileSelected = { node ->
                        selectedFile = node.path
                        if (!node.isDirectory) {
                            scope.launch {
                                val content = app.workspace.readRelative(node.path)
                                fileContent = content ?: ""
                            }
                        }
                    },
                    fileContent = fileContent,
                    onFileContentChange = { fileContent = it },
                    onFileSave = { path, content ->
                        scope.launch {
                            app.workspace.writeRelative(path, content)
                            projectFiles = app.workspace.listRoot()
                        }
                    }
                )
                1 -> IdeOutputTab(outputLog = outputLog)
            }
        }

        // Floating Build Button - Top Right Corner
        FloatingActionButton(
            onClick = {
                scope.launch {
                    isBuilding = true
                    outputLog = ""
                    val projectDir = app.workspace.active?.let { ws ->
                        if (ws.kind == WorkspaceEntry.Kind.INTERNAL) {
                            File(context.filesDir, "workspaces/${ws.id}").absolutePath
                        } else null
                    }
                    if (projectDir != null) {
                        outputLog += "开始构建...\n"
                        val result = app.terminal.execute("cd $projectDir && ./gradlew assembleDebug 2>&1")
                        outputLog += result.stdout + "\n" + result.stderr
                        outputLog += "\n构建完成，退出码: ${result.exitCode}\n"
                    } else {
                        outputLog = "请先选择或创建一个内部工作区项目。"
                    }
                    isBuilding = false
                    currentTab = 1
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 72.dp, end = 16.dp)
                .size(56.dp),
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            if (isBuilding) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "构建",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
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
    onFileSelected: (WorkspaceNode) -> Unit,
    fileContent: String,
    onFileContentChange: (String) -> Unit,
    onFileSave: (String, String) -> Unit
) {
    Row(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(0.4f).fillMaxSize().padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(projectFiles) { node ->
                Surface(
                    modifier = Modifier.fillMaxWidth()
                        .clickable { onFileSelected(node) },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (node.isDirectory) Icons.Default.Folder else Icons.Default.Code,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (node.isDirectory) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            node.name,
                            modifier = Modifier.weight(1f),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.weight(0.6f).fillMaxSize().padding(8.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                if (fileContent.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("编辑器", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Button(onClick = {
                            // Save handled by parent
                        }) {
                            Text("保存")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = fileContent,
                        onValueChange = onFileContentChange,
                        modifier = Modifier.fillMaxSize(),
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "从左侧选择文件进行编辑",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IdeOutputTab(outputLog: String) {
    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Text("构建输出", fontWeight = FontWeight.Medium, fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF0D1117)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                contentPadding = PaddingValues(8.dp)
            ) {
                item {
                    Text(
                        outputLog.ifEmpty { "暂无输出" },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFFE6EDF3),
                        lineHeight = 16.sp
                    )
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
        title = { Text("新建项目") },
        text = {
            Column {
                Text("项目名称", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("项目名") },
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
                Text("创建")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun IdeDependencyDialog(app: AuraApp, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var installing by remember { mutableStateOf(false) }
    var installOutput by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("全部") }

    val categories = listOf("全部", "基础", "Android", "语言", "编译", "网络", "编辑")
    val filteredDeps = if (selectedCategory == "全部") AndroidDependencyManager.allDependencies
    else AndroidDependencyManager.allDependencies.filter { it.category == selectedCategory }

    AlertDialog(
        onDismissRequest = { if (!installing) onDismiss() },
        title = { Text("依赖管理") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
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
                                cat,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))

                if (installing) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0D1117)
                    ) {
                        Text(
                            installOutput.ifEmpty { "安装中..." },
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color(0xFFE6EDF3),
                            modifier = Modifier.padding(8.dp).heightIn(max = 100.dp)
                        )
                    }
                } else {
                    LazyColumn {
                        item {
                            Button(
                                onClick = {
                                    installing = true
                                    installOutput = ""
                                    scope.launch {
                                        val backend = app.terminal.backend.value.name.lowercase()
                                        val cmds = AndroidDependencyManager.getInstallAllCommands(backend)
                                        for (cmd in cmds) {
                                            val r = app.terminal.execute(cmd)
                                            installOutput += r.stdout + "\n" + r.stderr
                                            if (r.exitCode != 0) break
                                        }
                                        installing = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("一键安装 Android 依赖")
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                        items(filteredDeps) { dep ->
                            Surface(
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
                                        Text(dep.name, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                                        Text(dep.description + " · " + dep.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            installing = true
                                            installOutput = ""
                                            scope.launch {
                                                val backend = app.terminal.backend.value.name.lowercase()
                                                val cmds = AndroidDependencyManager.getInstallCommands(dep, backend)
                                                for (cmd in cmds) {
                                                    val r = app.terminal.execute(cmd)
                                                    installOutput += r.stdout + "\n" + r.stderr
                                                    if (r.exitCode != 0) break
                                                }
                                                installing = false
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
        },
        confirmButton = {
            if (!installing) {
                TextButton(onClick = onDismiss) { Text("关闭") }
            }
        }
    )
}
