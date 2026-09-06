package com.bskai.ui.chat

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bskai.AuraApp
import com.bskai.agent.LlmClient
import com.bskai.agent.ModelInfo
import com.bskai.data.ChatMode
import com.bskai.update.DownloadStatus
import com.bskai.update.GitHubApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    app: AuraApp,
    snackbarHostState: SnackbarHostState
) {
    val settings by app.settings.settings.collectAsState()
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val messages = remember { mutableStateListOf<ChatMsg>() }
    var input by remember { mutableStateOf("") }
    var isStreaming by remember { mutableStateOf(false) }
    var showModelDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    fun submit() {
        val text = input.trim()
        if (text.isEmpty() || isStreaming) return
        input = ""
        messages.add(ChatMsg(role = "user", content = text))
        isStreaming = true
        scope.launch {
            try {
                messages.add(ChatMsg(role = "assistant", content = ""))
                isStreaming = false
            } catch (e: Exception) {
                messages.add(ChatMsg(role = "assistant", content = "错误: ${e.message}"))
                isStreaming = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Chat, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("AURA", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            if (settings.chatMode == ChatMode.DEV) "应用开发模式" else "深度 ${settings.thinkingLevel}/3",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            actions = {
                IconButton(onClick = { showModelDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "模型")
                }
                Box {
                    IconButton(onClick = { showMoreMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "更多")
                    }
                    DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("清空对话") },
                            onClick = { messages.clear(); showMoreMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("复制全部") },
                            onClick = {
                                val text = messages.joinToString("\n") { "${it.role}: ${it.content}" }
                                clipboard.setText(AnnotatedString(text))
                                showMoreMenu = false
                            }
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (messages.isEmpty()) {
                item { EmptyHint(settings.apiConfigured) }
            }
            items(messages) { msg -> ChatBubble(msg = msg) }
        }

        Surface(
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("输入消息...") },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = { submit() },
                    enabled = !isStreaming && input.isNotBlank()
                ) {
                    if (isStreaming) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Default.Send, contentDescription = "发送")
                    }
                }
            }
        }
    }

    if (showModelDialog) {
        UnifiedModelDialogV2(app = app, onDismiss = { showModelDialog = false })
    }
}

