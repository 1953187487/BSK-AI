package com.bskai.ui.chat

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bskai.AuraApp
import com.bskai.BuildConfig
import com.bskai.agent.ChatMsg
import com.bskai.data.DefaultModelPresets
import com.bskai.data.ThemeStyle
import com.bskai.update.GitHubApi
import com.bskai.update.RemoteRelease
import com.bskai.update.UpdateCheckResult
import com.bskai.ui.theme.ThemeBackdrop
import com.bskai.util.Permissions
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    app: AuraApp,
    onOpenSettings: () -> Unit
) {
    val conversation by app.agent.conversation.collectAsState()
    val listening by app.voice.isListening.collectAsState()
    val processing by app.coordinator.processing.collectAsState()
    val settings by app.settings.settings.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var input by remember { mutableStateOf("") }
    var showModelMenu by remember { mutableStateOf(false) }
    var showTopMenu by remember { mutableStateOf(false) }
    var voiceActive by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            app.coordinator.listenNow()
            voiceActive = true
        }
    }

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
    val hasRecordAudio = Permissions.hasRecordAudio(context)

    when (style) {
        ThemeStyle.VOICE -> VoiceLayout(
            app = app,
            conversation = conversation,
            listening = listening,
            processing = processing,
            listState = listState,
            hasRecordAudio = hasRecordAudio,
            onPermissionRequest = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
            onMicPress = {
                if (hasRecordAudio) {
                    app.coordinator.listenNow()
                    voiceActive = true
                } else {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
             onMicRelease = {
                 app.coordinator.stopListening()
                 voiceActive = false
             },
             voiceActive = voiceActive,
             onOpenTopMenu = {
                 showTopMenu = true
             },
             onOpenSettings = onOpenSettings,
             showTopMenu = showTopMenu,
             onDismissTopMenu = { showTopMenu = false }
         )
        else -> StandardLayout(
            app = app,
            conversation = conversation,
            listening = listening,
            processing = processing,
            listState = listState,
            input = input,
            onInputChange = { input = it },
            onSubmitText = { submitText() },
            onMicPress = {
                if (hasRecordAudio) {
                    app.coordinator.listenNow()
                    voiceActive = true
                } else {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
             onMicRelease = {
                 app.coordinator.stopListening()
                 voiceActive = false
             },
             voiceActive = voiceActive,
             hasRecordAudio = hasRecordAudio,
             onPermissionRequest = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
             showModelMenu = showModelMenu,
             onShowModelMenu = { showModelMenu = it },
             showTopMenu = showTopMenu,
             onShowTopMenu = { showTopMenu = it },
             onOpenSettings = onOpenSettings
         )
    }
}

// ───── Standard layout (AURORA / NEON / GLASS) ─────

@Composable
private fun StandardLayout(
    app: AuraApp,
    conversation: List<ChatMsg>,
    listening: Boolean,
    processing: Boolean,
    listState: androidx.compose.foundation.lazy.LazyListState,
    input: String,
    onInputChange: (String) -> Unit,
    onSubmitText: () -> Unit,
    onMicPress: () -> Unit,
    onMicRelease: () -> Unit,
    voiceActive: Boolean,
    hasRecordAudio: Boolean,
    onPermissionRequest: () -> Unit,
    showModelMenu: Boolean,
    onShowModelMenu: (Boolean) -> Unit,
     showTopMenu: Boolean,
     onShowTopMenu: (Boolean) -> Unit,
     onOpenSettings: () -> Unit
 ) {
     val settings by app.settings.settings.collectAsState()
     val style = settings.themeStyle

     Box(modifier = Modifier.fillMaxSize()) {
         ThemeBackdrop(style = style, dark = settings.darkTheme)

         Column(modifier = Modifier.fillMaxSize()) {
             TopHeader(
                 app = app,
                 showModelMenu = showModelMenu,
                 onShowModelMenu = onShowModelMenu,
                 showTopMenu = showTopMenu,
                 onShowTopMenu = onShowTopMenu,
                 onOpenSettings = onOpenSettings
             )

            if (!hasRecordAudio) {
                PermissionHint(text = "未授权录音权限，语音功能不可用") {
                    onPermissionRequest()
                }
            }

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
                            ChatBubble(msg, style, streaming = isStreaming)
                        }
                    }
                }
            }

            ModelPickerBar(
                app = app,
                showMenu = showModelMenu,
                onShowMenu = onShowModelMenu
            )

            InputBar(
                style = style,
                text = input,
                onTextChange = onInputChange,
                onSubmit = onSubmitText,
                onMicPress = onMicPress,
                onMicRelease = onMicRelease,
                active = voiceActive || listening || processing,
                micEnabled = hasRecordAudio
            )
        }
    }
}

