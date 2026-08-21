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
    TabItem("智能体", Icons.Outlined.Terminal, "agent"),
    TabItem("编排", Icons.Outlined.AccountTree, "orchestrate"),
    TabItem("模型", Icons.Outlined.Memory, "models"),
    TabItem("工具链", Icons.Outlined.Build, "toolchain"),
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
        androidx.compose.foundation.layout.Box(Modifier.padding(padding)) {
            when (current) {
                "agent" -> AgentScreen(agentViewModel)
                "orchestrate" -> OrchestrateScreen(app, pipelineStore)
                "models" -> ModelHubScreen(app)
                "toolchain" -> ToolchainScreen(app)
                "settings" -> SettingsScreen(app)
            }
        }
    }
}
