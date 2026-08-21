package com.bskai.ui.screens.toolchain

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.bskai.BskApp
import com.bskai.agent.tools.formatSize
import com.bskai.toolkit.ApkBuilder
import com.bskai.toolkit.ComponentStatus
import com.bskai.toolkit.ToolchainManager
import kotlinx.coroutines.launch

@Composable
fun ToolchainScreen(app: BskApp) {
    val manager = remember { ToolchainManager(app) }
    val apkBuilder = remember { ApkBuilder(app) }
    var components by remember { mutableStateOf(manager.components()) }
    var downloadingJar by remember { mutableStateOf(false) }
    var projects by remember { mutableStateOf(manager.projectRoot().listFiles()?.filter { it.isDirectory }?.map { it.name } ?: emptyList()) }
    var buildResult by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun refresh() {
        components = manager.components()
        projects = manager.projectRoot().listFiles()?.filter { it.isDirectory }?.map { it.name } ?: emptyList()
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Android 开发工具链", style = MaterialTheme.typography.headlineMedium)
        Text(
            "在设备上直接构建 APK：android.jar 由本应用下载，编译与签名工具由 Termux 提供",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(components, key = { it.id }) { comp ->
                ComponentCard(
                    comp = comp,
                    downloading = downloadingJar && comp.id == "android_jar",
                    onAction = {
                        if (comp.id == "android_jar" && !downloadingJar) {
                            downloadingJar = true
                            scope.launch {
                                val ok = apkBuilder.prepareAndroidJar { }
                                downloadingJar = false
                                buildResult = if (ok) "android.jar 下载完成" else "android.jar 下载失败"
                                refresh()
                            }
                        } else if (comp.id == "jdk" || comp.id == "build_tools") {
                            buildResult = "请在 Termux 中执行一键安装脚本，或运行: pkg install openjdk-17 aapt2 d8 apksigner zipalign"
                        } else if (comp.id == "termux") {
                            buildResult = "请安装 Termux（F-Droid），打开后运行 termux-setup-storage"
                        }
                    }
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("我的项目", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        if (projects.isEmpty()) {
            Text(
                "还没有项目。可在「智能体」终端中输入：新建一个 Android 项目并构建",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            projects.forEach { name ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(name, style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Monospace)
                            Text(
                                apkBuilder.projectDir(name).absolutePath,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(onClick = {
                            val err = apkBuilder.build(name)
                            buildResult = err ?: "已提交构建任务到 Termux"
                            refresh()
                        }) {
                            Text("构建 APK")
                        }
                    }
                }
            }
        }
        buildResult?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun ComponentCard(
    comp: com.bskai.toolkit.ToolchainComponent,
    downloading: Boolean,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(comp.name, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(8.dp))
                    StatusBadge(comp.status, downloading)
                }
                Text(
                    comp.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    comp.detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            when {
                downloading -> CircularProgressIndicator(Modifier.width(22.dp).height(22.dp), strokeWidth = 2.dp)
                comp.status == ComponentStatus.READY -> {}
                else -> Button(onClick = onAction) { Text("处理") }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: ComponentStatus, downloading: Boolean) {
    val (text, color) = when {
        downloading -> "下载中" to MaterialTheme.colorScheme.tertiary
        status == ComponentStatus.READY -> "就绪" to MaterialTheme.colorScheme.secondary
        else -> "缺失" to MaterialTheme.colorScheme.error
    }
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = color
    )
}
