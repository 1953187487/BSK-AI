package com.floatai.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 应用导航目的地定义。
 */
sealed class AppDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    data object Chat : AppDestination("chat", "AI 对话", Icons.AutoMirrored.Filled.Chat)
    data object Api : AppDestination("api", "API 配置", Icons.Filled.Dns)
    data object Settings : AppDestination("settings", "设置", Icons.Filled.Settings)

    companion object {
        val bottomBar = listOf(Chat, Api, Settings)
    }
}
