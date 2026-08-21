package com.bskai.ui.screens.models

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.bskai.models.LocalModel
import com.bskai.models.ModelDownloader
import com.bskai.models.ModelStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ModelHubScreen(app: BskApp) {
    val store = app.modelStore
    var models by remember { mutableStateOf(store.snapshot()) }
    var activeDownload by remember { mutableStateOf<String?>(null) }
    var showCustomDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val downloader = remember {
        ModelDownloader(
            context = app,
            store = store,
            onProgress = { catalogId, progress, _, _ ->
                store.updateOne(app, catalogId) { m ->
                    m.withStatus(ModelStatus.DOWNLOADING, progress = progress)
                }
                models = store.snapshot()
            },
            onFinished = { _, _, _ ->
                models = store.snapshot()
                activeDownload = null
            }
        )
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("本地模型中心", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            OutlinedButton(onClick = { showCustomDialog = true }) {
                Text("自定义下载")
            }
        }
        Text(
            "下载 GGUF 模型到设备本地，可用于离线推理与私有部署",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(models, key = { it.catalogId }) { model ->
                ModelCard(
                    model = model,
                    downloading = activeDownload == model.catalogId,
                    onDownload = {
                        activeDownload = model.catalogId
                        scope.launch {
                            downloader.download(model)
                        }
                    },
                    onCancel = {
                        downloader.cancel()
                        activeDownload = null
                    },
                    onDelete = {
                        store.remove(app, model.catalogId)
                        models = store.snapshot()
                    }
                )
            }
        }
    }

    if (showCustomDialog) {
        CustomUrlDialog(
            onDismiss = { showCustomDialog = false },
            onConfirm = { name, url, fileName, sizeHint ->
                store.addCustom(app, name, url, fileName, sizeHint)
                models = store.snapshot()
                showCustomDialog = false
            }
        )
    }
}

@Composable
private fun ModelCard(
    model: LocalModel,
    downloading: Boolean,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(model.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(
                    "${model.parameters} · ${model.quant}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Text(
                model.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                Text(model.sizeHint, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                when (model.status) {
                    ModelStatus.READY -> {
                        Text("已就绪", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.width(12.dp))
                        TextButton(onClick = onDelete) { Text("删除") }
                    }
                    ModelStatus.DOWNLOADING -> {
                        Text("下载中", color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.width(12.dp))
                        TextButton(onClick = onCancel) { Text("取消") }
                    }
                    ModelStatus.FAILED -> {
                        Text("下载失败", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.width(12.dp))
                        Button(onClick = onDownload, enabled = !downloading) { Text("重试") }
                    }
                    else -> {
                        Button(onClick = onDownload, enabled = !downloading) { Text("下载") }
                    }
                }
            }
            if (model.status == ModelStatus.DOWNLOADING) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { model.progress },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "${(model.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun CustomUrlDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("") }
    var sizeHint by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("自定义模型下载") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("模型名称") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("GGUF 下载链接") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = fileName, onValueChange = { fileName = it }, label = { Text("文件名 (.gguf)") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = sizeHint, onValueChange = { sizeHint = it }, label = { Text("大小提示，如 约 500 MB") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name.trim(), url.trim(), fileName.trim().ifBlank { url.trim().substringAfterLast('/') }, sizeHint.trim()) }, enabled = name.isNotBlank() && url.isNotBlank()) {
                Text("加入队列")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
