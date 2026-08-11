package com.floatai.ui.screens.api

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.floatai.ui.i18n.localStrings

/**
 * AI 聊天页的「管理模型」底部弹层。
 * 提供 Base URL / API Key 配置、拉取模型、选择/自定义模型能力。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiManagementSheet(
    onDismiss: () -> Unit
) {
    val app = LocalContext.current.applicationContext as App
    val vm: ApiConfigManager = viewModel(
        key = "api-config",
        factory = ApiConfigManager.factory(app.settingsRepository)
    )
    val state by vm.uiState.collectAsStateWithLifecycle()
    val strings = localStrings()
    var showCustomDialog by remember { mutableStateOf(false) }
    var customModel by remember { mutableStateOf(state.model) }

    LaunchedEffect(state.model) { customModel = state.model }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 720.dp)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(strings.api_title, style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onDismiss) { Text("✕") }
            }
            Text(
                strings.api_subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(strings.api_base_url_label, style = MaterialTheme.typography.labelLarge)
                    OutlinedTextField(
                        value = state.baseUrl,
                        onValueChange = vm::onBaseUrlChange,
                        placeholder = { Text("https://api.openai.com/v1") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Uri
                        ),
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                    )
                    Text(
                        strings.api_key_label,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    OutlinedTextField(
                        value = state.apiKey,
                        onValueChange = vm::onApiKeyChange,
                        placeholder = { Text("sk-...") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Password
                        ),
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Button(
                    onClick = vm::fetchModels,
                    enabled = !state.loading,
                    modifier = Modifier.weight(1f).padding(end = 6.dp)
                ) {
                    if (state.loading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.CloudDownload, contentDescription = null)
                        Text("  ${strings.api_fetch}")
                    }
                }
                Button(
                    onClick = vm::saveConfig,
                    modifier = Modifier.weight(1f).padding(start = 6.dp)
                ) {
                    Icon(Icons.Filled.Save, contentDescription = null)
                    Text("  ${strings.api_save}")
                }
            }

            Text(
                strings.api_title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 16.dp, bottom = 6.dp)
            )
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
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (state.model.isEmpty() || state.model == "auto") "auto（${strings.api_custom_hint}）" else state.model,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (state.models.isNotEmpty()) {
                Text(
                    strings.api_available(state.models.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp, bottom = 6.dp)
                )
                LazyColumn(modifier = Modifier.heightIn(max = 220.dp)) {
                    items(state.models, key = { it }) { m ->
                        val selected = m == state.model
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { vm.selectModel(m) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                m,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            if (selected) {
                                Text(
                                    strings.api_selected,
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }

            if (state.message.isNotEmpty()) {
                Text(
                    state.message,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
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
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (customModel.isNotBlank()) {
                        vm.selectModel(customModel.trim())
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
