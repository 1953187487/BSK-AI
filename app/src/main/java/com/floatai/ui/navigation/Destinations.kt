package com.floatai.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Build
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
    data object Atk : AppDestination("atk", "ATK", Icons.Filled.Build)
    data object Settings : AppDestination("settings", "设置", Icons.Filled.Settings)

    companion object {
        val bottomBar = listOf(Chat, Atk, Settings)
    }
}
