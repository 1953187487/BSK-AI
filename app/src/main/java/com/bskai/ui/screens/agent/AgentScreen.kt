package com.bskai.ui.screens.agent

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
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bskai.ui.components.PermissionDialog
import com.bskai.ui.theme.MonoFont

@Composable
fun AgentScreen(viewModel: AgentViewModel = viewModel()) {
    val items by viewModel.items.collectAsState()
    val streaming by viewModel.streaming.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val permission by viewModel.permission.collectAsState()
    val input by viewModel.input.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(items.size, streaming) {
        val count = items.size + if (streaming.isNotEmpty()) 1 else 0
        if (count > 0) listState.animateScrollToItem(count - 1)
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().imePadding()) {
            AgentHeader(isRunning) { viewModel.stop() }
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
}

@Composable
private fun AgentHeader(isRunning: Boolean, onStop: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("BSK Agent \u7ec8\u7aef", style = MaterialTheme.typography.titleLarge)
            Text(
                if (isRunning) "\u667a\u80fd\u4f53\u8fd0\u884c\u4e2d..." else "Claude Code \u98ce\u683c\u7f16\u7801\u667a\u80fd\u4f53",
                style = MaterialTheme.typography.labelMedium,
                color = if (isRunning) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
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
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
