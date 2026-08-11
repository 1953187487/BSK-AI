package com.floatai.ui.screens.api

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.floatai.App
import com.floatai.ui.components.GlassCard
import com.floatai.ui.components.SectionTitle

@Composable
fun ApiScreen(modifier: Modifier = Modifier) {
    val app = LocalContext.current.applicationContext as App
    val vm: ApiViewModel = viewModel(
        key = "api",
        factory = ApiViewModel.factory(app.settingsRepository)
    )
    val state by vm.uiState.collectAsStateWithLifecycle()

    var showCustomDialog by remember { mutableStateOf(false) }
    var customModel by remember { mutableStateOf(state.model) }

    val commonModels = listOf(
        "gpt-4o", "gpt-4o-mini", "gpt-3.5-turbo",
        "deepseek-chat", "deepseek-reasoner",
        "claude-3-5-sonnet", "gemini-pro", "qwen-max", "glm-4"
    )

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text("API 配置中心", style = MaterialTheme.typography.headlineMedium)
        Text(
            "支持所有 OpenAI 兼容协议的 AI 服务商",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp)
        )

        GlassCard(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Column {
                Text("Base URL", style = MaterialTheme.typography.bodyLarge)
                OutlinedTextField(
                    value = state.baseUrl,
                    onValueChange = vm::onBaseUrlChange,
                    placeholder = { Text("https://api.openai.com/v1") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                Text(
                    "API Key",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
                OutlinedTextField(
                    value = state.apiKey,
                    onValueChange = vm::onApiKeyChange,
                    placeholder = { Text("sk-...") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Button(
                onClick = vm::fetchModels,
                enabled = !state.loading,
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            ) {
                if (state.loading) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("获取模型")
                }
            }
            Button(
                onClick = vm::saveConfig,
                modifier = Modifier.weight(1f).padding(start = 8.dp)
            ) { Text("保存配置") }
        }

        SectionTitle("当前模型")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            customModel = state.model
                            showCustomDialog = true
                        }
                        .padding(12.dp)
                ) {
                    Text(
                        if (state.model.isEmpty() || state.model == "auto") "auto（点击自定义）" else state.model,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
                Text(
                    "点击可自定义模型名称",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        SectionTitle("常用模型")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                commonModels.chunked(2).forEach { rowModels ->
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                        rowModels.forEach { m ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp)
                                    .background(
                                        if (m == state.model) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                        else MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { vm.selectModel(m) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(m, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }

        if (state.models.isNotEmpty()) {
            SectionTitle("可用模型 (${state.models.size})")
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    state.models.forEach { m ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(
                                    if (m == state.model) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { vm.selectModel(m) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(m, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Text(
                state.message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    if (showCustomDialog) {
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = { Text("自定义模型") },
            text = {
                OutlinedTextField(
                    value = customModel,
                    onValueChange = { customModel = it },
                    placeholder = { Text("输入模型名称") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (customModel.isNotBlank()) {
                        vm.onModelChange(customModel.trim())
                        vm.saveConfig()
                    }
                    showCustomDialog = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false }) { Text("取消") }
            }
        )
    }
}
