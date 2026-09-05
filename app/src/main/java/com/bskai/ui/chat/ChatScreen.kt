package com.bskai.ui.chat

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.bskai.data.ChatMode
import com.bskai.data.ChatRole
import com.bskai.data.DefaultModelPresets
import com.bskai.data.ThemeStyle
import com.bskai.ui.theme.ThemeBackdrop
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    app: AuraApp,
    onOpenSettings: () -> Unit
) {
    val conversation by app.agent.conversation.collectAsState()
    val processing by app.coordinator.processing.collectAsState()
    val settings by app.settings.settings.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var input by remember { mutableStateOf("") }
    var showTopMenu by remember { mutableStateOf(false) }
    var showRoleDialog by rememberSaveable { mutableStateOf(false) }
    var showModeDialog by rememberSaveable { mutableStateOf(false) }
    var showModelDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(conversation.size) {
        if (conversation.isNotEmpty()) {
            listState.animateScrollToItem(conversation.size - 1)
        }
    }

    fun submitText() {
        if (input.isBlank()) return
        app.coordinator.submit(input)
        input = ""
    }

    val style = settings.themeStyle
    val currentRole = settings.roles.firstOrNull { it.id == settings.currentRoleId } ?: settings.roles.firstOrNull()
    val currentMode = settings.modes.firstOrNull { it.id == settings.currentModeId } ?: settings.modes.firstOrNull()

    Box(modifier = Modifier.fillMaxSize()) {
        ThemeBackdrop(style = style, dark = settings.darkTheme)

        Column(modifier = Modifier.fillMaxSize()) {
            // ───── Top Bar: Role + Model + Mode ─────
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                    // Row 1: Role avatar + name + menu
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Role avatar + name (clickable)
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { showRoleDialog = true }
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.secondary
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = currentRole?.avatar ?: "🤖",
                                    fontSize = 18.sp
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentRole?.name ?: "AURA",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = currentMode?.name ?: "聊天",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        // Settings button
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "设置", modifier = Modifier.size(20.dp))
                        }
                        // More menu
                        Box {
                            IconButton(onClick = { showTopMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = null, modifier = Modifier.size(20.dp))
                            }
                            DropdownMenu(expanded = showTopMenu, onDismissRequest = { showTopMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("角色管理") },
                                    onClick = { showTopMenu = false; showRoleDialog = true }
                                )
                                DropdownMenuItem(
                                    text = { Text("模式切换") },
                                    onClick = { showTopMenu = false; showModeDialog = true }
                                )
                                DropdownMenuItem(
                                    text = { Text("当前：${BuildConfig.APP_VERSION}") },
                                    enabled = false,
                                    onClick = {}
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    // Row 2: Model selector (half width rectangle) + Mode switcher
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Model selector - half width rectangle
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clickable { showModelDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = settings.apiModel.ifBlank { "选择模型" },
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                    fontWeight = if (settings.apiModel.isNotBlank()) FontWeight.Medium else FontWeight.Normal,
                                    color = if (settings.apiModel.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Icon(
                                    Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Mode switcher - compact
                        Surface(
                            modifier = Modifier
                                .height(36.dp)
                                .clickable { showModeDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = currentMode?.icon ?: "💬",
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = currentMode?.name ?: "聊天",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                if (currentMode?.id == "think" || settings.thinkingLevel > 0) {
                                    Text(
                                        text = "·${settings.thinkingLevel.coerceAtLeast(1)}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Icon(
                                    Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // ───── Chat messages ─────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    if (conversation.isEmpty()) {
                        item { EmptyHint(settings.apiConfigured, style) }
                    } else {
                        items(conversation) { msg ->
                            val isStreaming = msg.role == "assistant" && processing &&
                                msg === conversation.lastOrNull { it.role == "assistant" } &&
                                msg.content.isNotEmpty() &&
                                conversation.last() === msg
                            ChatBubble(msg, style, streaming = isStreaming, roleAvatar = currentRole?.avatar ?: "🤖")
                        }
                    }
                }
            }

            // ───── Input bar ─────
            ChatInputBar(
                text = input,
                onTextChange = { input = it },
                onSubmit = { submitText() },
                processing = processing
            )
        }
    }

    // ───── Dialogs ─────
    if (showRoleDialog) {
        RoleManagementDialog(
            app = app,
            onDismiss = { showRoleDialog = false }
        )
    }
    if (showModeDialog) {
        ModeSelectionDialog(
            app = app,
            onDismiss = { showModeDialog = false }
        )
    }
    if (showModelDialog) {
        ModelSelectionDialog(
            app = app,
            onDismiss = { showModelDialog = false },
            onOpenSettings = {
                showModelDialog = false
                onOpenSettings()
            }
        )
    }
}

@Composable
private fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSubmit: () -> Unit,
    processing: Boolean
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        shadowElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("说点什么，或输入 / 使用命令…", fontSize = 14.sp) },
                shape = RoundedCornerShape(20.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (text.isNotBlank()) onSubmit()
                }),
                maxLines = 4,
                enabled = !processing
            )
            Spacer(Modifier.width(8.dp))
            val canSend = text.isNotBlank() && !processing
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (canSend) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    )
                    .then(if (canSend) Modifier.clickable { onSubmit() } else Modifier),
                contentAlignment = Alignment.Center
            ) {
                if (processing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "发送",
                        tint = if (canSend) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(
    msg: ChatMsg,
    style: ThemeStyle,
    compact: Boolean = false,
    streaming: Boolean = false,
    roleAvatar: String = "🤖"
) {
    val isUser = msg.role == "user"
    val glass = style == ThemeStyle.GLASS
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = roleAvatar, fontSize = 16.sp)
            }
            Spacer(Modifier.width(6.dp))
        }
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = when {
                isUser -> MaterialTheme.colorScheme.primary
                glass -> MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
            modifier = Modifier.fillMaxWidth(0.78f)
        ) {
            Column {
                if (msg.role == "tool") {
                    Text(
                        text = "🔧 ${msg.toolName ?: "工具调用"}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = msg.content,
                    modifier = Modifier.padding(
                        start = 12.dp,
                        end = 12.dp,
                        top = if (msg.role == "tool") 0.dp else 10.dp,
                        bottom = 10.dp
                    ),
                    color = if (isUser) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface,
                    fontSize = if (compact) 14.sp else 15.sp
                )
                if (streaming && !isUser && msg.content.isNotEmpty()) {
                    StreamingCursor(
                        modifier = Modifier.padding(start = 12.dp, bottom = 10.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        if (isUser) {
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun StreamingCursor(
    modifier: Modifier = Modifier,
    color: Color
) {
    val alpha by rememberInfiniteTransition(label = "cursor")
        .animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(700),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )
    Box(
        modifier = modifier
            .size(width = 2.dp, height = 14.dp)
            .background(color.copy(alpha = alpha))
    )
}

@Composable
private fun EmptyHint(apiConfigured: Boolean, style: ThemeStyle) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🤖",
            fontSize = 48.sp
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "向 AURA 说点什么",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "打字输入，选择角色和模式开始对话",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
        if (!apiConfigured) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = "尚未配置 AI 服务，前往设置添加 API 或下载本地模型",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 24.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun RoleManagementDialog(
    app: AuraApp,
    onDismiss: () -> Unit
) {
    val settings by app.settings.settings.collectAsState()
    val scope = rememberCoroutineScope()
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var createName by rememberSaveable { mutableStateOf("") }
    var createPrompt by rememberSaveable { mutableStateOf("") }
    var createAvatar by rememberSaveable { mutableStateOf("🤖") }
    var generating by remember { mutableStateOf(false) }

    val avatarOptions = listOf("🤖", "💻", "✍️", "📊", "🎓", "🔬", "🎨", "🧠", "⚡", "🌟", "🔥", "💡")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("角色管理", fontWeight = FontWeight.SemiBold) },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                items(settings.roles) { role ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                app.settings.update { it.copy(currentRoleId = role.id) }
                                onDismiss()
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (role.id == settings.currentRoleId)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = role.avatar, fontSize = 20.sp)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(role.name, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    role.systemPrompt.take(40) + if (role.systemPrompt.length > 40) "..." else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (role.id == settings.currentRoleId) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("新建")
                }
                TextButton(onClick = onDismiss) { Text("关闭") }
            }
        }
    )

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("创建角色", fontWeight = FontWeight.SemiBold) },
            text = {
                LazyColumn {
                    item {
                        Text("头像", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            avatarOptions.chunked(6).forEach { row ->
                                row.forEach { avatar ->
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (avatar == createAvatar) MaterialTheme.colorScheme.primaryContainer
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                            .clickable { createAvatar = avatar },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(avatar, fontSize = 18.sp)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = createName,
                            onValueChange = { createName = it },
                            label = { Text("角色名称") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = createPrompt,
                            onValueChange = { createPrompt = it },
                            label = { Text("角色设定（System Prompt）") },
                            minLines = 3,
                            maxLines = 5,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        if (generating) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("AI 生成中…", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            generating = true
                            val prompt = if (createPrompt.isBlank()) {
                                "请为名为「$createName」的角色生成一段简洁的 system prompt，描述其性格和能力。"
                            } else {
                                createPrompt
                            }
                            try {
                                val generatedPrompt = app.agent.generateText(prompt)
                                val role = ChatRole(
                                    id = "role_${System.currentTimeMillis()}",
                                    name = createName.ifBlank { "新角色" },
                                    avatar = createAvatar,
                                    systemPrompt = generatedPrompt.ifBlank { prompt },
                                    isAiGenerated = true
                                )
                                app.settings.update { it.copy(roles = it.roles + role) }
                            } catch (_: Exception) {
                                val role = ChatRole(
                                    id = "role_${System.currentTimeMillis()}",
                                    name = createName.ifBlank { "新角色" },
                                    avatar = createAvatar,
                                    systemPrompt = createPrompt.ifBlank { "你是一个有用的AI助手。" }
                                )
                                app.settings.update { it.copy(roles = it.roles + role) }
                            }
                            generating = false
                            showCreateDialog = false
                            createName = ""
                            createPrompt = ""
                            createAvatar = "🤖"
                        }
                    },
                    enabled = !generating && createName.isNotBlank()
                ) {
                    Text("创建")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun ModeSelectionDialog(
    app: AuraApp,
    onDismiss: () -> Unit
) {
    val settings by app.settings.settings.collectAsState()
    var showThinkingSlider by rememberSaveable { mutableStateOf(false) }
    var tempThinkingLevel by rememberSaveable { mutableIntStateOf(settings.thinkingLevel) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("模式切换", fontWeight = FontWeight.SemiBold) },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                items(settings.modes) { mode ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                app.settings.update { it.copy(currentModeId = mode.id, thinkingLevel = if (mode.id == "think") mode.thinkingLevel else it.thinkingLevel) }
                                if (mode.id == "think") {
                                    showThinkingSlider = true
                                    tempThinkingLevel = settings.thinkingLevel.coerceAtLeast(1)
                                } else {
                                    onDismiss()
                                }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (mode.id == settings.currentModeId)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(mode.icon, fontSize = 24.sp)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(mode.name, fontWeight = FontWeight.Medium)
                                Text(
                                    mode.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (mode.id == settings.currentModeId) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                if (showThinkingSlider) {
                    item {
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(12.dp))
                        Text("思考深度: $tempThinkingLevel/3", fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(4.dp))
                        Slider(
                            value = tempThinkingLevel.toFloat(),
                            onValueChange = { tempThinkingLevel = it.toInt() },
                            onValueChangeFinished = {
                                app.settings.update { it.copy(thinkingLevel = tempThinkingLevel) }
                                onDismiss()
                            },
                            valueRange = 1f..3f,
                            steps = 1
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("简要", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("标准", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("深入", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}

@Composable
private fun ModelSelectionDialog(
    app: AuraApp,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val settings by app.settings.settings.collectAsState()
    val allApiModels = (DefaultModelPresets + settings.customModelList).distinct()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择模型", fontWeight = FontWeight.SemiBold) },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                if (settings.localModels.isNotEmpty()) {
                    item {
                        Text("本地模型", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                    }
                    items(settings.localModels) { model ->
                        ModelItem(
                            name = model.name,
                            subtitle = "${model.category} · ${model.sizeBytes / 1024 / 1024}MB",
                            selected = model.id == settings.apiModel && settings.modelSource == "local",
                            onClick = {
                                app.settings.update { it.copy(apiModel = model.id, modelSource = "local") }
                                onDismiss()
                            }
                        )
                    }
                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text("API 模型", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                    }
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
        confirmButton = {
            TextButton(onClick = onOpenSettings) {
                Text("管理模型")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}

@Composable
private fun ModelItem(
    name: String,
    subtitle: String?,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
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
