package com.floatai.ui.screens.atk

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import com.floatai.App
import com.floatai.tools.ApkInspector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * ATK 反编译界面 v1.0.6-rc.2：
 *  - 用户上传 APK（SAF GetContent）
 *  - 解析 APK 元数据（包名、版本、权限、Activity、Service、Receiver、Provider）
 *  - 列出 APK 内所有文件（classes.dex / resources.arsc / assets / res）
 *  - 提取 AndroidManifest.xml 原始字节并提示用户用 aapt2 / apktool 进一步反编译
 *  - 一键发送到 AI 聊天（附 APK 元数据）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtkScreen(onBack: () -> Unit, onSendToChat: (String) -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as App
    val scope = rememberCoroutineScope()

    var selectedApkUri by remember { mutableStateOf<Uri?>(null) }
    var selectedApkName by remember { mutableStateOf<String?>(null) }
    var metadata by remember { mutableStateOf<ApkInspector.ApkMetadata?>(null) }
    var entries by remember { mutableStateOf<List<ApkInspector.ApkEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedApkUri = uri
            selectedApkName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx) else null
                } else null
            } ?: uri.lastPathSegment
            loading = true
            error = null
            scope.launch {
                try {
                    // 复制 APK 到 cache
                    val cacheFile = File(context.cacheDir, "atk_${System.currentTimeMillis()}.apk")
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            cacheFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                    val meta = withContext(Dispatchers.IO) { ApkInspector.readMetadata(context, cacheFile) }
                    val ents = withContext(Dispatchers.IO) { ApkInspector.listEntries(cacheFile) }
                    metadata = meta
                    entries = ents
                    loading = false
                } catch (e: Exception) {
                    error = "解析失败：${e.message}"
                    loading = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ATK 反编译") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 上传卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.UploadFile,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.size(10.dp))
                        Text(
                            "上传 APK 反编译",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "支持任意 .apk 文件（不限 AndroidManifest 加密类型）。" +
                            "本工具纯本地解析，不上传任何数据。",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { picker.launch("application/vnd.android.package-archive") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.FolderOpen, contentDescription = null)
                        Spacer(Modifier.size(6.dp))
                        Text(if (selectedApkName == null) "选择 APK 文件" else "已选：$selectedApkName")
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            error?.let {
                Text(
                    "⚠ $it",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(8.dp))
            }

            if (loading) {
                Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                    Text("解析中…")
                }
            }

            metadata?.let { meta ->
                ApkMetadataCard(meta, onSendToChat = {
                    val text = formatMetaForChat(meta)
                    onSendToChat(text)
                })
                Spacer(Modifier.height(12.dp))
                if (entries.isNotEmpty()) {
                    ApkEntriesCard(entries)
                }
            }
        }
    }
}

@Composable
private fun ApkMetadataCard(meta: ApkInspector.ApkMetadata, onSendToChat: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.size(6.dp))
                Text("APK 元数据", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            MetaRow("包名", meta.packageName)
            MetaRow("版本", "${meta.versionName} (code ${meta.versionCode})")
            MetaRow("大小", "${meta.fileSize / 1024} KB")
            if (meta.minSdk > 0) MetaRow("minSdk", "${meta.minSdk}")
            MetaRow("targetSdk", "${meta.targetSdk}")
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text("权限 (${meta.permissions.size})", fontWeight = FontWeight.Medium, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            if (meta.permissions.isEmpty()) {
                Text("无", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                meta.permissions.take(20).forEach { p ->
                    Text(
                        "• $p",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                    )
                }
                if (meta.permissions.size > 20) {
                    Text(
                        "… 及其他 ${meta.permissions.size - 20} 项",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                    )
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text("组件统计", fontWeight = FontWeight.Medium, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Text("Activity: ${meta.activities.size}  ·  Service: ${meta.services.size}", fontSize = 11.sp)
            Text("Receiver: ${meta.receivers.size}  ·  Provider: ${meta.providers.size}", fontSize = 11.sp)
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onSendToChat,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.size(6.dp))
                Text("发送到 AI 聊天辅助分析", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun ApkEntriesCard(entries: List<ApkInspector.ApkEntry>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.size(6.dp))
                Text(
                    "APK 文件清单 (${entries.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "提示：classes.dex 是 Java/Kotlin 编译产物，需用 dex2jar + jd-cli 反编译。" +
                    "resources.arsc 和 AndroidManifest.xml 需用 aapt2 / apktool 反编译。",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Column {
                entries.take(50).forEach { e ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            e.path,
                            fontSize = 10.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            modifier = Modifier.weight(1f),
                            maxLines = 2
                        )
                        Text(
                            "${e.size / 1024} KB",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (entries.size > 50) {
                    Text(
                        "… 及其他 ${entries.size - 50} 个文件",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetaRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(width = 80.dp, height = 20.dp)
        )
        Text(
            value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun formatMetaForChat(meta: ApkInspector.ApkMetadata): String {
    return buildString {
        append("请帮我分析以下 APK 的元数据：\n\n")
        append("- 包名：${meta.packageName}\n")
        append("- 版本：${meta.versionName} (code ${meta.versionCode})\n")
        append("- 文件大小：${meta.fileSize / 1024} KB\n")
        if (meta.minSdk > 0) append("- minSdk：${meta.minSdk}\n")
        append("- targetSdk：${meta.targetSdk}\n\n")
        append("权限列表 (${meta.permissions.size})：\n")
        meta.permissions.take(30).forEach { append("- $it\n") }
        if (meta.permissions.size > 30) append("- ... 及其他 ${meta.permissions.size - 30} 项\n")
        append("\n组件：\n")
        append("- Activity: ${meta.activities.size} 个\n")
        append("- Service: ${meta.services.size} 个\n")
        append("- Receiver: ${meta.receivers.size} 个\n")
        append("- Provider: ${meta.providers.size} 个\n\n")
        append("请基于以上信息：\n")
        append("1. 推断应用类型和主要功能\n")
        append("2. 评估权限请求是否合理\n")
        append("3. 提示可能存在的隐私/安全风险\n")
        append("4. 给出优化建议")
    }
}
