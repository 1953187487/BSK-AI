package com.bskai.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bskai.AuraApp
import com.bskai.ui.screens.HomeScreen
import com.bskai.ui.screens.SkillsScreen
import com.bskai.ui.screens.SettingsScreen
import com.bskai.ui.screens.VoiceScreen
import com.bskai.ui.viewmodel.MainViewModel

private data class BottomTab(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val route: String
)

private val tabs = listOf(
    BottomTab("首页", Icons.Outlined.Home, Icons.Filled.Home, "home"),
    BottomTab("语音", Icons.Outlined.Mic, Icons.Filled.Mic, "voice"),
    BottomTab("技能", Icons.Outlined.Build, Icons.Filled.Build, "skills"),
    BottomTab("设置", Icons.Outlined.Settings, Icons.Filled.Settings, "settings")
)

@Composable
fun AppRoot(
    app: AuraApp,
    viewModel: MainViewModel = viewModel(factory = MainViewModel.Factory(app))
) {
    var currentRoute by remember { mutableStateOf("home") }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF16162A).copy(alpha = 0.95f),
                tonalElevation = 0.dp
            ) {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = { currentRoute = tab.route },
                        icon = {
                            Icon(
                                if (currentRoute == tab.route) tab.selectedIcon else tab.icon,
                                contentDescription = tab.label,
                                tint = if (currentRoute == tab.route) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (currentRoute) {
                "home" -> HomeScreen(viewModel = viewModel)
                "voice" -> VoiceScreen(viewModel = viewModel)
                "skills" -> SkillsScreen(viewModel = viewModel)
                "settings" -> SettingsScreen(viewModel = viewModel)
            }
        }
    }
}
