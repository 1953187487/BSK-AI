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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bskai.BuildConfig
import com.bskai.R
import com.bskai.permission.DhizukuBridge
import com.bskai.permission.ShizukuBridge
import com.bskai.terminal.TerminalEngine
import com.bskai.ui.glass.GlassButton
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
    shizuku: ShizukuBridge?,
    dhizuku: DhizukuBridge? = null
) {
    val backend by engine.backend.collectAsState()
    val shizukuState = shizuku?.state?.collectAsState()?.value ?: ShizukuBridge.State.UNAVAILABLE
    val dhizukuState = dhizuku?.state?.collectAsState()?.value ?: DhizukuBridge.State.UNAVAILABLE
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val history = remember { mutableStateListOf<HistoryLine>() }
    var input by remember { mutableStateOf("") }
    var showAuth by remember { mutableStateOf(false) }
    // Lets the user keep the sandboxed terminal open when neither privilege is granted.
    var bypassed by rememberSaveable { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val noOutput = stringResource(R.string.terminal_no_output)
    val bannerTitle = stringResource(R.string.terminal_banner_title, BuildConfig.APP_VERSION)
    val bannerHelp = stringResource(R.string.terminal_banner_help)

    val shizukuGranted = shizukuState == ShizukuBridge.State.GRANTED
    val dhizukuGranted = dhizukuState == DhizukuBridge.State.GRANTED
    val hasPrivilege = shizukuGranted || dhizukuGranted
    val locked = !hasPrivilege && !bypassed

    fun run() {
        val cmd = input.trim()
        if (cmd.isEmpty()) return
        input = ""
        history.add(HistoryLine(prompt = "$", command = cmd))
        scope.launch {
            val r = engine.execute(cmd)
            val outputText = if (r.stdout.isNotEmpty()) r.stdout else r.stderr.ifEmpty {
                noOutput
            }
            history.add(HistoryLine(
                prompt = r.backend.name.lowercase() + ":" + r.exitCode,
                output = outputText,
                isError = r.exitCode != 0
            ))
        }
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

    if (locked) {
        TerminalLockedView(
            shizukuState = shizukuState,
            dhizukuState = dhizukuState,
            shizuku = shizuku,
            dhizuku = dhizuku,
            onGrant = { showAuth = true },
            onLocalFallback = { bypassed = true }
        )
        return
    }

    if (showAuth) {
        AuthorizationDialog(
            shizukuState = shizukuState,
            dhizukuState = dhizukuState,
            onShizuku = {
                if (shizukuState == ShizukuBridge.State.UNAVAILABLE) shizuku?.refresh()
                else shizuku?.requestPermission()
            },
            onDhizuku = {
                if (dhizukuState == DhizukuBridge.State.UNAVAILABLE) dhizuku?.refresh()
                else dhizuku?.requestPermission()
            },
            onDismiss = { showAuth = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        GlassPanel(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TerminalEngine.Backend.entries.forEach { b ->
                    GlassChip(
                        text = stringResource(backendLabel(b)),
                        selected = backend == b,
                        enabled = isBackendUsable(b, shizukuGranted, dhizukuGranted),
                        onClick = { engine.setBackend(b) }
                    )
                }
                if (!hasPrivilege) {
                    GlassChip(
                        text = stringResource(R.string.terminal_authorize),
                        selected = false,
                        onClick = { showAuth = true }
                    )
                }
                Spacer(Modifier.weight(1f))
                GlassIconButton(
                    icon = Icons.Default.ContentCopy,
                    contentDescription = stringResource(R.string.terminal_copy_all),
                    size = 34.dp,
                    onClick = { copyAll() }
                )
                GlassIconButton(
                    icon = Icons.Default.ClearAll,
                    contentDescription = stringResource(R.string.terminal_clear),
                    size = 34.dp,
                    onClick = { history.clear() }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        GlassPanel(shape = RoundedCornerShape(20.dp), modifier = Modifier.weight(1f).fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(listOf(TERM_BG, Color(0xE60A0E18))),
                        RoundedCornerShape(20.dp)
                    )
            ) {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(14.dp)) {
                    if (history.isEmpty()) {
                        item {
                            Column {
                                Text(
                                    text = bannerTitle,
                                    style = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        color = TERM_GREEN,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = bannerHelp,
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

        GlassPanel(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
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
                    placeholder = stringResource(R.string.terminal_input_hint),
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
                    contentDescription = stringResource(R.string.terminal_run),
                    size = 40.dp,
                    tint = TERM_GREEN,
                    onClick = { run() }
                )
            }
        }
    }
}

@Composable
private fun backendLabel(b: TerminalEngine.Backend): Int = when (b) {
    TerminalEngine.Backend.LOCAL -> R.string.terminal_backend_local
    TerminalEngine.Backend.SHIZUKU -> R.string.terminal_backend_shizuku
    TerminalEngine.Backend.DHIZUKU -> R.string.terminal_backend_dhizuku
    TerminalEngine.Backend.ROOT -> R.string.terminal_backend_root
}

private fun isBackendUsable(
    b: TerminalEngine.Backend,
    shizukuGranted: Boolean,
    dhizukuGranted: Boolean
): Boolean = when (b) {
    TerminalEngine.Backend.LOCAL -> true
    TerminalEngine.Backend.SHIZUKU -> shizukuGranted
    TerminalEngine.Backend.DHIZUKU -> dhizukuGranted
    TerminalEngine.Backend.ROOT -> true
}

@Composable
private fun TerminalLockedView(
    shizukuState: ShizukuBridge.State,
    dhizukuState: DhizukuBridge.State,
    shizuku: ShizukuBridge?,
    dhizuku: DhizukuBridge?,
    onGrant: () -> Unit,
    onLocalFallback: () -> Unit
) {
    val glass = rememberGlassColors()
    val accent = MaterialTheme.colorScheme.primary

    GlassPanel(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = accent.copy(alpha = 0.7f),
                modifier = Modifier.size(44.dp)
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.terminal_locked_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = glass.content
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.terminal_locked_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = glass.contentMuted,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))

            GlassButton(text = stringResource(R.string.terminal_authorize), onClick = onGrant)
            Spacer(Modifier.height(10.dp))
            GlassButton(
                text = stringResource(R.string.terminal_local_fallback),
                onClick = onLocalFallback
            )
        }
    }
}

@Composable
private fun AuthorizationDialog(
    shizukuState: ShizukuBridge.State,
    dhizukuState: DhizukuBridge.State,
    onShizuku: () -> Unit,
    onDhizuku: () -> Unit,
    onDismiss: () -> Unit
) {
    val glass = rememberGlassColors()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.terminal_authorize_title)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = stringResource(R.string.terminal_authorize_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = glass.contentMuted
                )
                GlassPanel(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        AuthorizationRow(
                            name = stringResource(R.string.terminal_backend_shizuku),
                            stateText = stringResource(privilegeStateText(shizukuState)),
                            actionText = stringResource(privilegeActionText(shizukuState)),
                            onClick = onShizuku
                        )
                    }
                }
                GlassPanel(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        AuthorizationRow(
                            name = stringResource(R.string.terminal_backend_dhizuku),
                            stateText = stringResource(privilegeStateText(dhizukuState)),
                            actionText = stringResource(privilegeActionText(dhizukuState)),
                            onClick = onDhizuku
                        )
                    }
                }
            }
        },
        confirmButton = { GlassButton(text = stringResource(R.string.common_close), onClick = onDismiss) }
    )
}

