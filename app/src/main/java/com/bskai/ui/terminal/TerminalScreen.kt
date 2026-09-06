package com.bskai.ui.terminal

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bskai.permission.ShizukuBridge
import com.bskai.terminal.TerminalEngine
import kotlinx.coroutines.launch

@Composable
fun TerminalScreen(
    engine: TerminalEngine,
    shizuku: ShizukuBridge?,
    onClose: () -> Unit
) {
    val backend by engine.backend.collectAsState()
    val shizukuState = shizuku?.state?.collectAsState()?.value ?: ShizukuBridge.State.UNAVAILABLE
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val history = remember { mutableStateListOf<HistoryLine>() }
    var input by remember { mutableStateOf("") }

    fun run() {
        val cmd = input.trim()
        if (cmd.isEmpty()) return
        input = ""
        history.add(HistoryLine(prompt = "$", command = cmd))
        scope.launch {
            val r = engine.execute(cmd)
            history.add(HistoryLine(
                prompt = "${r.backend.name.lowercase()}:${r.exitCode}",
                output = if (r.stdout.isNotEmpty()) r.stdout else r.stderr.ifEmpty { "(no output)" },
                isError = r.exitCode != 0
            ))
        }
    }

    LaunchedEffect(history.size) {
        if (history.isNotEmpty()) listState.animateScrollToItem(history.size - 1)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .navigationBarsPadding()
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "内置终端",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "${TerminalEngine.Backend.entries.size} 后端",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "关闭") }
        }

        // Backend selector
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TerminalEngine.Backend.entries.forEach { b ->
                val enabled = when (b) {
                    TerminalEngine.Backend.LOCAL -> true
                    TerminalEngine.Backend.SHIZUKU -> shizukuState == ShizukuBridge.State.GRANTED
                    TerminalEngine.Backend.ROOT -> true
                }
                FilterChip(
                    selected = backend == b,
                    onClick = { engine.setBackend(b) },
                    enabled = enabled,
                    label = { Text(b.name) }
                )
            }
            if (shizukuState == ShizukuBridge.State.NEED_PERMISSION) {
                AssistChip(
                    onClick = { shizuku?.requestPermission() },
                    label = { Text("授权 Shizuku") },
                    leadingIcon = { Icon(Icons.Default.Security, contentDescription = null) }
                )
            } else if (shizukuState == ShizukuBridge.State.UNAVAILABLE) {
                Text(
                    text = "Shizuku 未安装，仅本地后端可用",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

        // Output area
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(12.dp)
            ) {
                if (history.isEmpty()) {
                    item {
                        Text(
                            text = "欢迎使用 AURA 内置终端\n\n" +
                                "• LOCAL 后端：仅本应用权限\n" +
                                "• SHIZUKU 后端：经 Shizuku 提权（无需 root）\n" +
                                "• ROOT 后端：直接以 root 身份执行\n\n" +
                                "提示：危险命令（rm -rf /、mkfs、shutdown 等）会被拒绝。\n" +
                                "AI 也可通过 run_shell 工具调用本终端，每次执行会写入审计日志。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                items(history) { line -> HistoryLineView(line) }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

        // Input row
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$ ",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("输入命令...") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { run() })
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = { run() }) {
                Icon(Icons.Default.PlayArrow, contentDescription = "执行")
            }
        }
    }
}

@Composable
private fun HistoryLineView(line: HistoryLine) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = "${line.prompt} ${line.command}",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        if (line.output.isNotEmpty()) {
            Text(
                text = line.output,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = if (line.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

data class HistoryLine(
    val prompt: String = "",
    val command: String = "",
    val output: String = "",
    val isError: Boolean = false
)
