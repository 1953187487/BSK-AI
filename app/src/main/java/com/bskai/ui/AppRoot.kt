package com.bskai.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.bskai.ui.screens.agent.AgentScreen
import com.bskai.ui.screens.agent.AgentViewModel
import com.bskai.ui.screens.settings.SettingsScreen
import com.bskai.ui.screens.toolchain.ToolboxScreen
import com.bskai.ui.screens.terminal.TerminalScreen

private data class TabItem(
    val label: String,
    val icon: ImageVector
)

private val tabs = listOf(
    TabItem("AI", Icons.Outlined.Chat),
    TabItem("工具箱", Icons.Outlined.Build),
    TabItem("终端", Icons.Outlined.Terminal),
    TabItem("设置", Icons.Outlined.Settings)
)

@Composable
fun AppRoot(
    app: BskApp,
    agentViewModel: AgentViewModel
) {
    var current by rememberSaveable { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = current == index,
                        onClick = { current = index },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        androidx.compose.foundation.layout.Box(
            Modifier.padding(padding),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            when (current) {
                0 -> AgentScreen(agentViewModel)
                1 -> ToolboxScreen(app)
                2 -> TerminalScreen(app)
                3 -> SettingsScreen(app)
            }
        }
    }
}