// ───── Voice layout (single big mic button) ─────

@Composable
private fun VoiceLayout(
    app: AuraApp,
    conversation: List<ChatMsg>,
    listening: Boolean,
    processing: Boolean,
    listState: androidx.compose.foundation.lazy.LazyListState,
    hasRecordAudio: Boolean,
    onPermissionRequest: () -> Unit,
    onMicPress: () -> Unit,
    onMicRelease: () -> Unit,
    voiceActive: Boolean,
     onOpenTopMenu: () -> Unit,
     onOpenSettings: () -> Unit,
     showTopMenu: Boolean,
     onDismissTopMenu: () -> Unit
 ) {
     val settings by app.settings.settings.collectAsState()
     val style = settings.themeStyle

     Box(modifier = Modifier.fillMaxSize()) {
         ThemeBackdrop(style = style, dark = settings.darkTheme)

         Column(modifier = Modifier.fillMaxSize()) {
             // Slim header: just menu
             Row(
                 modifier = Modifier.fillMaxWidth().padding(8.dp),
                 verticalAlignment = Alignment.CenterVertically
             ) {
                 Spacer(Modifier.weight(1f))
                 ModelBadge(settings.apiModel.ifBlank { "未选择模型" }, settings.apiConfigured)
                 Spacer(Modifier.weight(1f))
                 Box {
                     IconButton(onClick = onOpenTopMenu) {
                         Icon(Icons.Default.MoreVert, contentDescription = null)
                     }
                     DropdownMenu(
                         expanded = showTopMenu,
                         onDismissRequest = onDismissTopMenu
                     ) {
                         DropdownMenuItem(
                             leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                             text = { Text("设置") },
                             onClick = { onDismissTopMenu(); onOpenSettings() }
                         )
                     }
                 }
             }

            if (!hasRecordAudio) {
                PermissionHint(text = "未授权录音权限，语音功能不可用") {
                    onPermissionRequest()
                }
            }

            // Compact message history (last 3)
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                if (conversation.isEmpty()) {
                    item {
                        Text(
                            text = "按住下方按钮说话",
                            modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            fontSize = 18.sp
                        )
                    }
                } else {
                    items(conversation.takeLast(3)) { msg -> ChatBubble(msg, style, compact = true) }
                }
            }

            // Big mic button
            BigMicButton(
                active = voiceActive || listening || processing,
                enabled = hasRecordAudio,
                onPress = onMicPress,
                onRelease = onMicRelease
            )
            Spacer(Modifier.height(24.dp).navigationBarsPadding())
        }
    }
}

// ───── Shared components ─────

