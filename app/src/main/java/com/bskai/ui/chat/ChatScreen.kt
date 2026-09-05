package com.bskai.ui.chat

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.bskai.AuraApp
import com.bskai.BuildConfig
import com.bskai.agent.ChatMsg
import com.bskai.intent.SkillEngine
import com.bskai.update.RemoteRelease
import com.bskai.update.UpdateCheckResult
import com.bskai.update.GitHubApi
import com.bskai.util.Permissions
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    app: AuraApp,
    onShowUpdate: (UpdateCheckResult) -> Unit,
    onShowHistory: (List<RemoteRelease>) -> Unit
) {
    val conversation by app.agent.conversation.collectAsState()
    val listening by app.voice.isListening.collectAsState()
    val processing by app.coordinator.processing.collectAsState()
    val settings by app.settings.settings.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var input by remember { mutableStateOf("") }
    var showModelMenu by remember { mutableStateOf(false) }
    var showTopMenu by remember { mutableStateOf(false) }
    var checkingUpdate by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            app.coordinator.listenNow()
        }
    }

    LaunchedEffect(conversation.size) {
        if (conversation.isNotEmpty()) {
            listState.animateScrollToItem(conversation.size - 1)
        }
    }

    fun submit() {
        if (input.isBlank()) return
        app.coordinator.submit(input)
        input = ""
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(
            app = app,
            showModelMenu = showModelMenu,
            onShowModelMenu = { showModelMenu = it },
            showTopMenu = showTopMenu,
            onShowTopMenu = { showTopMenu = it },
            onCheckUpdate = {
                if (checkingUpdate) return@TopBar
                checkingUpdate = true
                coroutineScope.launch {
                    val releases = GitHubApi.listReleases()
                    checkingUpdate = false
                    val currentCode = BuildConfig.BUILD_NUMBER
                    val parsed = releases
                        .filter { it.versionCode > 0 }
                        .sortedByDescending { it.versionCode }
                    val latest = parsed.firstOrNull()
                    val hasUpdate = latest != null && latest.versionCode > currentCode
                    onShowUpdate(
                        UpdateCheckResult(
                            releases = parsed,
                            latestRelease = latest,
                            hasUpdate = hasUpdate
                        )
                    )
                }
            },
            onShowHistory = {
                coroutineScope.launch {
                    val releases = GitHubApi.listReleases()
                    onShowHistory(
                        releases
                            .filter { it.versionCode > 0 }
                            .sortedByDescending { it.versionCode }
                    )
                }
            }
        )

        if (!Permissions.hasRecordAudio(context)) {
            PermissionHint(text = "未授权录音权限，语音功能不可用") {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                if (conversation.isEmpty()) {
                    item {
                        EmptyHint(settings.apiConfigured)
                    }
                } else {
                    items(conversation) { msg ->
                        ChatBubble(msg)
                    }
                }
            }
        }

        SkillChipsRow(
            engine = app.skills,
            onSelect = { sample ->
                if (input.isBlank()) input = sample else input = "$input，$sample"
            }
        )

        InputBar(
            text = input,
            onTextChange = { input = it },
            onSubmit = { submit() },
            onVoicePress = {
                if (Permissions.hasRecordAudio(context)) {
                    app.coordinator.listenNow()
                } else {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            onVoiceRelease = { app.coordinator.stopListening() },
            listening = listening,
            processing = processing,
            voiceEnabled = Permissions.hasRecordAudio(context)
        )
    }
}

@Composable
private fun TopBar(
    app: AuraApp,
    showModelMenu: Boolean,
    onShowModelMenu: (Boolean) -> Unit,
    showTopMenu: Boolean,
    onShowTopMenu: (Boolean) -> Unit,
    onCheckUpdate: () -> Unit,
    onShowHistory: () -> Unit
) {
    val settings by app.settings.settings.collectAsState()
    val currentModel = settings.apiModel.ifBlank { "未选择模型" }
    val configured = settings.apiConfigured

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (configured) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp),
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
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(
                    onClick = { onShowModelMenu(true) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            DropdownMenu(
                expanded = showModelMenu,
                onDismissRequest = { onShowModelMenu(false) }
            ) {
                com.bskai.data.DefaultModelPresets.forEach { model ->
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
                DropdownMenuItem(
                    text = { Text("自定义…", color = MaterialTheme.colorScheme.primary) },
                    onClick = {
                        onShowModelMenu(false)
                        // 由设置页配置
                    }
                )
            }
        }

        Box {
            IconButton(onClick = { onShowTopMenu(true) }) {
                Icon(Icons.Default.MoreVert, contentDescription = null)
            }
            DropdownMenu(
                expanded = showTopMenu,
                onDismissRequest = { onShowTopMenu(false) }
            ) {
                DropdownMenuItem(
                    leadingIcon = { Icon(Icons.Default.SystemUpdateAlt, contentDescription = null) },
                    text = { Text("检查更新") },
                    onClick = {
                        onShowTopMenu(false)
                        onCheckUpdate()
                    }
                )
                DropdownMenuItem(
                    leadingIcon = { Icon(Icons.Default.History, contentDescription = null) },
                    text = { Text("历史版本") },
                    onClick = {
                        onShowTopMenu(false)
                        onShowHistory()
                    }
                )
                DropdownMenuItem(
                    leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) },
                    text = { Text("当前版本：${BuildConfig.APP_VERSION}") },
                    enabled = false,
                    onClick = {}
                )
            }
        }
    }
}

