package com.bskai.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ModelTraining
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bskai.BskApp
import com.bskai.core.settings.SettingsStore
import com.bskai.ui.screens.agreements.OpenSourceScreen
import com.bskai.ui.screens.agreements.TermsScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(app: BskApp) {
    val settingsStore: SettingsStore = app.settingsStore
    val settings by settingsStore.settings.collectAsState()
    var versionTapCount by remember { mutableIntStateOf(0) }
    var showTerms by remember { mutableStateOf(false) }
    var showOSS by remember { mutableStateOf(false) }

    if (showTerms) {
        TermsScreen(onBack = { showTerms = false })
        return
    }
    if (showOSS) {
        OpenSourceScreen(onBack = { showOSS = false })
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                SectionCard(title = "Model Config", icon = Icons.Outlined.ModelTraining) {
                    SettingRow(
                        icon = Icons.Outlined.Terminal,
                        title = "API Endpoint",
                        subtitle = settings.providerUrl.ifEmpty { "Not configured" }
                    ) { /* navigate to providers */ }
                    SettingRow(
                        icon = Icons.Outlined.Code,
                        title = "Local Model",
                        subtitle = if (settings.providerUrl.isEmpty()) "Using local GGUF model" else "Using API model"
                    ) { /* switch mode */ }
                }
            }

            item {
                SectionCard(title = "Appearance", icon = Icons.Outlined.Palette) {
                    SettingRowSwitch(
                        icon = Icons.Outlined.Palette,
                        title = "Dark Theme",
                        checked = settings.darkTheme
                    ) { settingsStore.update { it.copy(darkTheme = !it.darkTheme) } }
                }
            }

            item {
                SectionCard(title = "Agent", icon = Icons.Outlined.Terminal) {
                    SettingRowSwitch(
                        icon = Icons.Outlined.Terminal,
                        title = "Auto-approve Tools",
                        subtitle = "Execute tools without confirmation",
                        checked = settings.autoApproveTools
                    ) { settingsStore.update { it.copy(autoApproveTools = !it.autoApproveTools) } }
                }
            }

            item {
                SectionCard(title = "About", icon = Icons.Outlined.Info) {
                    SettingRow(
                        icon = Icons.Outlined.Info,
                        title = "User Agreement",
                        subtitle = "View full terms"
                    ) { showTerms = true }
                    SettingRow(
                        icon = Icons.Outlined.Gavel,
                        title = "Open Source Licenses",
                        subtitle = "Third-party component licenses"
                    ) { showOSS = true }
                    SettingRow(
                        icon = Icons.Outlined.Info,
                        title = "Version",
                        subtitle = "1.0.8"
                    ) {
                        versionTapCount++
                        if (versionTapCount >= 7) {
                            versionTapCount = 0
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SectionCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.width(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            content()
        }
    }
}

@Composable
private fun SettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String = "",
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            if (subtitle.isNotEmpty()) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Outlined.Code, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun SettingRowSwitch(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String = "",
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            if (subtitle.isNotEmpty()) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
