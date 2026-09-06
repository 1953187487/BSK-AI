package com.bskai.ui.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bskai.AuraApp
import com.bskai.BuildConfig
import com.bskai.agent.ChatMsg
import com.bskai.agent.LlmClient
import com.bskai.data.ChatMode
import com.bskai.data.DefaultModelPresets
import com.bskai.data.ThemeStyle
import com.bskai.ui.theme.ThemeBackdrop
import kotlinx.coroutines.launch

data class QueuedMessage(val text: String, val id: Int)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    app: AuraApp,
    snackbarHostState: SnackbarHostState = SnackbarHostState()
) {
    val conversation by app.agent.conversation.collectAsState()
    val processing by app.agent.processing.collectAsState()
    val settings by app.settings.settings.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var input by remember { mutableStateOf("") }
    var showTopMenu by remember { mutableStateOf(false) }
    var showModelDialog by rememberSaveable { mutableStateOf(false) }
    var showSlashSuggestions by rememberSaveable { mutableStateOf(false) }
    var slashQuery by rememberSaveable { mutableStateOf("") }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var showCopyMenu by remember { mutableStateOf(false) }
    var copyMenuText by remember { mutableStateOf("") }

    val messageQueue = remember { mutableStateListOf<QueuedMessage>() }
    var queueIdCounter by remember { mutableIntStateOf(0) }

    val shouldShowFeedback = remember {
        val s = settings
        !s.feedbackDismissedThisSession &&
            (System.currentTimeMillis() - s.lastFeedbackDismissTime > 24 * 60 * 60 * 1000L)
    }

    LaunchedEffect(shouldShowFeedback) {
        if (shouldShowFeedback) showFeedbackDialog = true
    }

    LaunchedEffect(conversation.size) {
        if (conversation.isNotEmpty()) listState.scrollToItem(conversation.size - 1)
    }

    fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("AURA", text))
    }

    fun submitText() {
        if (input.isBlank()) return
        val text = input.trim()
        if (text.startsWith("/")) {
            val parts = text.removePrefix("/").split(" ", limit = 2)
            val cmd = parts.firstOrNull() ?: ""
            val arg = if (parts.size > 1) parts[1] else ""
            val command = app.slashRegistry.get(cmd)
            if (command != null) {
                when (val outcome = command.resolve(arg)) {
                    is com.bskai.agent.slash.SlashOutcome.SendToAi -> app.coordinator.submit(outcome.text)
                    is com.bskai.agent.slash.SlashOutcome.LocalMessage -> app.agent.notifyAssistant(outcome.message)
                    is com.bskai.agent.slash.SlashOutcome.Cancel -> {}
                }
            }
        } else {
            app.coordinator.submit(text)
        }
        input = ""
        showSlashSuggestions = false
    }

    fun addToQueue() {
        if (input.isBlank()) return
        messageQueue.add(QueuedMessage(input.trim(), queueIdCounter++))
        input = ""
    }

    fun sendQueue() {
        if (messageQueue.isNotEmpty()) {
            val combined = messageQueue.joinToString("\n") { it.text }
            messageQueue.clear()
            app.coordinator.submit(combined)
        }
    }

    fun detectSlash(query: String) {
        slashQuery = query
        showSlashSuggestions = query.startsWith("/") && query.length > 1
    }

    val style = settings.themeStyle
    val thinkingLevel = settings.thinkingLevel
    val chatMode = settings.chatMode

    Box(modifier = Modifier.fillMaxSize()) {
        ThemeBackdrop(style = style, dark = settings.darkTheme)

        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🤖", fontSize = 20.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("AURA", fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
                        Text(
                            text = if (chatMode == ChatMode.DEV) "应用开发模式" else "思考模式 · 深度 $thinkingLevel",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box {
                        IconButton(onClick = { showTopMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = null, modifier = Modifier.size(22.dp))
                        }
                        DropdownMenu(expanded = showTopMenu, onDismissRequest = { showTopMenu = false }) {
                            DropdownMenuItem(text = { Text("思考深度: $thinkingLevel/3") }, onClick = {})
                            DropdownMenuItem(
                                text = { Text("清空对话") },
                                onClick = {
                                    showTopMenu = false
                                    app.agent.clearConversation()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("当前：${BuildConfig.APP_VERSION}") },
                                enabled = false,
                                onClick = {}
                            )
                        }
                    }
                }
            }

            // Slash command suggestions
            if (showSlashSuggestions) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    shadowElevation = 2.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                        val suggestions = app.slashRegistry.suggestions(slashQuery)
                        suggestions.forEach { cmd ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .combinedClickable(onClick = {
                                        input = "/${cmd.key} "
                                        showSlashSuggestions = false
                                    })
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "/${cmd.key}",
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.width(70.dp)
                                )
                                Text(
                                    text = cmd.description,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            // Message queue display
            if (messageQueue.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            "消息队列 (${messageQueue.size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(4.dp))
                        messageQueue.forEach { qm ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    qm.text,
                                    modifier = Modifier.weight(1f),
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                IconButton(onClick = { messageQueue.remove(qm) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Delete, "移除", modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { sendQueue() }) { Text("立即发送全部", fontSize = 12.sp) }
                        }
                    }
                }
            }

            // Chat messages
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    if (conversation.isEmpty()) {
                        item { EmptyHint(settings.apiConfigured) }
                    } else {
                        itemsIndexed(conversation) { _, msg ->
                            val isStreaming = msg.role == "assistant" && processing &&
                                msg === conversation.lastOrNull { it.role == "assistant" } &&
                                msg.content.isNotEmpty() &&
                                conversation.last() === msg
                            ChatBubble(
                                msg = msg,
                                style = style,
                                streaming = isStreaming,
                                onLongPress = { copyMenuText = it; showCopyMenu = true },
                                onResend = if (msg.role == "user") {
                                    { app.coordinator.submit(msg.content) }
                                } else null
                            )
                        }
                    }
                }
            }

            // Input bar
            ChatInputBar(
                text = input,
                onTextChange = { input = it; detectSlash(it) },
                onSubmit = { submitText() },
                onAddToQueue = { addToQueue() },
                processing = processing,
                queueSize = messageQueue.size,
                chatMode = chatMode,
                thinkingLevel = thinkingLevel,
                onToggleMode = {
                    val next = if (chatMode == ChatMode.THINK) ChatMode.DEV else ChatMode.THINK
                    app.settings.update { it.copy(chatMode = next) }
                },
                onThinkingLevelChange = { level -> app.settings.update { it.copy(thinkingLevel = level) } },
                onOpenModelSelector = { showModelDialog = true }
            )
        }
    }

    if (showModelDialog) {
        ModelSelectionDialog(
            app = app,
            onDismiss = { showModelDialog = false }
        )
    }

    if (showFeedbackDialog) {
        FeedbackDialog(
            onFeedback = {
                copyToClipboard("1953187487@qq.com")
                showFeedbackDialog = false
                app.settings.update { it.copy(lastFeedbackDismissTime = System.currentTimeMillis()) }
            },
            onCancel = {
                showFeedbackDialog = false
                app.settings.update { it.copy(feedbackDismissedThisSession = true) }
            },
            onDismiss = {
                showFeedbackDialog = false
                app.settings.update { it.copy(lastFeedbackDismissTime = System.currentTimeMillis()) }
            }
        )
    }

    if (showCopyMenu) {
        AlertDialog(
            onDismissRequest = { showCopyMenu = false },
            title = { Text("操作") },
            text = {
                Column {
                    Text(
                        text = "复制",
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(onClick = {
                                copyToClipboard(copyMenuText)
                                showCopyMenu = false
                            })
                            .padding(vertical = 12.dp)
                    )
                }
            },
            confirmButton = { TextButton(onClick = { showCopyMenu = false }) { Text("关闭") } }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onAddToQueue: () -> Unit,
    processing: Boolean,
    queueSize: Int,
    chatMode: ChatMode,
    thinkingLevel: Int,
    onToggleMode: () -> Unit,
    onThinkingLevelChange: (Int) -> Unit,
    onOpenModelSelector: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth().imePadding().navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
            if (queueSize > 0) {
                Text(
                    "队列中有 $queueSize 条消息",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    modifier = Modifier.height(36.dp).combinedClickable(onClick = onToggleMode),
                    shape = RoundedCornerShape(10.dp),
                    color = if (chatMode == ChatMode.DEV)
                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = if (chatMode == ChatMode.DEV) "🔧" else "🧠", fontSize = 14.sp)
                        Text(text = if (chatMode == ChatMode.DEV) "开发" else "思考", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                if (chatMode == ChatMode.THINK) {
                    Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (1..3).forEach { level ->
                            Surface(
                                modifier = Modifier.weight(1f).height(36.dp)
                                    .combinedClickable(onClick = { onThinkingLevelChange(level) }),
                                shape = RoundedCornerShape(10.dp),
                                color = if (thinkingLevel == level)
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "$level",
                                        fontSize = 12.sp,
                                        fontWeight = if (thinkingLevel == level) FontWeight.Bold else FontWeight.Normal,
                                        color = if (thinkingLevel == level) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Surface(
                    modifier = Modifier.height(36.dp).combinedClickable(onClick = onOpenModelSelector),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("📦", fontSize = 14.sp)
                        Text(text = "模型", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("输入消息，/ 使用命令…", fontSize = 14.sp) },
                    shape = RoundedCornerShape(24.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { if (text.isNotBlank()) onSubmit() }),
                    maxLines = 4,
                    enabled = !processing
                )
                Spacer(Modifier.width(8.dp))
                if (text.isNotBlank()) {
                    IconButton(onClick = onAddToQueue, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "加入队列",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                val canSend = text.isNotBlank() && !processing
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (canSend) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        )
                        .then(if (canSend) Modifier.combinedClickable(onClick = onSubmit) else Modifier),
                    contentAlignment = Alignment.Center
                ) {
                    if (processing) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "发送",
                            tint = if (canSend) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatBubble(
    msg: ChatMsg,
    style: ThemeStyle,
    streaming: Boolean = false,
    onLongPress: (String) -> Unit = {},
    onResend: (() -> Unit)? = null
) {
    val isUser = msg.role == "user"
    val isTool = msg.role == "tool"
    val glass = style == ThemeStyle.GLASS || style == ThemeStyle.LIQUID

    if (isTool) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .combinedClickable(onLongClick = { onLongPress(msg.content) }, onClick = {}),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = "🔧 ${msg.toolName ?: "工具调用"}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = msg.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🤖", fontSize = 18.sp)
            }
            Spacer(Modifier.width(8.dp))
        }
        Surface(
            shape = if (isUser) RoundedCornerShape(20.dp, 6.dp, 20.dp, 20.dp)
            else RoundedCornerShape(6.dp, 20.dp, 20.dp, 20.dp),
            color = when {
                isUser -> MaterialTheme.colorScheme.primary
                glass -> MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .combinedClickable(
                    onLongClick = {
                        onLongPress(msg.content)
                        onResend?.invoke()
                    },
                    onClick = {}
                )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (msg.toolCalls.isNotEmpty()) {
                    msg.toolCalls.forEach { call ->
                        Text(
                            text = "⚡ 调用 ${call.name}(${call.argumentsJson.take(60)}${if (call.argumentsJson.length > 60) "..." else ""})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (msg.content.isNotBlank()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 6.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    }
                }
                if (msg.content.isNotBlank()) {
                    Text(
                        text = msg.content,
                        color = if (isUser) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp
                    )
                }
                if (streaming && !isUser && msg.content.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .size(width = 2.dp, height = 16.dp)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), RoundedCornerShape(1.dp))
                    )
                }
            }
        }
        if (isUser) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyHint(apiConfigured: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🤖", fontSize = 56.sp)
        Spacer(Modifier.height(16.dp))
        Text("你好，我是 AURA", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "配置 AI 服务后开始对话\n输入 / 查看可用命令",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
        if (!apiConfigured) {
            Spacer(Modifier.height(16.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            ) {
                Text(
                    text = "尚未配置 AI 服务\n前往设置添加 API 或下载本地模型",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun FeedbackDialog(
    onFeedback: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("反馈与建议", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("如果您在使用过程中遇到问题或有任何建议，欢迎反馈给作者。", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                Text(
                    "邮箱：1953187487@qq.com",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "点击「反馈」按钮将自动复制邮箱地址到剪贴板。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = { Button(onClick = onFeedback) { Text("反馈（复制邮箱）") } },
        dismissButton = {
            Row {
                TextButton(onClick = onCancel) { Text("本次取消") }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onDismiss) { Text("一天后提醒") }
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ModelSelectionDialog(
    app: AuraApp,
    onDismiss: () -> Unit
) {
    val settings by app.settings.settings.collectAsState()
    val scope = rememberCoroutineScope()
    val allApiModels = (DefaultModelPresets + settings.customModelList).distinct()

    var selectedProvider by rememberSaveable { mutableStateOf("") }
    var providerUrl by rememberSaveable { mutableStateOf("") }
    var providerKey by rememberSaveable { mutableStateOf("") }
    var availableModels by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoadingModels by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(mapOf<String, Float>()) }

    val providers = listOf(
        "Ollama" to "http://localhost:11434/v1",
        "LM Studio" to "http://localhost:1234/v1",
        "vLLM" to "http://localhost:8000/v1",
        "OpenAI" to "https://api.openai.com/v1",
        "DeepSeek" to "https://api.deepseek.com/v1",
        "DashScope" to "https://dashscope.aliyuncs.com/compatible-mode/v1",
        "Custom" to ""
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择模型", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp)) {
                item {
                    Text("本地 AI 提供商", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        providers.chunked(3).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                row.forEach { (name, url) ->
                                    Surface(
                                        modifier = Modifier.weight(1f).height(44.dp)
                                            .combinedClickable(onClick = {
                                                selectedProvider = name
                                                providerUrl = url
                                                availableModels = emptyList()
                                            }),
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (selectedProvider == name) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text(
                                                name, fontSize = 11.sp,
                                                fontWeight = if (selectedProvider == name) FontWeight.Bold else FontWeight.Normal,
                                                maxLines = 1, overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }

                if (selectedProvider.isNotEmpty()) {
                    item {
                        OutlinedTextField(
                            value = providerUrl, onValueChange = { providerUrl = it },
                            label = { Text("API 地址") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = providerKey, onValueChange = { providerKey = it },
                            label = { Text("API Key (可选)") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    isLoadingModels = true
                                    try {
                                        val models = LlmClient(app).listModels(providerUrl, providerKey)
                                        availableModels = models
                                        app.settings.update {
                                            it.copy(
                                                apiProviderUrl = providerUrl,
                                                apiProviderKey = providerKey,
                                                apiModel = models.firstOrNull() ?: "",
                                                modelSource = "local"
                                            )
                                        }
                                    } catch (_: Exception) { availableModels = emptyList() }
                                    isLoadingModels = false
                                }
                            },
                            enabled = providerUrl.isNotBlank() && !isLoadingModels,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(if (isLoadingModels) "连接中…" else "连接并获取模型") }
                        Spacer(Modifier.height(8.dp))
                    }

                    if (availableModels.isNotEmpty()) {
                        item {
                            Text(
                                "可用模型 (${availableModels.size})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(4.dp))
                        }
                        items(availableModels) { model ->
                            val progress = downloadProgress[model]
                            Surface(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                                    .combinedClickable(onClick = {
                                        app.settings.update { it.copy(apiModel = model) }
                                        onDismiss()
                                    }),
                                shape = RoundedCornerShape(10.dp),
                                color = if (model == settings.apiModel) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(model, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                        if (progress != null) {
                                            Spacer(Modifier.height(4.dp))
                                            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(4.dp))
                                        }
                                    }
                                    if (model == settings.apiModel) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                    Text("API 模型预设", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                }

                items(allApiModels) { model ->
                    ModelItem(
                        name = model,
                        subtitle = if (model == settings.apiModel && settings.modelSource == "api") "当前使用" else null,
                        selected = model == settings.apiModel && settings.modelSource == "api",
                        onClick = {
                            app.settings.update { it.copy(apiModel = model, modelSource = "api") }
                            onDismiss()
                        }
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("管理模型") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ModelItem(
    name: String,
    subtitle: String?,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).combinedClickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (selected) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
        }
    }
}
