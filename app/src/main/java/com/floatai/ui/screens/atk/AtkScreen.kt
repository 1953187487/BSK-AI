package com.floatai.ui.screens.atk

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.floatai.App
import com.floatai.ui.components.GlassCard
import com.floatai.ui.components.SectionTitle
import com.floatai.ui.i18n.localStrings

@Composable
fun AtkScreen() {
    val app = LocalContext.current.applicationContext as App
    val vm: AtkViewModel = viewModel(key = "atk", factory = AtkViewModel.factory(app))
    val s by vm.state.collectAsStateWithLifecycle()
    val strings = localStrings()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(strings.atk_title, style = MaterialTheme.typography.headlineMedium)
        Text(
            strings.atk_subtitle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp)
        )

        SectionTitle(strings.atk_project)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = s.projectName,
                        onValueChange = vm::setProjectName,
                        label = { Text(strings.atk_project_name) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = s.packageName,
                        onValueChange = vm::setPackageName,
                        label = { Text(strings.atk_project_pkg) },
                        singleLine = true,
                        modifier = Modifier.weight(1.4f)
                    )
                }
                Button(
                    onClick = vm::scaffold,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                ) {
                    Icon(Icons.Filled.Save, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(strings.atk_project_create)
                }
                if (s.projectDir.isNotBlank()) {
                    Text(
                        "Path: ${s.projectDir}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }

        SectionTitle(strings.atk_terminal)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = s.terminalCmd,
                        onValueChange = vm::setTerminalCmd,
                        placeholder = { Text(strings.atk_terminal_hint) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = vm::runTerminal) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(strings.atk_run)
                    }
                }
                LogBox(text = s.logs, modifier = Modifier.fillMaxWidth().padding(top = 10.dp).heightIn(min = 120.dp, max = 220.dp))
                Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                    IconButton(onClick = vm::clearLog) { Icon(Icons.Filled.Delete, contentDescription = strings.atk_clear_log) }
                }
            }
        }

        SectionTitle("Build & Publish")
        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { vm.build("assembleDebug") },
                enabled = !s.building && s.projectDir.isNotBlank(),
                modifier = Modifier.weight(1f).padding(end = 6.dp)
            ) {
                Icon(Icons.Filled.Build, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text(strings.atk_build_debug)
            }
            Button(
                onClick = { vm.build("assembleRelease") },
                enabled = !s.building && s.projectDir.isNotBlank(),
                modifier = Modifier.weight(1f).padding(start = 6.dp)
            ) {
                Icon(Icons.Filled.Build, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text(strings.atk_build_release)
            }
        }
        if (s.lastApk.isNotBlank()) {
            Text(
                "APK: ${s.lastApk}",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        SectionTitle(strings.atk_publish)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                OutlinedTextField(
                    value = s.repoName,
                    onValueChange = vm::setRepoName,
                    label = { Text(strings.atk_publish_repo) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = s.repoDesc,
                    onValueChange = vm::setRepoDesc,
                    label = { Text(strings.atk_publish_desc) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Checkbox(checked = s.repoPrivate, onCheckedChange = vm::setRepoPrivate)
                    Text(strings.atk_publish_private)
                }
                Button(
                    onClick = vm::publish,
                    enabled = !s.publishing,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                ) {
                    Icon(Icons.Filled.CloudUpload, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(strings.atk_publish)
                }
            }
        }

        SectionTitle("AI")
        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = vm::aiDiagnose,
                modifier = Modifier.weight(1f).padding(end = 6.dp),
                enabled = s.logs.isNotBlank()
            ) {
                Icon(Icons.Filled.Memory, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text(strings.atk_ai_diagnose)
            }
            IconButton(onClick = vm::clearAi) { Icon(Icons.Filled.Cancel, contentDescription = null) }
        }
        if (s.aiAnswer.isNotBlank()) {
            LogBox(
                text = s.aiAnswer,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).heightIn(min = 120.dp, max = 320.dp)
            )
        }

        if (s.message.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = s.message,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun LogBox(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color(0xCC0E0E10), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Text(
            text = text.ifBlank { "[empty]" },
            color = Color(0xFFD3D3D8),
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp
        )
    }
}
