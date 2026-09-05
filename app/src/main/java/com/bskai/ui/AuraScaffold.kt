package com.bskai.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.bskai.AuraApp
import com.bskai.BuildConfig
import com.bskai.data.loadAnnouncements
import com.bskai.ui.chat.ChatScreen
import com.bskai.ui.settings.SettingsScreen
import com.bskai.ui.update.HistoryDialog
import com.bskai.ui.update.UpdateDialog
import com.bskai.update.GitHubApi
import com.bskai.update.RemoteRelease
import com.bskai.update.UpdateCheckResult
import kotlinx.coroutines.launch

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
                    androidx.compose.foundation.layout.Column {
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

    var updateResult by remember { mutableStateOf<UpdateCheckResult?>(null) }
    var historyReleases by remember { mutableStateOf<List<RemoteRelease>?>(null) }
    var historyLoading by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // 监听外部 nav_target：仅支持 "settings"
    LaunchedEffect(Unit) {
        com.bskai.MainActivity.navRequests.collect { target ->
            if (target == "settings") showSettings = true
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            ChatScreen(
                app = app,
                onShowUpdate = { updateResult = it },
                onShowHistory = {
                    historyReleases = emptyList()
                    historyLoading = true
                    scope.launch {
                        val r = GitHubApi.listReleases()
                            .filter { it.versionCode > 0 }
                            .sortedByDescending { it.versionCode }
                        historyReleases = r
                        historyLoading = false
                    }
                },
                onOpenSettings = { showSettings = true }
            )

            if (showSettings) {
                SettingsScreen(app = app, onClose = { showSettings = false })
            }
        }
    }

    updateResult?.let {
        UpdateDialog(result = it, onDismiss = { updateResult = null })
    }
    historyReleases?.let {
        HistoryDialog(
            releases = it,
            loading = historyLoading && it.isEmpty(),
            onDismiss = { historyReleases = null }
        )
    }
}
