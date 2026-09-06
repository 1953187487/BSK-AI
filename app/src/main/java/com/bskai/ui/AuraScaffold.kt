package com.bskai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import com.bskai.AuraApp
import com.bskai.BuildConfig
import com.bskai.data.loadAnnouncements
import com.bskai.ui.chat.ChatScreen
import com.bskai.ui.settings.SettingsScreen
import com.bskai.ui.terminal.TerminalScreen

private enum class Tab(val label: String, val icon: ImageVector) {
    CHAT("对话", Icons.Default.Chat),
    TERMINAL("终端", Icons.Default.Terminal),
    SETTINGS("设置", Icons.Default.Settings)
}

@Composable
fun AuraScaffold(app: AuraApp) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

    val allAnnouncements = remember { loadAnnouncements(app) }
    val lastSeen = app.settings.lastSeenVersion()
    val currentVersion = BuildConfig.APP_VERSION
    val snackbarHostState = remember { SnackbarHostState() }
    var currentTab by rememberSaveable { mutableIntStateOf(0) }

    var showAnnouncement: Boolean by rememberSaveable(lastSeen) {
        mutableStateOf(lastSeen != currentVersion && allAnnouncements.isNotEmpty())
    }

    if (showAnnouncement) {
        val target = allAnnouncements.firstOrNull { it.version == currentVersion }
            ?: allAnnouncements.firstOrNull()
        if (target != null) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = {
                    app.settings.setLastSeenVersion(currentVersion)
                    showAnnouncement = false
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = {
                        app.settings.setLastSeenVersion(currentVersion)
                        showAnnouncement = false
                    }) {
                        Text("知道了")
                    }
                },
                title = { Text(target.title) },
                text = {
                    androidx.compose.foundation.layout.Column {
                        Text(target.content)
                        for (line in target.changelog) {
                            Text("• $line")
                        }
                    }
                }
            )
        } else {
            LaunchedEffect(Unit) {
                app.settings.setLastSeenVersion(currentVersion)
                showAnnouncement = false
            }
        }
    }

    LaunchedEffect(Unit) {
        com.bskai.MainActivity.navRequests.collect { target ->
            when (target) {
                "settings" -> currentTab = 2
                "terminal" -> currentTab = 1
            }
        }
    }

    if (isLandscape) {
        // 横屏：按钮在左边
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Row(modifier = Modifier.fillMaxSize().padding(padding)) {
                NavigationRail {
                    Tab.entries.forEachIndexed { index, tab ->
                        NavigationRailItem(
                            selected = currentTab == index,
                            onClick = { currentTab = index },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    when (currentTab) {
                        0 -> ChatScreen(app = app, snackbarHostState = snackbarHostState)
                        1 -> TerminalScreen(engine = app.terminal, shizuku = app.shizuku)
                        2 -> SettingsScreen(app = app)
                    }
                }
            }
        }
    } else {
        // 竖屏：按钮在底部
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                NavigationBar {
                    Tab.entries.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = currentTab == index,
                            onClick = { currentTab = index },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                when (currentTab) {
                    0 -> ChatScreen(app = app, snackbarHostState = snackbarHostState)
                    1 -> TerminalScreen(engine = app.terminal, shizuku = app.shizuku)
                    2 -> SettingsScreen(app = app)
                }
            }
        }
    }
}
