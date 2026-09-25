package com.lingxi.ai.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.annotation.StringRes
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lingxi.ai.LingXiApp
import com.lingxi.ai.R
import com.lingxi.ai.agent.ChatMsg
import com.lingxi.ai.agent.LlmClient
import com.lingxi.ai.agent.ModelInfo
import com.lingxi.ai.data.ChatMode
import com.lingxi.ai.data.LocalModelEntry
import com.lingxi.ai.ui.glass.GlassBubble
import com.lingxi.ai.ui.str
import com.lingxi.ai.ui.glass.GlassCardRow
import com.lingxi.ai.ui.glass.GlassChip
import com.lingxi.ai.ui.glass.GlassIconButton
import com.lingxi.ai.ui.glass.GlassPanel
import com.lingxi.ai.ui.glass.GlassTextField
import com.lingxi.ai.ui.glass.rememberGlassColors
import com.lingxi.ai.update.DownloadStatus
import com.lingxi.ai.update.GitHubApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    app: LingXiApp,
    snackbarHostState: SnackbarHostState
) {
    val settings by app.settings.settings.collectAsState()
    val conversation by app.agent.conversation.collectAsState()
    val processing by app.agent.processing.collectAsState()
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val glass = rememberGlassColors()
    val listState = rememberLazyListState()
    var input by remember { mutableStateOf("") }
    var showModelDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showVideoDialog by remember { mutableStateOf(false) }
    val generating by app.videoGen.generating.collectAsState()

    androidx.compose.runtime.LaunchedEffect(conversation.size) {
        if (conversation.isNotEmpty()) {
            listState.animateScrollToItem(conversation.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        GlassPanel(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassChip(
                    text = settings.apiModel.ifEmpty { stringResource(R.string.chat_select_model) },
                    selected = false,
                    onClick = { showModelDialog = true }
                )
                Spacer(Modifier.width(8.dp))
                GlassChip(
                    text = if (settings.chatMode == ChatMode.DEV)
                        stringResource(R.string.chat_mode_dev)
                    else stringResource(R.string.chat_thinking_level, settings.thinkingLevel),
                    selected = false,
                    onClick = {
                        val next = (settings.thinkingLevel % 3) + 1
                        app.settings.update { it.copy(thinkingLevel = next) }
                    }
                )
                Spacer(Modifier.width(8.dp))
                GlassChip(
                    text = if (generating) stringResource(R.string.chat_generating)
                    else stringResource(R.string.chat_video_generate),
                    selected = generating,
                    onClick = { showVideoDialog = true }
                )
                Spacer(Modifier.weight(1f))
                Box {
                    GlassIconButton(
                        icon = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.chat_more),
                        onClick = { showMoreMenu = true }
                    )
                    DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.chat_clear)) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            onClick = {
                                app.agent.clearConversation()
                                showMoreMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.chat_copy_all)) },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            onClick = {
                                val text = conversation.joinToString("\n") { "${it.role}: ${it.content}" }
                                clipboard.setText(AnnotatedString(text))
                                showMoreMenu = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (conversation.isEmpty()) {
                item { EmptyHint(apiConfigured = settings.apiConfigured) }
            }
            items(conversation, key = { it.hashCode() }) { msg ->
                ChatBubble(msg = msg, streaming = processing && msg == conversation.lastOrNull() && msg.role == "assistant")
            }
        }

        Spacer(Modifier.height(8.dp))

        GlassPanel(
            shape = RoundedCornerShape(26.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                GlassTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = if (settings.apiConfigured)
                        stringResource(R.string.chat_input_hint)
                    else stringResource(R.string.chat_api_not_configured),
                    maxLines = 4,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSend = {
                        val text = input.trim()
                        if (text.isNotEmpty() && !processing) {
                            input = ""
                            app.coordinator.submit(text)
                        }
                    })
                )
                Spacer(Modifier.width(8.dp))
                if (processing) {
                    GlassIconButton(
                        icon = Icons.Default.Stop,
                        contentDescription = stringResource(R.string.chat_stop),
                        size = 44.dp,
                        onClick = { app.agent.requestCancel() }
                    )
                } else {
                    GlassIconButton(
                        icon = Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.chat_send),
                        size = 44.dp,
                        onClick = {
                            val text = input.trim()
                            if (text.isNotEmpty() && !processing) {
                                input = ""
                                app.coordinator.submit(text)
                            }
                        }
                    )
                }
            }
        }
    }

    if (showModelDialog) {
        UnifiedModelDialogV2(app = app, onDismiss = { showModelDialog = false })
    }
    if (showVideoDialog) {
        VideoGenDialog(app = app, onDismiss = { showVideoDialog = false })
    }
}

