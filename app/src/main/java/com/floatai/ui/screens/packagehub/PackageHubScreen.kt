package com.floatai.ui.screens.packagehub

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.floatai.ui.screens.mcp.McpScreen
import com.floatai.ui.screens.packages.PackagesScreen

private enum class HubTab(
    val title: String,
    val icon: ImageVector
) {
    Packages("包管理", Icons.Filled.Storage),
    Plugins("插件", Icons.Filled.Extension),
    Skills("技能", Icons.Filled.AutoAwesome),
    Mcp("MCP 服务", Icons.Filled.SmartToy);
}

/**
 * Package Hub：四 Tab 统一容器（v1.0.3 新增）。
 *
 *  - Packages: 复用 v1.0.2 已实现的 PackageRegistry
 *  - Plugins / Skills: v1.0.3 占位（v1.0.4 实现创建逻辑）
 *  - Mcp: 复用 v1.0.2 已实现的 McpRegistry
 */
@Composable
fun PackageHubScreen() {
    var tabIndex by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TabRow(tabIndex = tabIndex, onTabSelected = { tabIndex = it })

        Box(modifier = Modifier.fillMaxSize()) {
            when (HubTab.entries[tabIndex]) {
                HubTab.Packages -> PackagesScreen()
                HubTab.Plugins -> PlaceholderTab(
                    title = "插件",
                    icon = Icons.Filled.Extension,
                    description = "插件系统即将推出。你可以在此管理第三方扩展，自定义命令与 UI 组件。"
                )
                HubTab.Skills -> PlaceholderTab(
                    title = "技能",
                    icon = Icons.Filled.AutoAwesome,
                    description = "技能系统即将推出。通过 AI 快速生成可复用技能，自动绑定到对话与抓包流程。"
                )
                HubTab.Mcp -> McpScreen()
            }
        }
    }
}

@Composable
private fun TabRow(tabIndex: Int, onTabSelected: (Int) -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    val tabs = HubTab.entries
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            tabs.forEachIndexed { i, tab ->
                val selected = i == tabIndex
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTabSelected(i) }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        tab.icon,
                        contentDescription = tab.title,
                        tint = if (selected) primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        tab.title,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    val animProgress by animateFloatAsState(
                        targetValue = if (selected) 1f else 0f,
                        animationSpec = tween(220),
                        label = "tab-indicator"
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(3.dp)
                            .background(
                                color = primary.copy(alpha = animProgress),
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaceholderTab(
    title: String,
    icon: ImageVector,
    description: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(60.dp))
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(12.dp))
        Text(
            description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                "v1.0.4 即将推出",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