@Composable
private fun TopHeader(
    app: AuraApp,
    showModelMenu: Boolean,
    onShowModelMenu: (Boolean) -> Unit,
    showTopMenu: Boolean,
    onShowTopMenu: (Boolean) -> Unit,
    onOpenSettings: () -> Unit
 ) {
     val settings by app.settings.settings.collectAsState()
     val currentModel = settings.apiModel.ifBlank { "未选择模型" }
     val configured = settings.apiConfigured
     var showThemeMenu by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Row(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .background(
                        if (configured) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    )
                    .clickable { onShowModelMenu(true) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.GraphicEq,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = currentModel,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
            DropdownMenu(
                expanded = showModelMenu,
                onDismissRequest = { onShowModelMenu(false) }
            ) {
                DropdownMenuItem(
                    text = { Text("本地模型", fontWeight = FontWeight.SemiBold) },
                    leadingIcon = { Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    onClick = {
                        onShowModelMenu(false)
                        onOpenSettings()
                    }
                )
                DropdownMenuItem(
                    text = { Text("外接模型 (API)", fontWeight = FontWeight.SemiBold) },
                    leadingIcon = { Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    onClick = {
                        onShowModelMenu(false)
                        onOpenSettings()
                    }
                )
                HorizontalDivider()
                val customModels = settings.customModelList
                val allModels = (DefaultModelPresets + customModels).distinct()
                allModels.forEach { model ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (model == currentModel) "$model (当前)" else model,
                                fontWeight = if (model == currentModel) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            app.settings.update { it.copy(apiModel = model) }
                            onShowModelMenu(false)
                        }
                    )
                }
                if (settings.localModels.isNotEmpty()) {
                    HorizontalDivider()
                    settings.localModels.forEach { local ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (local.id == settings.apiModel) "${local.name} (当前)" else local.name,
                                    fontWeight = if (local.id == settings.apiModel) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                app.settings.update { it.copy(apiModel = local.id, modelSource = "local") }
                                onShowModelMenu(false)
                            }
                        )
                    }
                }
                DropdownMenuItem(
                    text = { Text("去设置管理模型…", color = MaterialTheme.colorScheme.primary) },
                    onClick = {
                        onShowModelMenu(false)
                        onOpenSettings()
                    }
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Box {
            IconButton(onClick = { onShowTopMenu(true) }) {
                Icon(Icons.Default.MoreVert, contentDescription = null)
            }
            DropdownMenu(
                expanded = showTopMenu,
                onDismissRequest = { onShowTopMenu(false) }
            ) {
                // 切换主题
                Box {
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null) },
                        text = {
                            Text(
                                text = "主题：${settings.themeStyle.label}",
                                fontWeight = FontWeight.Medium
                            )
                        },
                        trailingIcon = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                        onClick = { showThemeMenu = true }
                    )
                    DropdownMenu(
                        expanded = showThemeMenu,
                        onDismissRequest = { showThemeMenu = false }
                    ) {
                        com.bskai.data.ThemeStyle.entries.forEach { style ->
                            DropdownMenuItem(
                                leadingIcon = {
                                    if (settings.themeStyle == style) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                                    } else {
                                        Spacer(Modifier.size(24.dp))
                                    }
                                },
                                text = {
                                    Column {
                                        Text(text = style.label, fontWeight = FontWeight.Medium)
                                        Text(
                                            text = style.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    app.settings.update { it.copy(themeStyle = style) }
                                    showThemeMenu = false
                                    onShowTopMenu(false)
                                }
                            )
                        }
                    }
                }
                 DropdownMenuItem(
                     leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                     text = { Text("设置") },
                     onClick = { onShowTopMenu(false); onOpenSettings() }
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

@Composable
private fun ModelPickerBar(
    app: AuraApp,
    showMenu: Boolean,
    onShowMenu: (Boolean) -> Unit
) {
    val settings by app.settings.settings.collectAsState()
    val currentModel = settings.apiModel.ifBlank { "未选择模型" }
    val configured = settings.apiConfigured
    val allModels = (DefaultModelPresets + settings.customModelList).distinct()

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(allModels) { model ->
            val selected = model == settings.apiModel
            Surface(
                shape = MaterialTheme.shapes.small,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.clickable {
                    app.settings.update { it.copy(apiModel = model) }
                }
            ) {
                Text(
                    text = model,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun ModelBadge(model: String, configured: Boolean) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (configured) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Text(
            text = model,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun InputBar(
    style: ThemeStyle,
    text: String,
    onTextChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onMicPress: () -> Unit,
    onMicRelease: () -> Unit,
    active: Boolean,
    micEnabled: Boolean
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = if (style == ThemeStyle.GLASS) 0.7f else 1f),
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
                placeholder = { Text("说点什么，点右侧说话，或输入 / 使用命令…") },
                shape = MaterialTheme.shapes.large,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (text.isNotBlank()) onSubmit()
                }),
                maxLines = 4,
                enabled = !active
            )
            Spacer(Modifier.width(6.dp))
            // 麦克风：始终在发送按钮左边
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primaryContainer
                    )
                    .then(
                        if (!active && text.isBlank() && micEnabled)
                            Modifier.pointerInput(Unit) {
                                awaitPointerEventScope {
                                    awaitFirstDown(requireUnconsumed = false)
                                    onMicPress()
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        if (event.changes.all { !it.pressed }) break
                                    }
                                    onMicRelease()
                                }
                            }
                        else if (!active && micEnabled)
                            Modifier.clickable {
                                if (micEnabled) onMicPress()
                            }
                        else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (active) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "语音输入",
                    tint = if (active) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.width(6.dp))
            // 发送按钮：始终在右侧
            val canSend = text.isNotBlank() && !active
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (canSend) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    )
                    .then(
                        if (canSend)
                            Modifier.clickable { onSubmit() }
                        else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
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

@Composable
private fun BigMicButton(
    active: Boolean,
    enabled: Boolean,
    onPress: () -> Unit,
    onRelease: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "mic-pulse")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (active) 1.18f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer ring (visible when active)
        if (active) {
            Box(
                modifier = Modifier
                    .size((160 * pulse).dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            )
        }
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(
                    if (!enabled) MaterialTheme.colorScheme.surfaceVariant
                    else if (active) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.primaryContainer
                )
                .border(
                    width = 3.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = if (active) 0.8f else 0.3f),
                    shape = CircleShape
                )
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    awaitPointerEventScope {
                        awaitFirstDown(requireUnconsumed = false)
                        onPress()
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.changes.all { !it.pressed }) break
                        }
                        onRelease()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = if (active) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "按住说话",
                    modifier = Modifier.size(64.dp),
                    tint = if (active) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (!enabled) "未授权"
                    else if (active) "聆听中…松开结束"
                    else "按住 说话",
                    color = if (active) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ChatBubble(
    msg: ChatMsg,
    style: ThemeStyle,
    compact: Boolean = false,
    streaming: Boolean = false
) {
    val isUser = msg.role == "user"
    val glass = style == ThemeStyle.GLASS
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            BubbleAvatar(letter = "A", isUser = false, style = style)
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
            Text(
                text = msg.content,
                modifier = Modifier.padding(if (compact) 10.dp else 12.dp),
                color = if (isUser) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurface,
                fontSize = if (compact) 14.sp else 15.sp
            )
            if (streaming && !isUser && msg.content.isNotEmpty()) {
                StreamingCursor(
                    modifier = Modifier.padding(end = 10.dp, bottom = if (compact) 10.dp else 12.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        if (isUser) {
            Spacer(Modifier.width(6.dp))
            BubbleAvatar(letter = "你", isUser = true, style = style)
        }
    }
}

@Composable
private fun StreamingCursor(
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color
) {
    val alpha by rememberInfiniteTransition(label = "cursor").animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .size(width = 2.dp, height = 14.dp)
            .background(color.copy(alpha = alpha))
    )
}

@Composable
private fun BubbleAvatar(letter: String, isUser: Boolean, style: ThemeStyle) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(
                if (isUser) MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.primary
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter,
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun EmptyHint(apiConfigured: Boolean, style: ThemeStyle) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.GraphicEq,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "向 AURA 说点什么",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "打字或按住右侧麦克风说话",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
        if (!apiConfigured) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = "尚未配置 AI 服务，前往设置添加 API 可获得更强的对话能力",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}

@Composable
private fun PermissionHint(text: String, action: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall
            )
            TextButton(onClick = action) { Text("授权") }
        }
    }
}