@Composable
private fun ChatBubble(msg: ChatMsg) {
    val isUser = msg.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp, topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text(
                text = msg.content.ifEmpty { "..." },
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun EmptyHint(apiConfigured: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🤖", fontSize = 48.sp)
        Spacer(Modifier.height(12.dp))
        Text("开始对话", fontWeight = FontWeight.Medium)
        Text(
            if (apiConfigured) "输入消息开始与 AURA 对话" else "请先配置 AI 服务",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

data class ChatMsg(
    val role: String,
    val content: String
)

// ========== 模型下载源 ==========
data class ModelSource(
    val name: String,
    val baseUrl: String,
    val description: String,
    val icon: String = "📦"
)

val localModelSources = listOf(
    ModelSource("HuggingFace", "https://huggingface.co/api/models?tag=llama", "HuggingFace 模型仓库", "🤗"),
    ModelSource("Ollama Library", "https://ollama.com/api/tags", "Ollama 官方模型库", "🦙"),
    ModelSource("LM Studio", "https://api.lmstudio.ai/v1/models", "LM Studio 模型市场", "🧪"),
    ModelSource("vLLM", "http://localhost:8000/v1/models", "vLLM 本地推理", "⚡"),
    ModelSource("Jan", "http://localhost:1337/v1/models", "Jan 本地模型", "🐦")
)

// ========== 统一模型对话框 V2 ==========
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedModelDialogV2(app: AuraApp, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }

    var downloadProgress by remember { mutableStateOf(mapOf<String, DownloadStatus>()) }
    var downloading by remember { mutableStateOf<String?>(null) }

    var selectedProvider by remember { mutableStateOf("") }
    var providerUrl by remember { mutableStateOf("") }
    var providerKey by remember { mutableStateOf("") }
    var availableModels by remember { mutableStateOf<List<ModelInfo>>(emptyList()) }
    var isLoadingModels by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var selectedSource by remember { mutableStateOf<ModelSource?>(null) }

    val providers = listOf(
        "Ollama" to "http://localhost:11434/v1",
        "LM Studio" to "http://localhost:1234/v1",
        "vLLM" to "http://localhost:8000/v1",
        "Jan" to "http://localhost:1337/v1",
        "OpenAI" to "https://api.openai.com/v1",
        "DeepSeek" to "https://api.deepseek.com/v1",
        "DashScope" to "https://dashscope.aliyuncs.com/compatible-mode/v1",
        "Custom" to ""
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("模型配置", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 550.dp)) {
                // Tab 选择器
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f).height(48.dp)
                            .clickable { selectedTab = 0 },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedTab == 0) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📥", fontSize = 16.sp)
                            Spacer(Modifier.width(6.dp))
                            Text("本地模型", fontSize = 12.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                    Surface(
                        modifier = Modifier.weight(1f).height(48.dp)
                            .clickable { selectedTab = 1 },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedTab == 1) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🔌", fontSize = 16.sp)
                            Spacer(Modifier.width(6.dp))
                            Text("API 服务商", fontSize = 12.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (selectedTab == 0) {
                    // ===== 本地模型 Tab =====
                    Text("选择下载源", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))

                    LazyColumn(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                        items(localModelSources) { source ->
                            Surface(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                                    .clickable {
                                        selectedSource = source
                                        providerUrl = source.baseUrl
                                        availableModels = emptyList()
                                        testResult = null
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selectedSource == source) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(source.icon, fontSize = 20.sp)
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(source.name, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                        Text(source.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                    }
                                    if (selectedSource == source) {
                                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }

                        if (selectedSource != null) {
                            item {
                                Spacer(Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        if (providerUrl.isNotBlank()) {
                                            isLoadingModels = true
                                            scope.launch {
                                                val models = withContext(Dispatchers.IO) {
                                                    try {
                                                        LlmClient(app).listModels(providerUrl, providerKey)
                                                    } catch (e: Exception) {
                                                        emptyList()
                                                    }
                                                }
                                                availableModels = models
                                                isLoadingModels = false
                                            }
                                        }
                                    },
                                    enabled = !isLoadingModels && providerUrl.isNotBlank(),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (isLoadingModels) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("刷新中...")
                                    } else {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("刷新模型列表")
                                    }
                                }
                            }
                        }

                        if (availableModels.isNotEmpty()) {
                            item {
                                Spacer(Modifier.height(12.dp))
                                Text("可用模型 (${availableModels.size})", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(4.dp))
                            }
                            items(availableModels) { model ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                        .clickable {
                                            val targetFile = File(context.filesDir, "models/${model.id}")
                                            downloading = model.id
                                            scope.launch {
                                                GitHubApi.downloadApk("${providerUrl}/models/${model.id}", targetFile).collect { status ->
                                                    downloadProgress = downloadProgress + (model.id to status)
                                                    if (status is DownloadStatus.Done || status is DownloadStatus.Failed) {
                                                        downloading = null
                                                    }
                                                }
                                            }
                                        },
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(model.name, modifier = Modifier.weight(1f))
                                        if (downloading == model.id) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // ===== 自定义服务商 Tab =====
                    Text("配置 AI 服务商", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))

                    // 服务商网格
                    Text("快速选择", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        providers.chunked(4).forEach { row ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                row.forEach { (name, url) ->
                                    Surface(
                                        modifier = Modifier.weight(1f).height(42.dp)
                                            .clickable {
                                                selectedProvider = name
                                                providerUrl = url
                                                availableModels = emptyList()
                                                testResult = null
                                            },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (selectedProvider == name) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text(
                                                name, fontSize = 11.sp,
                                                fontWeight = if (selectedProvider == name) FontWeight.Bold else FontWeight.Normal,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // API 配置表单
                    if (selectedProvider.isNotEmpty()) {
                        Text("API 配置", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = providerUrl,
                            onValueChange = { providerUrl = it },
                            label = { Text("API 地址") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = providerKey,
                            onValueChange = { providerKey = it },
                            label = { Text("API Key (可选)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(10.dp))

                        // 测试连接按钮
                        Button(
                            onClick = {
                                scope.launch {
                                    isLoadingModels = true
                                    testResult = null
                                    try {
                                        val models = withContext(Dispatchers.IO) {
                                            LlmClient(app).listModels(providerUrl, providerKey)
                                        }
                                        availableModels = models
                                        app.settings.update { it.copy(apiProviderUrl = providerUrl, apiProviderKey = providerKey, modelSource = "local") }
                                        testResult = "✅ 连接成功，获取到 ${models.size} 个模型"
                                    } catch (e: Exception) {
                                        testResult = "❌ 连接失败: ${e.message}"
                                        availableModels = emptyList()
                                    }
                                    isLoadingModels = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoadingModels && providerUrl.isNotBlank()
                        ) {
                            if (isLoadingModels) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("测试中...")
                            } else {
                                Text("🔌 测试连接")
                            }
                        }

                        if (testResult != null) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                testResult!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (testResult!!.startsWith("✅")) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    // 可用模型列表
                    if (availableModels.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text("可用模型 (${availableModels.size})", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        LazyColumn(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                            items(availableModels) { model ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                        .clickable {
                                            app.settings.update { it.copy(apiModel = model.id, modelSource = "api") }
                                        },
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(model.name, modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("完成") }
        }
    )
}
