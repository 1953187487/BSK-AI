package com.bskai.ui.screens.orchestrate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bskai.BskApp
import com.bskai.orchestration.OrchestratorEngine
import com.bskai.orchestration.PipelineStore
import com.bskai.orchestration.StepStatus
import com.bskai.ui.components.PermissionDialog
import com.bskai.ui.theme.MonoFont
import kotlinx.coroutines.launch

@Composable
fun OrchestrateScreen(app: BskApp, pipelineStore: PipelineStore) {
    val pipelines by pipelineStore.pipelines.collectAsState()
    val running by pipelineStore.running.collectAsState()
    var task by remember { mutableStateOf("") }
    var showPermission by remember { mutableStateOf(false) }
    var pendingToolName by remember { mutableStateOf("") }
    var pendingArgs by remember { mutableStateOf(org.json.JSONObject()) }
    var pendingAllow by remember { mutableStateOf<Boolean?>(null) }
    val scope = rememberCoroutineScope()

    val workspaceDir = remember {
        java.io.File(
            app.getExternalFilesDir(null) ?: app.filesDir,
            "workspace"
        ).apply { mkdirs() }.absolutePath
    }

    fun startPipeline() {
        if (task.isBlank() || running) return
        val provider = app.providerStore.activeProvider()
        if (provider == null) return
        val settings = app.settingsStore.settings.value
        val engine = OrchestratorEngine(
            appContext = app,
            provider = provider,
            workspaceRoot = workspaceDir,
            store = pipelineStore,
            autoApprove = settings.autoApproveTools,
            maxRounds = settings.maxPipelineRounds,
            permissionResolver = { tool, args ->
                pendingToolName = tool.name
                pendingArgs = args
                pendingAllow = null
                showPermission = true
                while (pendingAllow == null) {
                    kotlinx.coroutines.delay(100)
                }
                showPermission = false
                pendingAllow == true
            }
        )
        scope.launch {
            engine.run(task)
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("多智能体编排", style = MaterialTheme.typography.headlineMedium)
        Text(
            "规划器 → 编码器 → 审查器 迭代流水线（OpenClaw 风格）",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = task,
                onValueChange = { task = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("输入开发任务，如：写一个待办清单应用") },
                maxLines = 3
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = { startPipeline() }, enabled = task.isNotBlank() && !running) {
                Icon(Icons.Outlined.ArrowForward, contentDescription = null)
            }
        }
        if (running) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                CircularProgressIndicator(Modifier.width(18.dp).height(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("流水线运行中...", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
            }
        }
        Spacer(Modifier.height(12.dp))
        if (pipelines.isEmpty()) {
            Text(
                "还没有运行任务。提交任务后，多个专业智能体会分工协作完成开发。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(pipelines, key = { it.id }) { pipeline ->
                PipelineCard(pipeline)
            }
        }
    }

    if (showPermission) {
        PermissionDialog(
            toolName = pendingToolName,
            args = pendingArgs,
            onAllow = { pendingAllow = true },
            onDeny = { pendingAllow = false }
        )
    }
}

@Composable
private fun PipelineCard(pipeline: com.bskai.orchestration.Pipeline) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("任务", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(8.dp))
                Text(pipeline.task, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                val statusText = when (pipeline.status) {
                    StepStatus.DONE -> "已完成"
                    StepStatus.FAILED -> "失败"
                    StepStatus.RUNNING -> "运行中"
                    else -> "等待中"
                }
                Text(statusText, style = MaterialTheme.typography.labelMedium, color = when (pipeline.status) {
                    StepStatus.DONE -> MaterialTheme.colorScheme.secondary
                    StepStatus.FAILED -> MaterialTheme.colorScheme.error
                    StepStatus.RUNNING -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                })
            }
            Spacer(Modifier.height(8.dp))
            pipeline.steps.forEach { step ->
                StepRow(step)
            }
        }
    }
}

@Composable
private fun StepRow(step: com.bskai.orchestration.PipelineStep) {
    val icon = when (step.status) {
        StepStatus.RUNNING -> "⟳"
        StepStatus.DONE -> "✓"
        StepStatus.FAILED -> "✗"
        else -> "·"
    }
    val color = when (step.status) {
        StepStatus.RUNNING -> MaterialTheme.colorScheme.tertiary
        StepStatus.DONE -> MaterialTheme.colorScheme.secondary
        StepStatus.FAILED -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$icon ", color = color, fontFamily = MonoFont)
            Text("[${step.role.display}]", fontFamily = MonoFont, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            Spacer(Modifier.width(8.dp))
            Text(step.label, style = MaterialTheme.typography.bodyMedium)
        }
        if (step.output.isNotEmpty()) {
            Text(
                step.output.take(300),
                fontSize = 11.sp,
                fontFamily = MonoFont,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 5,
                modifier = Modifier.padding(start = 12.dp, top = 2.dp)
            )
        }
        if (step.error.isNotEmpty()) {
            Text(
                step.error,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 12.dp, top = 2.dp)
            )
        }
    }
}
