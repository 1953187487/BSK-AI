package com.bskai.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.bskai.BskApp
import com.bskai.orchestration.PipelineStore
import com.bskai.ui.screens.agent.AgentScreen
import com.bskai.ui.screens.agent.AgentViewModel
import com.bskai.ui.screens.models.ModelHubScreen
import com.bskai.ui.screens.orchestrate.OrchestrateScreen
import com.bskai.ui.screens.settings.SettingsScreen
import com.bskai.ui.screens.toolchain.ToolchainScreen

private data class TabItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

private val tabs = listOf(
    // 编排和对话合在一起
    TabItem("对话/编排", Icons.Outlined.Terminal, "agent"),
    // 模型和设置合并，模型可下载本地
    TabItem("模型/设置", Icons.Outlined.Memory, "models"),
    // 新增安卓应用开发（开发）在原来位置
    TabItem("开发", Icons.Outlined.Build, "dev"),
    TabItem("设置", Icons.Outlined.Settings, "settings")
)

@Composable
fun AppRoot(
    app: BskApp,
    agentViewModel: AgentViewModel
) {
    var current by rememberSaveable { mutableStateOf("agent") }
    val pipelineStore = androidx.compose.runtime.remember { PipelineStore() }

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = current == tab.route,
                        onClick = { current = tab.route },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        androidx.compose.foundation.layout.Column(Modifier.padding(padding)) {
            // 工作区与计划模式放在对话框上面
            androidx.compose.foundation.layout.Box(Modifier.weight(0.35f).padding(8.dp)) {
                when (current) {
                    "agent" -> Text("工作区：智能体与编排合并模式", modifier = Modifier.padding(8.dp))
                    "orchestrate" -> Text("计划模式：流水线编排", modifier = Modifier.padding(8.dp))
                    "dev" -> Text("安卓应用开发：构建与部署", modifier = Modifier.padding(8.dp))
                    else -> {} // 其他页面保持原样
                }
            }
            androidx.compose.foundation.layout.Box(Modifier.weight(0.65f)) {
                when (current) {
                    "agent" -> AgentScreen(agentViewModel)
                    "orchestrate" -> OrchestrateScreen(app, pipelineStore)
                    "models" -> ModelHubScreen(app) // 模型可下载本地
                    "dev" -> ToolchainScreen(app) // 安卓应用开发（开发）
                    "toolchain" -> ToolchainScreen(app)
                    "settings" -> SettingsScreen(app)
                }
            }
        }
    }
}
