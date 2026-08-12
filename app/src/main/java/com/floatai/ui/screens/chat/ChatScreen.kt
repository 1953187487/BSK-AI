package com.floatai.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.floatai.App
import com.floatai.data.model.ChatHistory
import com.floatai.data.model.ChatMessage
import com.floatai.ui.i18n.localStrings
import com.floatai.ui.screens.api.ApiManagementSheet
import com.floatai.ui.screens.character.CharacterAvatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.applicationContext as App
    val vm: ChatViewModel = viewModel(
        key = "chat",
        factory = ChatViewModel.factory(app.settingsRepository, app.chatRepository, app.characterRepository)
    )

    val strings = localStrings()
    val messages by vm.messages.collectAsStateWithLifecycle()
    val input by vm.input.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val model by vm.model.collectAsStateWithLifecycle()
    val availableModels by vm.availableModels.collectAsStateWithLifecycle()
    val histories by vm.histories.collectAsStateWithLifecycle()

    var showHistory by remember { mutableStateOf(false) }
    var showModelPicker by remember { mutableStateOf(false) }
    var showApiManagement by remember { mutableStateOf(false) }
    var showCharacter by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // 任意 sheet 打开时强制收起其他 sheet，避免重叠 / 点击穿透
    LaunchedEffect(showHistory, showModelPicker, showApiManagement) {
        if (showHistory && (showModelPicker || showApiManagement)) {
            showHistory = false
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxSize()
    ) {
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        showCharacter = true
                    }
                ) {
                    CharacterAvatar(
                        character = app.characterRepository.characters.value
                            .firstOrNull { it.id == vm.activeCharacter.value }
                            ?: com.floatai.data.model.DEFAULT_CHARACTER,
                        size = 36.dp
                    )
                    Spacer(Modifier.size(8.dp))
                    Column {
                        Text(
                            vm.characters.value.firstOrNull { it.id == vm.activeCharacter.value }?.name
                                ?: strings.chat_title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(model.ifBlank { "auto" },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.size(2.dp))
                            Icon(
                                Icons.Filled.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            },
            actions = {
                IconButton(onClick = {
                    runCatching {
                        app.startService(
                            android.content.Intent(app, com.floatai.service.FloatService::class.java)
                        )
                    }.onFailure {
                        android.widget.Toast.makeText(
                            app,
                            "启动悬浮窗失败：${it.message}",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }) {
                    Icon(androidx.compose.material.icons.Icons.Filled.OpenInNew, contentDescription = "启动悬浮窗")
                }
                IconButton(onClick = { showHistory = true }) {
                    Icon(Icons.Filled.History, contentDescription = strings.chat_history)
                }
                IconButton(onClick = { vm.clearChat() }) {
                    Icon(Icons.Filled.Delete, contentDescription = strings.chat_clear)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            strings.chat_empty,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            items(messages, key = { "${it.role}-${it.timestamp}" }) { msg ->
                ChatBubble(msg)
            }
        }

        ChatInputBar(
            value = input,
            onValueChange = vm::setInput,
            onSend = vm::send,
            loading = loading
        )
    }

    if (showHistory) {
        HistorySheet(
            histories = histories,
            onDismiss = { showHistory = false },
            onSelect = { history ->
                vm.loadHistory(history)
                showHistory = false
            },
            onDelete = vm::deleteHistory,
            onNewChat = {
                vm.newChat()
                showHistory = false
            },
            onClearAll = vm::clearAllHistories
        )
    }

    if (showModelPicker) {
        ModelPickerSheet(
            models = availableModels,
            selected = model,
            onSelect = {
                vm.selectModel(it)
            },
            onManageModels = {
                showModelPicker = false
                showApiManagement = true
            },
            onDismiss = { showModelPicker = false }
        )
    }

    if (showApiManagement) {
        ApiManagementSheet(onDismiss = {
            showApiManagement = false
            // 关闭后从 SettingsRepository 重新加载可用模型
            val list = app.settingsRepository.apiConfig.value.models
            vm.updateAvailableModels(list)
        })
    }

    if (showCharacter) {
        // 全屏覆盖：角色管理界面（独立于 AI 聊天）
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.ui.platform.LocalContext provides context
        ) {
            com.floatai.ui.screens.character.CharacterScreen(onBack = {
                showCharacter = false
            })
        }
    }
}

/**
 * 顶栏上点击展开模型列表的触发按钮。
 */
@Composable
private fun ModelPickerTrigger(selected: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(start = 8.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            selected,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 140.dp)
        )
        Icon(
            Icons.Filled.KeyboardArrowDown,
            contentDescription = "展开模型列表",
            modifier = Modifier.size(18.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelPickerSheet(
    models: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    onManageModels: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = localStrings()
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(strings.chat_select_model, style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onDismiss) { Text("✕") }
            }
            Text(
                if (models.isEmpty()) strings.chat_no_models else "${models.size} models",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LazyColumn(modifier = Modifier.heightIn(max = 460.dp)) {
                items(models, key = { it }) { m ->
                    val isSelected = m == selected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                else MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                onSelect(m)
                                onDismiss()
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            m,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Text(
                                strings.api_selected,
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
                // 末尾「管理模型」入口
                item(key = "__manage_models__") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                onManageModels()
                            }
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            strings.chat_manage_models,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            "+",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(msg: ChatMessage) {
    val isUser = msg.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .background(
                    if (isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                    else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isUser) 18.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 18.dp
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .fillMaxWidth(0.82f)
        ) {
            Text(
                msg.content,
                color = if (isUser) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    loading: Boolean
) {
    val strings = localStrings()
    val context = androidx.compose.ui.platform.LocalContext.current
    var recording by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val recognizerHolder = androidx.compose.runtime.remember {
        object {
            var instance: com.floatai.voice.VoiceRecognizer? = null
        }
    }
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            recognizerHolder.instance?.stop()
            recognizerHolder.instance = null
        }
    }

    fun toggleVoice() {
        if (recording) {
            recognizerHolder.instance?.stop()
            recognizerHolder.instance = null
            recording = false
        } else {
            val r = com.floatai.voice.VoiceRecognizer(
                context = context,
                onPartial = { partial ->
                    if (partial.isNotEmpty()) onValueChange(partial)
                },
                onResult = { text ->
                    onValueChange(text)
                    recording = false
                    recognizerHolder.instance?.stop()
                    recognizerHolder.instance = null
                },
                onError = { _ ->
                    recording = false
                    recognizerHolder.instance?.stop()
                    recognizerHolder.instance = null
                }
            )
            recognizerHolder.instance = r
            r.start()
            recording = true
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                RoundedCornerShape(22.dp)
            )
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        // 语音输入按钮
        IconButton(
            onClick = { toggleVoice() },
            enabled = !loading,
            modifier = Modifier.padding(end = 2.dp)
        ) {
            Icon(
                imageVector = if (recording) Icons.Filled.Stop else Icons.Filled.Mic,
                contentDescription = strings.chat_voice_input,
                tint = if (recording) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary
            )
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    if (recording) strings.chat_voice_recording
                    else strings.chat_placeholder
                )
            },
            modifier = Modifier.weight(1f),
            maxLines = 4,
            enabled = !loading && !recording,
            shape = RoundedCornerShape(18.dp),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent
            )
        )
        IconButton(
            onClick = onSend,
            enabled = !loading && value.isNotBlank(),
            modifier = Modifier.padding(start = 4.dp)
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = strings.chat_send,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorySheet(
    histories: List<ChatHistory>,
    onDismiss: () -> Unit,
    onSelect: (ChatHistory) -> Unit,
    onDelete: (ChatHistory) -> Unit,
    onNewChat: () -> Unit,
    onClearAll: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("历史对话", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onNewChat) { Text("新对话") }
            }
            if (histories.isEmpty()) {
                Text(
                    "暂无历史记录",
                    modifier = Modifier.fillMaxWidth().padding(40.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                items(histories, key = { it.id }) { history ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(history) }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                history.title,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "${history.messages.size} 条消息",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onDelete(history) }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "删除",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
            if (histories.isNotEmpty()) {
                TextButton(
                    onClick = onClearAll,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                ) { Text("清空所有记录", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}
