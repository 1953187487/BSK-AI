package com.bskai.ui.screens.providers

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.unit.dp
import com.bskai.BskApp
import com.bskai.data.remote.ModelsClient
import com.bskai.models.ProviderConfig
import kotlinx.coroutines.launch

@Composable
fun ProvidersScreen(app: BskApp, onBack: () -> Unit) {
    val store = app.providerStore
    var providers by remember { mutableStateOf(store.snapshot()) }
    var showEditor by remember { mutableStateOf<ProviderConfig?>(null) }
    var showNew by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun refresh() {
        providers = store.snapshot()
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
            }
            Text("模型服务商", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            Button(onClick = { showNew = true }) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("新增")
            }
        }
        Text(
            "支持所有 OpenAI 兼容接口，可配置自定义服务商模型",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        if (providers.isEmpty()) {
            Text(
                "还没有配置服务商。点击右上角「新增」，填入 Base URL、API Key 与模型即可开始使用。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(providers, key = { it.id }) { p ->
                    ProviderCard(
                        config = p,
                        onSelect = {
                            store.setActive(p.id)
                            refresh()
                        },
                        onEdit = { showEditor = p },
                        onDelete = {
                            store.remove(p.id)
                            refresh()
                        },
                        onTest = {
                            scope.launch {
                                testResult = "测试中..."
                                val ok = ModelsClient.testConnection(p.baseUrl, p.apiKey, p.model)
                                testResult = if (ok) "连接成功: ${p.name}" else "连接失败: ${p.name}"
                            }
                        }
                    )
                }
            }
        }
        testResult?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = if (it.startsWith("连接成功")) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }

    if (showNew) {
        ProviderEditorDialog(
            title = "新增服务商",
            initial = null,
            onDismiss = { showNew = false },
            onConfirm = { cfg ->
                store.upsert(cfg)
                refresh()
                showNew = false
            }
        )
    }

    showEditor?.let { cfg ->
        ProviderEditorDialog(
            title = "编辑服务商",
            initial = cfg,
            onDismiss = { showEditor = null },
            onConfirm = { updated ->
                store.upsert(updated)
                refresh()
                showEditor = null
            }
        )
    }
}

@Composable
private fun ProviderCard(
    config: ProviderConfig,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTest: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = config.isActive, onClick = onSelect)
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(config.name, style = MaterialTheme.typography.titleMedium)
                    if (config.isActive) {
                        Text(
                            "  ·  使用中",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                Text(
                    config.baseUrl,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "模型: ${config.model}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Row(Modifier.padding(top = 6.dp)) {
                    TextButton(onClick = onTest) { Text("测试连接") }
                    IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, contentDescription = "编辑") }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Outlined.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderEditorDialog(
    title: String,
    initial: ProviderConfig?,
    onDismiss: () -> Unit,
    onConfirm: (ProviderConfig) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var baseUrl by remember { mutableStateOf(initial?.baseUrl ?: "") }
    var apiKey by remember { mutableStateOf(initial?.apiKey ?: "") }
    var model by remember { mutableStateOf(initial?.model ?: "") }
    var modelsText by remember { mutableStateOf(initial?.models?.joinToString(", ") ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("服务商名称") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("Base URL") },
                    placeholder = { Text("如 https://api.openai.com/v1 或 https://api.deepseek.com/v1") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("默认模型") },
                    placeholder = { Text("如 deepseek-chat") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = modelsText,
                    onValueChange = { modelsText = it },
                    label = { Text("可选模型（逗号分隔）") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        ProviderConfig(
                            id = initial?.id ?: "prov_${System.currentTimeMillis()}",
                            name = name.trim().ifBlank { "未命名服务商" },
                            baseUrl = baseUrl.trim().trimEnd('/'),
                            apiKey = apiKey.trim(),
                            model = model.trim().ifBlank { "auto" },
                            models = modelsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        )
                    )
                },
                enabled = baseUrl.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
