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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Http
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RecentActors
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.bskai.toolkit.ProjectConfig
import com.bskai.toolkit.ProjectScaffold
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

    if (showNewProject) {
        NewProjectSheet(app) { result ->
            projectResult = result
            showNewProject = false
        }
        return
    }

    val tools = listOf(
        ToolItem("新建 Android 项目", "创建 Java Android 项目骨架，可直接构建 APK", Icons.Outlined.Android, MaterialTheme.colorScheme.primary) {
            showNewProject = true
        },
        ToolItem("APK 分析器", "读取 APK 包名、版本、权限、签名等信息", Icons.Outlined.Code, MaterialTheme.colorScheme.secondary) {
            /* TODO: open file picker for APK */
        },
        ToolItem("APK 签名", "使用 keystore 签名 APK 文件", Icons.Outlined.Key, MaterialTheme.colorScheme.tertiary) {
            /* TODO: open keystore */
        },
        ToolItem("项目导出", "将工作区项目复制到公共下载目录", Icons.Outlined.Download, BskCyan) {
            /* export project */
        },
        ToolItem("构建历史", "查看最近构建的 APK 和结果", Icons.Outlined.RecentActors, BskRose) {
            /* show history */
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
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(tools) { tool ->
                ToolCard(tool) { tool.action() }
            }
            item { Spacer(Modifier.height(16.dp)) }
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
