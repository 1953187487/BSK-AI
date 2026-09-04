package com.bskai.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.bskai.AuraApp
import com.bskai.BuildConfig
import com.bskai.MainActivity
import com.bskai.data.loadAnnouncements
import com.bskai.ui.chat.ChatScreen
import com.bskai.ui.settings.SettingsScreen
import com.bskai.ui.skills.SkillsScreen

@Composable
fun AuraScaffold(app: AuraApp) {
    val allAnnouncements = remember { loadAnnouncements(app) }
    val lastSeen = app.settings.lastSeenVersion()
    val currentVersion = BuildConfig.APP_VERSION

    var showAnnouncement by rememberSaveable(lastSeen) {
        mutableStateOf(lastSeen != currentVersion && allAnnouncements.isNotEmpty())
    }

    if (showAnnouncement) {
        val target = allAnnouncements.firstOrNull { it.version == currentVersion }
            ?: allAnnouncements.firstOrNull()
        if (target != null) {
            AlertDialog(
                onDismissRequest = {
                    app.settings.setLastSeenVersion(currentVersion)
                    showAnnouncement = false
                },
                confirmButton = {
                    TextButton(onClick = {
                        app.settings.setLastSeenVersion(currentVersion)
                        showAnnouncement = false
                    }) {
                        Text("知道了")
                    }
                },
                title = { Text(target.title) },
                text = {
                    Column {
                        Text(target.content)
                        target.changelog.forEach { line ->
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

    var tab by rememberSaveable { mutableStateOf("chat") }
    LaunchedEffect(Unit) {
        MainActivity.navRequests.collect { target ->
            if (target != null) tab = target
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                listOf(
                    Triple("chat", "对话", Icons.Default.Chat),
                    Triple("skills", "技能", Icons.Default.Extension),
                    Triple("settings", "设置", Icons.Default.Settings)
                ).forEach { (key, label, icon) ->
                    NavigationBarItem(
                        selected = tab == key,
                        onClick = { tab = key },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                "chat" -> ChatScreen(app = app)
                "skills" -> SkillsScreen(app = app)
                "settings" -> SettingsScreen(app = app)
            }
        }
    }
}
