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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bskai.BuildConfig
import com.bskai.permission.ShizukuBridge
import com.bskai.terminal.TerminalEngine
import com.bskai.ui.glass.GlassChip
import com.bskai.ui.glass.GlassIconButton
import com.bskai.ui.glass.GlassPanel
import com.bskai.ui.glass.GlassTextField
import com.bskai.ui.glass.rememberGlassColors
import kotlinx.coroutines.launch

private val TERM_BG = Color(0xCC0A0E18)
private val TERM_TEXT = Color(0xFFE6EDF3)
private val TERM_MUTED = Color(0xFF8B949E)
private val TERM_GREEN = Color(0xFF3FB950)
private val TERM_BLUE = Color(0xFF58A6FF)
private val TERM_RED = Color(0xFFF85149)

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
            .imePadding()
    ) {
        GlassPanel(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TerminalEngine.Backend.entries.forEach { b ->
                    val enabled = when (b) {
                        TerminalEngine.Backend.LOCAL -> true
                        TerminalEngine.Backend.SHIZUKU -> shizukuState == ShizukuBridge.State.GRANTED
                        TerminalEngine.Backend.ROOT -> true
                    }
                    GlassChip(
                        text = b.name,
                        selected = backend == b,
                        enabled = enabled,
                        onClick = { engine.setBackend(b) }
                    )
                }
                if (shizukuState == ShizukuBridge.State.NEED_PERMISSION) {
                    GlassChip(
                        text = "授权 Shizuku",
                        selected = false,
                        onClick = { shizuku?.requestPermission() }
                    )
                } else if (shizukuState == ShizukuBridge.State.UNAVAILABLE) {
                    Text(
                        text = "Shizuku 未安装",
                        style = MaterialTheme.typography.bodySmall,
                        color = TERM_MUTED
                    )
                }
                Spacer(Modifier.weight(1f))
                GlassIconButton(
                    icon = Icons.Default.ContentCopy,
                    contentDescription = "复制全部",
                    size = 34.dp,
                    onClick = { copyAll() }
                )
                GlassIconButton(
                    icon = Icons.Default.ClearAll,
                    contentDescription = "清空",
                    size = 34.dp,
                    onClick = { clearHistory() }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        GlassPanel(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(listOf(TERM_BG, Color(0xE60A0E18))),
                        RoundedCornerShape(20.dp)
                    )
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(14.dp)
                ) {
                    if (history.isEmpty()) {
                        item {
                            Column {
                                Text(
                                    text = "AURA Terminal v${BuildConfig.APP_VERSION}",
                                    style = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        color = TERM_GREEN,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "LOCAL     应用沙盒权限\nSHIZUKU   Shizuku 提权（免 root）\nROOT      直接 root 执行\n\n危险命令自动拦截\nAI 可通过 run_shell 工具调用",
                                    style = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = TERM_MUTED,
                                        lineHeight = 17.sp
                                    )
                                )
                            }
                        }
                    }
                    items(history) { line -> HistoryLineView(line) }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        GlassPanel(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$",
                    color = TERM_GREEN,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 15.sp
                )
                Spacer(Modifier.width(8.dp))
                GlassTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = "输入命令…",
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { run() })
                )
                Spacer(Modifier.width(8.dp))
                GlassIconButton(
                    icon = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "执行",
                    size = 40.dp,
                    tint = TERM_GREEN,
                    onClick = { run() }
                )
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
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    color = TERM_BLUE,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            )
        }
        if (line.output.isNotEmpty()) {
            Text(
                text = line.output,
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    color = if (line.isError) TERM_RED else TERM_TEXT,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
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