@Composable
private fun SkillChipsRow(
    engine: SkillEngine,
    onSelect: (String) -> Unit
) {
    val skills = remember { engine.skillsList() }
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(skills) { skill ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.pointerInput(skill.id) {
                    awaitPointerEventScope {
                        awaitFirstDown(requireUnconsumed = false)
                        onSelect(skill.description)
                    }
                }
            ) {
                Text(
                    text = skill.label,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun InputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onVoicePress: () -> Unit,
    onVoiceRelease: () -> Unit,
    listening: Boolean,
    processing: Boolean,
    voiceEnabled: Boolean
) {
    val active = listening || processing
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().imePadding().navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("说点什么，或点右侧说话…") },
                shape = RoundedCornerShape(22.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                maxLines = 4,
                enabled = !processing
            )
            Spacer(Modifier.width(8.dp))
            if (text.isNotBlank()) {
                IconButton(
                    onClick = onSubmit,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "发送",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (active) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.primaryContainer
                        )
                        .pointerInput(voiceEnabled) {
                            if (!voiceEnabled) return@pointerInput
                            awaitPointerEventScope {
                                awaitFirstDown(requireUnconsumed = false)
                                onVoicePress()
                                while (true) {
                                    val event = awaitPointerEvent()
                                    if (event.changes.all { !it.pressed }) break
                                }
                                onVoiceRelease()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (active) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "按住说话",
                        tint = if (active) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(msg: ChatMsg) {
    val isUser = msg.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            BubbleAvatar(letter = "A", isUser = false)
            Spacer(Modifier.width(6.dp))
        }
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(0.78f)
        ) {
            Text(
                text = msg.content,
                modifier = Modifier.padding(12.dp),
                color = if (isUser) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurface
            )
        }
        if (isUser) {
            Spacer(Modifier.width(6.dp))
            BubbleAvatar(letter = "你", isUser = true)
        }
    }
}

@Composable
private fun BubbleAvatar(letter: String, isUser: Boolean) {
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
private fun EmptyHint(apiConfigured: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.GraphicEq,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "向 AURA 说点什么",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "按下方技能快速开始，或直接输入",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(12.dp))
        if (!apiConfigured) {
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
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
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

@Composable
private fun rememberCoroutineScope() = androidx.compose.runtime.rememberCoroutineScope()
