package com.bskai.ui.screens.toolchain

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.RecentActors
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bskai.BskApp
import com.bskai.toolkit.ApkInspector
import com.bskai.toolkit.MediaTransfer
import com.bskai.toolkit.ProjectConfig
import com.bskai.toolkit.ProjectScaffold
import com.bskai.toolkit.TermuxBridge
import java.io.File

data class ToolItem(
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val accent: Color,
    val action: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolboxScreen(app: BskApp) {
    var showNewProject by remember { mutableStateOf(false) }
    var projectResult by remember { mutableStateOf<String?>(null) }
    var apkAnalysis by remember { mutableStateOf<String?>(null) }
    var signingResult by remember { mutableStateOf<String?>(null) }
    var exportResult by remember { mutableStateOf<String?>(null) }
    var showHistory by remember { mutableStateOf(false) }

    if (showNewProject) {
        NewProjectSheet(app) { result ->
            projectResult = result
            showNewProject = false
        }
        return
    }

    if (apkAnalysis != null) {
        ApkAnalysisDialog(
            analysis = apkAnalysis!!,
            onDismiss = { apkAnalysis = null }
        )
        return
    }

    if (signingResult != null) {
        SigningResultDialog(
            result = signingResult!!,
            onDismiss = { signingResult = null }
        )
        return
    }

    if (exportResult != null) {
        ExportResultDialog(
            result = exportResult!!,
            onDismiss = { exportResult = null }
        )
        return
    }

    if (showHistory) {
        BuildHistoryScreen(
            app = app,
            onBack = { showHistory = false }
        )
        return
    }

    var showApkAnalyzer by remember { mutableStateOf(false) }
    var showSignApk by remember { mutableStateOf(false) }

    if (showApkAnalyzer) {
        ShowApkAnalyzerDialog(app) { result ->
            showApkAnalyzer = false
            apkAnalysis = result
        }
        return
    }

    if (showSignApk) {
        ShowSignApkDialog(app) { result ->
            showSignApk = false
            signingResult = result
        }
        return
    }

    val tools = listOf(
        ToolItem("新建 Android 项目", "创建 Java Android 项目骨架，可直接构建 APK", Icons.Outlined.Android, MaterialTheme.colorScheme.primary) {
            showNewProject = true
        },
        ToolItem("APK 分析器", "读取 APK 包名、版本、权限、签名等信息", Icons.Outlined.Code, MaterialTheme.colorScheme.secondary) {
            showApkAnalyzer = true
        },
        ToolItem("APK 签名", "使用 keystore 签名 APK 文件", Icons.Outlined.Key, MaterialTheme.colorScheme.tertiary) {
            showSignApk = true
        },
        ToolItem("项目导出", "将工作区项目复制到公共下载目录", Icons.Outlined.Download, BskCyan) {
            val projects = app.getExternalFilesDir(null)?.let { File(it, "projects") }?.listFiles()?.filter { f -> f.isDirectory } ?: emptyList()
            if (projects.isEmpty()) {
                exportResult = "没有可导出的项目"
            } else {
                val lastProject = projects.lastOrNull()
                if (lastProject != null) {
                    val copied = MediaTransfer.copyProjectToPublic(app, lastProject.name)
                    exportResult = if (copied != null) "已导出: ${copied.absolutePath}" else "导出失败"
                } else {
                    exportResult = "请选择项目"
                }
            }
        },
        ToolItem("构建历史", "查看最近构建的 APK 和结果", Icons.Outlined.RecentActors, BskRose) {
            showHistory = true
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("工具箱", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp)
        ) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(tools) { tool ->
                    ToolCard(tool) { tool.action() }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
            projectResult?.let {
                Spacer(Modifier.height(12.dp))
                Text("成功创建项目: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                projectResult = null
            }
        }
    }
}

@Composable
private fun ShowApkAnalyzerDialog(app: BskApp, onAnalyzed: (String) -> Unit) {
    var apkPath by remember { mutableStateOf("") }
    val defaultPaths = listOf(
        "/sdcard/Download/",
        "${MediaTransfer.publicProjectRoot()}/",
        "${app.getExternalFilesDir(null)}/projects/"
    )

    AlertDialog(
        onDismissRequest = { onAnalyzed("") },
        title = { Text("APK 分析器") },
        text = {
            Column {
                Text("输入 APK 文件的完整路径:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = apkPath,
                    onValueChange = { apkPath = it },
                    label = { Text("APK 路径") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("如 /sdcard/Download/app.apk") }
                )
                Spacer(Modifier.height(8.dp))
                Row {
                    defaultPaths.forEach { path ->
                        TextButton(onClick = { apkPath = path }) {
                            Text(path, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (apkPath.isBlank()) return@Button
                    val file = File(apkPath)
                    if (!file.exists()) {
                        onAnalyzed("APK 文件不存在: $apkPath")
                        return@Button
                    }
                    val result = runCatching {
                        val info = ApkInspector.inspect(app, file).getOrNull()
                            ?: throw Exception("无法解析 APK")
                        buildString {
                            appendLine("应用: ${info.applicationLabel}")
                            appendLine("包名: ${info.packageName}")
                            appendLine("版本: ${info.versionName} (${info.versionCode})")
                            appendLine("Min SDK: ${info.minSdk}")
                            appendLine("Target SDK: ${info.targetSdk}")
                            if (info.permissions.isNotEmpty()) {
                                appendLine("权限 (${info.permissions.size}):")
                                info.permissions.take(10).forEach { appendLine("  - $it") }
                                if (info.permissions.size > 10) appendLine("  ... 还有 ${info.permissions.size - 10} 个权限")
                            }
                            if (info.signatures.isNotEmpty()) {
                                appendLine("签名 SHA-256: ${info.signatures.firstOrNull()?.take(40)}...")
                            }
                        }.trimEnd()
                    }.getOrElse { "分析失败: ${it.message}" }
                    onAnalyzed(result)
                },
                enabled = apkPath.isNotBlank()
            ) {
                Text("分析")
            }
        },
        dismissButton = {
            TextButton(onClick = { onAnalyzed("") }) { Text("取消") }
        }
    )
}

@Composable
private fun ApkAnalysisDialog(analysis: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("APK 分析结果") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(
                    analysis,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("关闭") }
        }
    )
}

@Composable
private fun ShowSignApkDialog(app: BskApp, onSigned: (String) -> Unit) {
    var apkPath by remember { mutableStateOf("") }
    var keystorePath by remember { mutableStateOf("") }
    var alias by remember { mutableStateOf("bsk") }
    var storePassword by remember { mutableStateOf("bsk2026") }
    var keyPassword by remember { mutableStateOf("bsk2026") }

    if (!TermuxBridge.isAvailable(app)) {
        AlertDialog(
            onDismissRequest = { onSigned("请先安装 Termux") },
            title = { Text("Termux 未安装") },
            text = { Text("APK 签名需要 Termux 环境。请从 F-Droid 安装 Termux 并允许外部应用执行命令。") },
            confirmButton = { Button(onClick = { onSigned("请先安装 Termux") }) { Text("明白") } }
        )
        return
    }

    AlertDialog(
        onDismissRequest = { onSigned("") },
        title = { Text("APK 签名") },
        text = {
            Column {
                OutlinedTextField(
                    value = apkPath,
                    onValueChange = { apkPath = it },
                    label = { Text("APK 路径") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("如 /sdcard/Download/app.apk") }
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = keystorePath,
                    onValueChange = { keystorePath = it },
                    label = { Text("Keystore 路径 (可选)") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("留空则使用默认 keystore") }
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = alias,
                    onValueChange = { alias = it },
                    label = { Text("别名") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = storePassword,
                    onValueChange = { storePassword = it },
                    label = { Text("Keystore 密码") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = keyPassword,
                    onValueChange = { keyPassword = it },
                    label = { Text("密钥密码") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (apkPath.isBlank()) return@Button
                    val file = File(apkPath)
                    if (!file.exists()) {
                        onSigned("APK 文件不存在: $apkPath")
                        return@Button
                    }
                    val ksArg = if (keystorePath.isBlank()) {
                        val defaultKs = File(app.filesDir, "bsk-release.keystore")
                        if (!defaultKs.exists()) {
                            onSigned("未找到默认 keystore，请先在设置中配置签名信息")
                            return@Button
                        }
                        "--ks ${defaultKs.absolutePath} --ks-pass pass:$storePassword --key-pass pass:$keyPassword"
                    } else {
                        "--ks $keystorePath --ks-pass pass:$storePassword --key-pass pass:$keyPassword"
                    }
                    val command = "apksigner sign $ksArg --out \"${file.absolutePath}.signed.apk\" \"$apkPath\""
                    val launched = TermuxBridge.runCommand(app, command)
                    onSigned(if (launched) "签名命令已提交到 Termux，请在 Termux 中查看结果" else "无法启动 Termux")
                },
                enabled = apkPath.isNotBlank()
            ) {
                Text("签名")
            }
        },
        dismissButton = {
            TextButton(onClick = { onSigned("") }) { Text("取消") }
        }
    )
}

@Composable
private fun SigningResultDialog(result: String, onDismiss: () -> Unit) {
    val isSuccess = result.contains("成功") || result.contains("已提交")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isSuccess) "签名完成" else "签名失败") },
        text = { Text(result) },
        confirmButton = { Button(onClick = onDismiss) { Text("关闭") } },
        dismissButton = {
            if (!isSuccess) {
                TextButton(onClick = onDismiss) { Text("重试") }
            }
        }
    )
}

@Composable
private fun ExportResultDialog(result: String, onDismiss: () -> Unit) {
    val isSuccess = result.contains("已导出")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isSuccess) "导出完成" else "导出失败") },
        text = { Text(result) },
        confirmButton = { Button(onClick = onDismiss) { Text("关闭") } }
    )
}

