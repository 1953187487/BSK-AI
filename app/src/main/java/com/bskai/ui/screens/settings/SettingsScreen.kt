package com.bskai.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ModelTraining
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bskai.BskApp
import com.bskai.core.admin.AdminAuth
import com.bskai.core.admin.Announcement
import com.bskai.core.admin.AnnouncementStore
import com.bskai.core.settings.SettingsStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(app: BskApp) {
    val settingsStore = app.settingsStore
    val settings by settingsStore.settings.collectAsState()
    var adminLoggedIn by remember { mutableStateOf(AdminAuth.isLoggedIn) }
    var showAdminPanel by remember { mutableStateOf(false) }
    var versionTapCount by remember { mutableIntStateOf(0) }

    if (showAdminPanel) {
        AdminPanel(app, onBack = { showAdminPanel = false })
        return
    }

    val announcements = AnnouncementStore.load(app)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            // Announcements banner
            if (announcements.isNotEmpty()) {
                var expanded by remember { mutableStateOf(false) }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .clickable { expanded = !expanded },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                ) {
                    Column {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.width(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("公告", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            if (announcements.firstOrNull { it.pinned } != null) {
                                Spacer(Modifier.width(6.dp))
                                Text("置顶", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                        if (expanded) {
                            Divider(color = MaterialTheme.colorScheme.outlineVariant)
                            announcements.forEach { ann ->
                                AnnouncementItem(ann)
                            }
                        } else {
                            announcements.firstOrNull()?.let { ann ->
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(ann.title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Model config
                item {
                    SectionCard(title = "模型配置", icon = Icons.Outlined.ModelTraining) {
                        SettingRow(
                            icon = Icons.Outlined.Settings,
                            title = "API 端点",
                            subtitle = settings.providerUrl.ifEmpty { "未配置" }
                        ) { /* navigate to providers */ }
                        SettingRow(
                            icon = Icons.Outlined.Terminal,
                            title = "本地模型",
                            subtitle = if (settings.providerUrl.isEmpty()) "使用本地 GGUF 模型" else "使用 API 模型"
                        ) { /* switch mode */ }
                    }
                }

                // Appearance
                item {
                    SectionCard(title = "外观", icon = Icons.Outlined.Palette) {
                        SettingRowSwitch(
                            icon = Icons.Outlined.Palette,
                            title = "深色模式",
                            checked = settings.darkTheme
                        ) { settingsStore.update { it.copy(darkTheme = !it.darkTheme) } }
                    }
                }

                // Agent settings
                item {
                    SectionCard(title = "智能体", icon = Icons.Outlined.Terminal) {
                        SettingRowSwitch(
                            icon = Icons.Outlined.AdminPanelSettings,
                            title = "自动批准工具",
                            subtitle = "无需确认直接执行工具",
                            checked = settings.autoApproveTools
                        ) { settingsStore.update { it.copy(autoApproveTools = !it.autoApproveTools) } }
                    }
                }

                // About
                item {
                    SectionCard(title = "关于", icon = Icons.Outlined.Info) {
                        SettingRow(
                            icon = Icons.Outlined.Info,
                            title = "版本号",
                            subtitle = "1.0.7"
                        ) {
                            versionTapCount++
                            if (versionTapCount >= 3) {
                                versionTapCount = 0
                                if (adminLoggedIn) {
                                    showAdminPanel = true
                                } else {
                                    // Trigger login
                                    showAdminPanel = true
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
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
        Icon(Icons.Outlined.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
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

@Composable
private fun AnnouncementItem(ann: Announcement) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (ann.pinned) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(16.dp)
                    .background(MaterialTheme.colorScheme.error, RoundedCornerShape(2.dp))
            )
            Spacer(Modifier.width(10.dp))
        } else {
            Spacer(Modifier.width(14.dp))
        }
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(ann.title, style = MaterialTheme.typography.bodyMedium)
                if (ann.pinned) {
                    Spacer(Modifier.width(6.dp))
                    Text("置顶", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
            }
            Text(ann.content, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminPanel(app: BskApp, onBack: () -> Unit) {
    val adminStore = remember { AnnouncementStore }
    var announcements by remember { mutableStateOf(adminStore.load(app)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("管理员", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onBack) {
                        androidx.compose.material3.Text("← 返回", color = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(announcements) { ann ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(ann.title, style = MaterialTheme.typography.titleSmall)
                            if (ann.pinned) {
                                Spacer(Modifier.width(6.dp))
                                androidx.compose.material3.Text("置顶", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(ann.content, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            androidx.compose.material3.TextButton(onClick = { /* edit */ }) {
                                androidx.compose.material3.Text("编辑", fontSize = 12.sp)
                            }
                            androidx.compose.material3.TextButton(onClick = {
                                announcements = announcements.filter { it.id != ann.id }
                                adminStore.save(app, announcements)
                            }) {
                                androidx.compose.material3.Text("删除", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
            item {
                androidx.compose.material3.Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { /* new announcement */ },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    androidx.compose.material3.Text("发布新公告")
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
