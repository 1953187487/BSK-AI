package com.bskai.ui.screens.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bskai.ui.components.PermissionDialog
import com.bskai.ui.screens.agent.AgentUiItem
import com.bskai.ui.screens.agent.AgentViewModel
import com.bskai.ui.theme.BskGlassCard
import com.bskai.ui.theme.MonoFont
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentScreen(viewModel: AgentViewModel = viewModel()) {
    val items by viewModel.items.collectAsState()
    val streaming by viewModel.streaming.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val permission by viewModel.permission.collectAsState()
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var canSend by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(items.size, streaming) {
        val count = items.size + if (streaming.isNotEmpty()) 1 else 0
        if (count > 0) listState.animateScrollToItem(count - 1)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = {},
                    onDragCancel = {},
                    onDragEnd = {},
                    onHorizontalDrag = { _, _ -> }
                )
            }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "BSK AI",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isRunning) "Agent running..." else "Claude Code style coding assistant",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .background(Color(0xFF0A0914))
            ) {
                // Message list
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (items.isEmpty() && streaming.isEmpty()) {
                        item {
                            WelcomeCard()
                        }
                    }
                    items(items, key = { it.hashCode() }) { item ->
                        when (item) {
                            is AgentUiItem.User -> UserBubble(item.text)
                            is AgentUiItem.Assistant -> AssistantBubble(item.text)
                            is AgentUiItem.ToolItem -> ToolCard(item)
                            is AgentUiItem.SystemMsg -> SystemMsgBubble(item)
                        }
                    }
                    if (streaming.isNotEmpty()) {
                        item { AssistantBubble(streaming + " |") }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }

                // Input bar
                InputBar(
                    value = input,
                    onValueChange = { input = it },
                    onSend = {
                        if (input.isNotBlank() && canSend) {
                            viewModel.send(input.trim())
                            input = ""
                            canSend = false
                            scope.launch { delay(500); canSend = true }
                        }
                    },
                    enabled = !isRunning
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
}

@Composable
private fun WelcomeCard() {
    BskGlassCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), Color.Transparent),
                            center = Offset(0.5f, 0.5f),
                            radius = 160f
                        ),
                        RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("\u26A1", fontSize = 24.sp)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = "BSK AI v1.0.8",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Enter a task description and the agent will execute it",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun UserBubble(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Card(
            modifier = Modifier.widthIn(max = 300.dp),
            shape = RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun AssistantBubble(text: String) {
    BskGlassCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = MonoFont
        )
    }
}

@Composable
private fun ToolCard(item: AgentUiItem.ToolItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = if (item.running) "\u25B6 ${item.name}" else "\u2713 ${item.name}",
                style = MaterialTheme.typography.labelSmall,
                color = if (item.running) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary,
                fontFamily = MonoFont
            )
            if (!item.output.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = item.output!!.take(400),
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
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        color = if (item.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
        style = MaterialTheme.typography.labelSmall,
        fontFamily = MonoFont,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun InputBar(value: String, onValueChange: (String) -> Unit, onSend: () -> Unit, enabled: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .background(
                    Brush.linearGradient(listOf(Color(0x301A1730), Color(0x201A1730))),
                    RoundedCornerShape(14.dp)
                )
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            textStyle = androidx.compose.ui.text.TextStyle(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp
            ),
            maxLines = 4
        )
        Spacer(Modifier.width(8.dp))
        IconButton(
            onClick = onSend,
            enabled = enabled && value.isNotBlank(),
            modifier = Modifier
                .background(
                    Brush.radialGradient(
                        colors = listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), Color.Transparent),
                        center = Offset(0.5f, 0.5f),
                        radius = 96f
                    ),
                    androidx.compose.foundation.shape.CircleShape
                )
        ) {
            Icon(
                Icons.Outlined.Send,
                contentDescription = "Send",
                tint = if (enabled && value.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
