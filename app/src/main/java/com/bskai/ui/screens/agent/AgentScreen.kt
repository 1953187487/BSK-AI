package com.bskai.ui.screens.agent

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bskai.core.settings.AgentWorkspaceConfig
import com.bskai.core.settings.WorkspaceMode
import com.bskai.ui.components.PermissionDialog
import com.bskai.ui.theme.MonoFont
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AgentScreen(viewModel: AgentViewModel = viewModel()) {
    val items by viewModel.items.collectAsState()
    val streaming by viewModel.streaming.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val permission by viewModel.permission.collectAsState()
    val input by viewModel.input.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val workspace by viewModel.workspace.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    var showSessions by remember { mutableStateOf(false) }
    var showWorkspace by remember { mutableStateOf(false) }

    val treeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            viewModel.applyWorkspace(
                AgentWorkspaceConfig(mode = WorkspaceMode.SAF, treeUri = uri.toString())
            )
        }
    }

    LaunchedEffect(items.size, streaming) {
        val count = items.size + if (streaming.isNotEmpty()) 1 else 0
        if (count > 0) listState.animateScrollToItem(count - 1)
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().imePadding()) {
            AgentHeader(
                isRunning = isRunning,
                workspaceLabel = workspace.displayName,
                onStop = { viewModel.stop() },
                onNewSession = { viewModel.newSession() },
                onShowSessions = { showSessions = true },
                onShowWorkspace = { showWorkspace = true }
            )
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items, key = { it.hashCode() }) { item ->
                    when (item) {
                        is AgentUiItem.User -> UserBubble(item.text)
                        is AgentUiItem.Assistant -> AssistantBubble(item.text)
                        is AgentUiItem.ToolItem -> ToolCard(item)
                        is AgentUiItem.SystemMsg -> SystemMsgBubble(item)
                    }
                }
                if (streaming.isNotEmpty()) {
                    item { AssistantBubble(streaming + " \u258c") }
                    // 流动式输出指示：半透明液态玻璃提示
                    item {
                        Text(
                            text = "流动式输出中...",
                            modifier = Modifier.padding(horizontal = 8.dp).background(
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f),
                                RoundedCornerShape(12.dp)
                            ).padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = MonoFont
                        )
                    }
                }
            }
            AgentInputBar(
                value = input,
                enabled = !isRunning,
                onValueChange = viewModel::onInputChange,
                onSend = { viewModel.send(it) }
            )
        }
    }

    permission?.let { req ->
        PermissionDialog(
            toolName = req.toolName,
            args = req.args,
            onAllow = { viewModel.resolvePermission(true) },
            onDeny = { viewModel.resolvePermission(false) }
        )
    }

    if (showSessions) {
        SessionPickerDialog(
            sessions = sessions,
            onDismiss = { showSessions = false },
            onLoad = { viewModel.loadSession(it) },
            onDelete = { viewModel.deleteSession(it) }
        )
    }

    if (showWorkspace) {
        WorkspaceDialog(
            workspaceLabel = workspace.displayName,
            onDismiss = { showWorkspace = false },
            onPick = {
                showWorkspace = false
                treeLauncher.launch(null)
            },
            onReset = {
                viewModel.resetWorkspace()
                showWorkspace = false
            }
        )
    }
}

