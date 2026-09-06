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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bskai.permission.ShizukuBridge
import com.bskai.terminal.TerminalEngine
import kotlinx.coroutines.launch

private val COLOR_BG = Color(0xFF0D1117)
private val COLOR_SURFACE = Color(0xFF161B22)
private val COLOR_DIVIDER = Color(0xFF21262D)
private val COLOR_TEXT = Color(0xFFE6EDF3)
private val COLOR_MUTED = Color(0xFF8B949E)
private val COLOR_GREEN = Color(0xFF3FB950)
private val COLOR_BLUE = Color(0xFF58A6FF)
private val COLOR_RED = Color(0xFFF85149)
private val COLOR_INPUT_BORDER = Color(0xFF30363D)
private val COLOR_PLACEHOLDER = Color(0xFF484F58)

@Composable
fun TerminalScreen(
    engine: TerminalEngine,
    shizuku: ShizukuBridge?
) {
    val backend by engine.backend.collectAsState()
    val shizukuState = shizuku?.state?.collectAsState()?.value ?: ShizukuBridge.State.UNAVAILABLE
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val history = remember { mutableStateListOf<HistoryLine>() }
    var input by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current

    fun run() {
        val cmd = input.trim()
        if (cmd.isEmpty()) return
        input = ""
        history.add(HistoryLine(prompt = "$", command = cmd))
        scope.launch {
            val r = engine.execute(cmd)
            history.add(HistoryLine(
                prompt = r.backend.name.lowercase() + ":" + r.exitCode,
                output = if (r.stdout.isNotEmpty()) r.stdout else r.stderr.ifEmpty { "(no output)" },
                isError = r.exitCode != 0
            ))
        }
    }

    fun clearHistory() {
        history.clear()
    }

    fun copyAll() {
        val sb = StringBuilder()
        for (line in history) {
            if (line.command.isNotEmpty()) sb.append(line.prompt + " " + line.command)
            if (line.output.isNotEmpty()) {
                if (sb.isNotEmpty()) sb.append("\n")
                sb.append(line.output)
            }
            sb.append("\n")
        }
        clipboard.setText(AnnotatedString(sb.toString()))
    }

    LaunchedEffect(history.size) {
        if (history.isNotEmpty()) listState.scrollToItem(history.size - 1)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(COLOR_BG)
            .imePadding()
            .navigationBarsPadding()
    ) {
        Surface(
            color = COLOR_SURFACE,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AURA Terminal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = COLOR_TEXT,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = backend.name.lowercase(),
                    style = MaterialTheme.typography.bodySmall,
                    color = when (backend) {
                        TerminalEngine.Backend.LOCAL -> COLOR_GREEN
                        TerminalEngine.Backend.SHIZUKU -> COLOR_BLUE
                        TerminalEngine.Backend.ROOT -> COLOR_RED
                    },
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { copyAll() }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "复制全部", modifier = Modifier.size(18.dp), tint = COLOR_MUTED)
                }
                IconButton(onClick = { clearHistory() }) {
                    Icon(Icons.Default.ClearAll, contentDescription = "清空", modifier = Modifier.size(18.dp), tint = COLOR_MUTED)
                }
            }
        }

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
                    label = { Text(b.name, fontSize = 11.sp) }
                )
            }
            if (shizukuState == ShizukuBridge.State.NEED_PERMISSION) {
                AssistChip(
                    onClick = { shizuku?.requestPermission() },
                    label = { Text("授权 Shizuku", fontSize = 11.sp) },
                    leadingIcon = { Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
            } else if (shizukuState == ShizukuBridge.State.UNAVAILABLE) {
                Text(
                    text = "Shizuku 未安装",
                    style = MaterialTheme.typography.bodySmall,
                    color = COLOR_MUTED
                )
            }
        }

        HorizontalDivider(color = COLOR_DIVIDER)

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(12.dp)
            ) {
                if (history.isEmpty()) {
                    item {
                        Column {
                            Text(
                                text = "╭──────────────────────────────────────╮",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = COLOR_GREEN
                            )
                            Text(
                                text = "│  Welcome to AURA Terminal v2.0.6     │",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = COLOR_GREEN
                            )
                            Text(
                                text = "╰──────────────────────────────────────╯",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = COLOR_GREEN
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "  • LOCAL  — 应用沙盒权限\n  • SHIZUKU — Shizuku 提权（免 root）\n  • ROOT   — 直接 root 执行\n\n  危险命令自动拦截\n  AI 可通过 run_shell 工具调用",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = COLOR_MUTED,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
                items(history) { line -> HistoryLineView(line) }
            }
        }

        HorizontalDivider(color = COLOR_DIVIDER)

        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$ ",
                color = COLOR_GREEN,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            )
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("输入命令...", color = COLOR_PLACEHOLDER) },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    color = COLOR_TEXT
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = COLOR_GREEN,
                    unfocusedBorderColor = COLOR_INPUT_BORDER,
                    cursorColor = COLOR_GREEN,
                    focusedContainerColor = COLOR_BG,
                    unfocusedContainerColor = COLOR_BG
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { run() })
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = { run() }) {
                Icon(Icons.Default.PlayArrow, contentDescription = "执行", tint = COLOR_GREEN)
            }
        }
    }
}

@Composable
private fun HistoryLineView(line: HistoryLine) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        if (line.command.isNotEmpty()) {
            Text(
                text = line.prompt + " " + line.command,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = COLOR_BLUE,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
        }
        if (line.output.isNotEmpty()) {
            Text(
                text = line.output,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = if (line.isError) COLOR_RED else COLOR_TEXT,
                fontSize = 12.sp,
                lineHeight = 16.sp
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