@Composable
private fun VideoGenDialog(app: LingXiApp, onDismiss: () -> Unit) {
    val settings by app.settings.settings.collectAsState()
    val vg = settings.videoGen
    var prompt by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.chat_video_title), fontWeight = FontWeight.SemiBold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(
                        R.string.chat_video_script_model,
                        vg.scriptModel.ifBlank { settings.apiModel.ifEmpty { stringResource(R.string.common_unknown) } }
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(
                        R.string.chat_video_model_line,
                        vg.videoModel.ifEmpty { stringResource(R.string.chat_video_model_none) }
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text(stringResource(R.string.chat_video_prompt_label)) },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                if (vg.videoModel.isBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.chat_video_need_local_model),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val p = prompt.trim()
                    if (p.isNotEmpty()) {
                        app.videoGen.generateVideo(p)
                        onDismiss()
                    }
                },
                enabled = prompt.isNotBlank() && vg.videoModel.isNotBlank()
            ) {
                Text(stringResource(R.string.chat_video_generate_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}

@Composable
private fun ChatBubble(msg: ChatMsg, streaming: Boolean) {
    val glass = rememberGlassColors()
    when {
        msg.role == "user" -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                GlassBubble(
                    accent = true,
                    modifier = Modifier.fillMaxWidth(0.82f)
                ) {
                    Text(
                        text = msg.content,
                        color = glass.onAccent,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        msg.role == "tool" -> {
            GlassCardRow {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    tint = glass.accent,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = msg.toolName ?: stringResource(R.string.chat_tool_default),
                    color = glass.accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = msg.content,
                    color = glass.contentMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 3,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        msg.role == "video" -> {
            VideoResultCard(path = msg.toolName ?: "", summary = msg.content)
        }
        else -> {
            Row(modifier = Modifier.fillMaxWidth()) {
                GlassBubble(
                    accent = false,
                    modifier = Modifier.fillMaxWidth(0.88f)
                ) {
                    Text(
                        text = msg.content.ifEmpty { if (streaming) "▍" else "…" } + if (streaming && msg.content.isNotEmpty()) " ▍" else "",
                        color = glass.content,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoResultCard(path: String, summary: String) {
    val glass = rememberGlassColors()
    Row(modifier = Modifier.fillMaxWidth()) {
        GlassCardRow {
            Icon(
                Icons.Default.Download,
                contentDescription = null,
                tint = glass.accent,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.chat_video_output), color = glass.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    text = summary,
                    color = glass.contentMuted,
                    fontSize = 11.sp,
                    maxLines = 2,
                    modifier = Modifier.padding(top = 2.dp)
                )
                if (path.isNotBlank()) {
                    Text(
                        text = path,
                        color = glass.contentMuted,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}


@Composable
private fun EmptyHint(apiConfigured: Boolean) {
    val glass = rememberGlassColors()
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 44.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GlassPanel(shape = RoundedCornerShape(28.dp)) {
            Column(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.app_name), fontSize = 30.sp, fontWeight = FontWeight.Bold, color = glass.content, letterSpacing = 4.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    if (apiConfigured) stringResource(R.string.chat_empty_title)
                    else stringResource(R.string.chat_empty_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = glass.contentMuted
                )
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassChip(text = stringResource(R.string.chat_hint_code), selected = false, onClick = {})
                    GlassChip(text = stringResource(R.string.chat_hint_explain), selected = false, onClick = {})
                }
            }
        }
    }
}

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedModelDialogV2(app: LingXiApp, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val settings by app.settings.settings.collectAsState()
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
    var scannedLocalModels by remember { mutableStateOf<List<ModelInfo>>(emptyList()) }

    suspend fun rescanLocalModels() {
        scannedLocalModels = withContext(Dispatchers.IO) {
            val dir = File(context.filesDir, "models")
            (dir.listFiles() ?: emptyArray())
                .filter { f -> f.isFile && !f.name.startsWith(".") }
                .map { f ->
                    val mb = String.format("%.1f", f.length() / 1048576.0)
                    ModelInfo(
                        id = f.name,
                        name = f.name,
                        description = context.str(R.string.chat_model_local_downloaded, mb)
                    )
                }
                .sortedByDescending { it.name }
        }
    }

    LaunchedEffect(Unit) { rescanLocalModels() }

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
        title = { Text(stringResource(R.string.chat_model_config_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 550.dp)) {
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
                            Text(stringResource(R.string.chat_model_tab_local), fontSize = 12.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal)
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
                            Text(stringResource(R.string.chat_model_tab_api), fontSize = 12.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (selectedTab == 0) {
                    Text(stringResource(R.string.chat_source_select), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
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
                                        Text(stringResource(modelSourceDescRes(source.name)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
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
                                        Text(stringResource(R.string.chat_refreshing))
                                    } else {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(stringResource(R.string.chat_refresh_models))
                                    }
                                }
                            }
                        }

                        if (availableModels.isNotEmpty()) {
                            item {
                                Spacer(Modifier.height(12.dp))
                                Text(stringResource(R.string.chat_available_models, availableModels.size), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
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
                                                        if (status is DownloadStatus.Done) rescanLocalModels()
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
                    Text(stringResource(R.string.chat_api_provider_config), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))

                    Text(stringResource(R.string.chat_quick_preset), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
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

                    if (selectedProvider.isNotEmpty()) {
                        Text(stringResource(R.string.chat_api_config), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = providerUrl,
                            onValueChange = { providerUrl = it },
                            label = { Text(stringResource(R.string.settings_api_url)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = providerKey,
                            onValueChange = { providerKey = it },
                            label = { Text(stringResource(R.string.chat_api_key_optional)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(10.dp))

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
                                        testResult = context.str(R.string.chat_test_ok, models.size)
                                    } catch (e: Exception) {
                                        testResult = context.str(R.string.chat_test_failed, e.message)
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
                                Text(stringResource(R.string.chat_test_testing))
                            } else {
                                Text(stringResource(R.string.settings_api_test))
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

                    if (availableModels.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.chat_available_models, availableModels.size), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
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

                    if (scannedLocalModels.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            stringResource(R.string.chat_local_downloaded_section, scannedLocalModels.size),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        LazyColumn(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                            items(scannedLocalModels, key = { it.id }) { model ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                        .clickable {
                                            app.settings.update {
                                                it.copy(
                                                    apiModel = model.id,
                                                    modelSource = "local",
                                                    localModels = it.localModels + LocalModelEntry(
                                                        id = model.id,
                                                        name = model.name,
                                                        path = File(context.filesDir, "models/${model.id}").absolutePath,
                                                        sizeBytes = 0L,
                                                        source = "local"
                                                    )
                                                )
                                            }
                                        },
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(model.name, modifier = Modifier.weight(1f))
                                            Text(
                                                model.description,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 双模型视频生成：指定脚本模型（API/自定义模型）
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.chat_video_script_label),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.chat_video_script_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    if (availableModels.isNotEmpty()) {
                        LazyColumn(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                            items(availableModels, key = { "sm_" + it.id }) { model ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                        .clickable {
                                            app.settings.update {
                                                it.copy(videoGen = it.videoGen.copy(scriptModel = model.id))
                                            }
                                        },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (settings.videoGen.scriptModel == model.id ||
                                        (settings.videoGen.scriptModel.isBlank() && settings.apiModel == model.id)
                                    ) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
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

                    // 双模型视频生成：指定本地视频模型（默认即当前选中本地模型）
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.chat_video_model_local_label),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.chat_video_dual_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    if (scannedLocalModels.isNotEmpty()) {
                        LazyColumn(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                            items(scannedLocalModels, key = { "vm_" + it.id }) { model ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                        .clickable {
                                            app.settings.update {
                                                it.copy(
                                                    videoGen = it.videoGen.copy(
                                                        enabled = true,
                                                        videoModel = model.id,
                                                        scriptModel = it.videoGen.scriptModel.ifBlank { model.id }
                                                    )
                                                )
                                            }
                                        },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (settings.videoGen.videoModel == model.id)
                                        MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(model.name, modifier = Modifier.weight(1f))
                                            Text(
                                                stringResource(R.string.chat_video_model_as_video),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            stringResource(R.string.chat_video_no_local),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_done)) }
        }
    )
}


@StringRes
private fun modelSourceDescRes(name: String): Int = when (name) {
    "HuggingFace" -> R.string.chat_source_hf_desc
    "Ollama Library" -> R.string.chat_source_ollama_desc
    "LM Studio" -> R.string.chat_source_lmstudio_desc
    "vLLM" -> R.string.chat_source_vllm_desc
    "Jan" -> R.string.chat_source_jan_desc
    else -> R.string.common_unknown
}