@Composable
private fun AuthorizationRow(name: String, stateText: String, actionText: String, onClick: () -> Unit) {
    val glass = rememberGlassColors()
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = glass.content)
            Spacer(Modifier.height(2.dp))
            Text(stateText, style = MaterialTheme.typography.bodySmall, color = glass.contentMuted)
        }
        Spacer(Modifier.width(10.dp))
        GlassChip(text = actionText, selected = false, onClick = onClick)
    }
}

private fun privilegeStateText(state: ShizukuBridge.State): Int = when (state) {
    ShizukuBridge.State.UNAVAILABLE -> R.string.privilege_state_unavailable
    ShizukuBridge.State.NEED_PERMISSION -> R.string.privilege_state_need_permission
    ShizukuBridge.State.GRANTED -> R.string.privilege_state_granted
}

private fun privilegeStateText(state: DhizukuBridge.State): Int = when (state) {
    DhizukuBridge.State.UNAVAILABLE -> R.string.privilege_state_unavailable
    DhizukuBridge.State.NEED_PERMISSION -> R.string.privilege_state_need_permission
    DhizukuBridge.State.GRANTED -> R.string.privilege_state_granted
}

private fun privilegeActionText(state: ShizukuBridge.State): Int =
    if (state == ShizukuBridge.State.UNAVAILABLE) R.string.privilege_redetect
    else R.string.privilege_authorize

private fun privilegeActionText(state: DhizukuBridge.State): Int =
    if (state == DhizukuBridge.State.UNAVAILABLE) R.string.privilege_redetect
    else R.string.privilege_authorize

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