@Composable
private fun AgentHeader(
    isRunning: Boolean,
    workspaceLabel: String,
    onStop: () -> Unit,
    onNewSession: () -> Unit,
    onShowSessions: () -> Unit,
    onShowWorkspace: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("BSK Agent \u7ec8\u7aef", style = MaterialTheme.typography.titleLarge)
            Text(
                text = if (isRunning) "\u667a\u80fd\u4f53\u8fd0\u884c\u4e2d..." else workspaceLabel,
                style = MaterialTheme.typography.labelMedium,
                color = if (isRunning) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onNewSession) {
            Icon(Icons.Outlined.Add, contentDescription = "\u65b0\u5efa\u4f1a\u8bdd")
        }
        IconButton(onClick = onShowSessions) {
            Icon(Icons.Outlined.History, contentDescription = "\u4f1a\u8bdd\u5217\u8868")
        }
        IconButton(onClick = onShowWorkspace) {
            Icon(Icons.Outlined.FolderOpen, contentDescription = "\u5de5\u4f5c\u533a")
        }
        if (isRunning) {
            IconButton(onClick = onStop) {
                Icon(Icons.Outlined.StopCircle, contentDescription = "\u505c\u6b62", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun UserBubble(text: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Text(
            text = text,
            modifier = Modifier
                .widthIn(max = 320.dp)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f), RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.95f),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun AssistantBubble(text: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Text(
            text = text,
            modifier = Modifier
                .widthIn(max = 340.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = MonoFont
        )
    }
}

@Composable
private fun ToolCard(item: AgentUiItem.ToolItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = if (item.running) "\u25b6 ${item.name}" else "\u2713 ${item.name}",
                style = MaterialTheme.typography.labelMedium,
                color = if (item.running) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary,
                fontFamily = MonoFont
            )
            if (!item.output.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = item.output!!.take(500),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = MonoFont
                )
            }
        }
    }
}

@Composable
private fun SystemMsgBubble(item: AgentUiItem.SystemMsg) {
    Text(
        text = item.text,
        modifier = Modifier.fillMaxWidth().padding(4.dp),
        color = if (item.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
        style = MaterialTheme.typography.bodyMedium,
        fontFamily = MonoFont
    )
}

@Composable
private fun AgentInputBar(
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    onSend: (String) -> Unit
) {
    var text by remember(value) { mutableStateOf(value) }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.weight(1f),
            placeholder = { Text("\u8f93\u5165\u4efb\u52a1\uff0c\u5982\uff1a\u521b\u5efa\u4e00\u4e2a\u8ba1\u7b97\u5668\u9879\u76ee\u5e76\u6784\u5efa") },
            maxLines = 4
        )
        Spacer(Modifier.width(8.dp))
        Button(onClick = { onSend(text) }, enabled = enabled && text.isNotBlank()) {
            Text("\u53d1\u9001")
        }
    }
}

@Composable
private fun SessionPickerDialog(
    sessions: List<com.bskai.core.session.SessionMeta>,
    onDismiss: () -> Unit,
    onLoad: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("\u4f1a\u8bdd\u5217\u8868") },
        text = {
            if (sessions.isEmpty()) {
                Text("\u8fd8\u6ca1\u6709\u4f1a\u8bdd\uff0c\u53d1\u9001\u4e00\u6761\u6d88\u606f\u5f00\u59cb\u3002")
            } else {
                Column {
                    sessions.forEachIndexed { i, s ->
                        if (i > 0) HorizontalDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(s.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(s.updatedAt)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = { onLoad(s.id); onDismiss() }) { Text("\u6253\u5f00") }
                            IconButton(onClick = { onDelete(s.id) }) {
                                Icon(Icons.Outlined.Delete, contentDescription = "\u5220\u9664", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("\u5173\u95ed") }
        }
    )
}

@Composable
private fun WorkspaceDialog(
    workspaceLabel: String,
    onDismiss: () -> Unit,
    onPick: () -> Unit,
    onReset: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("\u5de5\u4f5c\u533a") },
        text = {
            Column {
                Text("\u5f53\u524d\u5de5\u4f5c\u533a\uff1a", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    workspaceLabel,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = MonoFont,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "\u5207\u6362\u5230\u5916\u90e8\u76ee\u5f55\u540e\uff0cshell / \u521b\u5efa\u9879\u76ee / \u6784\u5efa APK \u5c06\u6682\u4e0d\u53ef\u7528\u3002",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Row {
                OutlinedButton(onClick = onReset) { Text("\u6062\u590d\u9ed8\u8ba4") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = onPick) { Text("\u9009\u62e9\u76ee\u5f55") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("\u53d6\u6d88") }
        }
    )
}
