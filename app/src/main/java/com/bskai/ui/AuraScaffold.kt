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
import com.bskai.data.Agreements
import com.bskai.data.loadAnnouncements
import com.bskai.ui.chat.ChatScreen
import com.bskai.ui.legal.AgreementDecision
import com.bskai.ui.legal.AgreementDialog
import com.bskai.ui.settings.SettingsScreen
import com.bskai.ui.terminal.TerminalScreen
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

    var showSettings by remember { mutableStateOf(false) }
    var showTerminal by remember { mutableStateOf(false) }

    var pendingAgreementFor by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        com.bskai.MainActivity.navRequests.collect { target ->
            when (target) {
                "settings" -> showSettings = true
                "terminal" -> showTerminal = true
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
             ChatScreen(
                 app = app,
                 onOpenSettings = { showSettings = true }
             )

            if (showSettings) {
                SettingsScreen(
                    app = app,
                    onClose = { showSettings = false },
                    onOpenTerminal = {
                        showSettings = false
                        showTerminal = true
                    }
                )
            }
            if (showTerminal) {
                TerminalScreen(
                    engine = app.terminal,
                    shizuku = app.shizuku,
                    onClose = { showTerminal = false }
                )
            }
        }
    }

    pendingAgreementFor?.let { version ->
        AgreementDialog(
            version = version,
            openSource = Agreements.openSource,
            privacy = Agreements.privacy.copy(body = Agreements.renderPrivacy(version)),
            requireBoth = true,
            onCancel = { pendingAgreementFor = null },
            onConfirm = { _: AgreementDecision ->
                app.settings.markSessionAgreement(version)
                pendingAgreementFor = null
            }
        )
    }
}