@Composable
private fun BuildHistoryScreen(app: BskApp, onBack: () -> Unit) {
    val projects = remember {
        app.getExternalFilesDir(null)?.let { File(it, "projects") }?.listFiles()?.filter { f -> f.isDirectory }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }
    val publicRoot = remember { MediaTransfer.publicProjectRoot() }
    val publicProjects = remember {
        if (publicRoot.exists()) publicRoot.listFiles()?.filter { f -> f.isDirectory }?.sortedByDescending { it.lastModified() } ?: emptyList()
        else emptyList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("构建历史") },
                navigationIcon = {
                    androidx.compose.material.icons.Icons
                    androidx.compose.material3.IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text("本地项目", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
            }
            if (projects.isEmpty()) {
                item {
                    Text("还没有项目。在工具箱中创建新项目名称。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                }
            } else {
                items(projects.take(10)) { dir ->
                    ProjectHistoryCard(dir)
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
            item {
                Text("公共目录", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
            }
            if (publicProjects.isEmpty()) {
                item {
                    Text("公共目录为空。导出项目后会显示在这里。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                }
            } else {
                items(publicProjects.take(10)) { dir ->
                    PublicProjectCard(dir)
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun ProjectHistoryCard(dir: File) {
    val buildExists = dir.listFiles()?.any { it.name.startsWith("build") } == true
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Android, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.width(24.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(dir.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "最后修改: ${android.text.format.DateFormat.format("yyyy-MM-dd HH:mm:ss", dir.lastModified())}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (buildExists) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
private fun PublicProjectCard(dir: File) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Download, contentDescription = null, tint = BskCyan, modifier = Modifier.width(24.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(dir.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    dir.absolutePath,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ToolCard(tool: ToolItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(44.dp)
                    .background(tool.accent.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(tool.icon, contentDescription = null, tint = tool.accent, modifier = Modifier.width(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(tool.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(tool.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Outlined.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun NewProjectSheet(app: BskApp, onCreated: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var packageName by remember { mutableStateOf("") }
    var appLabel by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }

    androidx.compose.foundation.layout.Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("新建 Android 项目", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        androidx.compose.material3.OutlinedTextField(
            value = name,
            onValueChange = { name = it.lowercase().filter { c -> c.isLetterOrDigit() || c == '_' } },
            label = { Text("项目名（英文）") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        androidx.compose.material3.OutlinedTextField(
            value = packageName,
            onValueChange = { packageName = it },
            label = { Text("包名（如 com.example.myapp）") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        androidx.compose.material3.OutlinedTextField(
            value = appLabel,
            onValueChange = { appLabel = it },
            label = { Text("应用显示名") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(16.dp))

        if (creating) {
            CircularProgressIndicator(modifier = Modifier.width(32.dp))
        } else {
            androidx.compose.material3.Button(
                onClick = {
                    if (name.isBlank() || packageName.isBlank() || appLabel.isBlank()) return@Button
                    creating = true
                    val config = ProjectConfig(name = name, packageName = packageName, appLabel = appLabel)
                    val root = ProjectScaffold.create(app, config)
                    if (root != null) {
                        creating = false
                        onCreated(root.absolutePath)
                    } else {
                        creating = false
                    }
                },
                enabled = name.isNotBlank() && packageName.isNotBlank() && appLabel.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("创建项目")
            }
        }
        Spacer(Modifier.height(12.dp))
        androidx.compose.material3.TextButton(onClick = { creating = false }) {
            Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private val BskCyan = Color(0xFF22D3EE)
private val BskRose = Color(0xFFF43F5E)
