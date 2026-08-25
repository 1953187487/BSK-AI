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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.bskai.BskApp
import com.bskai.core.admin.AdminAuth
import com.bskai.core.admin.Announcement
import com.bskai.core.admin.AnnouncementStore
import com.bskai.core.settings.BskLanguage
import com.bskai.core.settings.OrchestrationMode
import com.bskai.ui.screens.providers.ProvidersScreen
import com.bskai.ui.theme.BskAmber
import com.bskai.ui.theme.BskEmerald
import com.bskai.ui.theme.BskIndigo
import com.bskai.ui.theme.BskRose
import java.net.URL

@Composable
fun SettingsScreen(app: BskApp) {
    var showProviders by rememberSaveable { mutableStateOf(false) }
    if (showProviders) {
        ProvidersScreen(app) { showProviders = false }
        return
    }
    MainSettings(app) { showProviders = true }
}

@Composable
private fun MainSettings(app: BskApp, onOpenProviders: () -> Unit) {
    val settingsStore = app.settingsStore
    val settings by settingsStore.settings.collectAsState()
    val announcements = remember { mutableStateOf(AnnouncementStore.load(app)) }
    var adminLoggedIn by remember { mutableStateOf(AdminAuth.isLoggedIn) }
    var showLogin by remember { mutableStateOf(false) }
    var showEditor by remember { mutableStateOf<Announcement?>(null) }
    var showNewEditor by remember { mutableStateOf(false) }
    val versionTaps = remember { mutableIntStateOf(0) }

    fun refreshAnnouncements() {
        announcements.value = AnnouncementStore.load(app)
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text("设置", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                SectionTitle("外观")
                SettingCard {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
                        Text("深色模式", Modifier.weight(1f))
                        Switch(
                            checked = settings.darkTheme,
                            onCheckedChange = { checked ->
                                settingsStore.update { s -> s.copy(darkTheme = checked) }
                            }
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                        Text("壁纸动态取色", Modifier.weight(1f))
                        Switch(
                            checked = settings.dynamicColor,
                            onCheckedChange = { checked ->
                                settingsStore.update { s -> s.copy(dynamicColor = checked) }
                            }
                        )
                    }
                    Column(Modifier.padding(12.dp)) {
                        Text("主题色", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AccentChip("靛蓝", BskIndigo, settings.accentColor == BskIndigo.value.toLong()) {
                                settingsStore.update { it.copy(accentColor = BskIndigo.value.toLong()) }
                            }
                            AccentChip("翠绿", BskEmerald, settings.accentColor == BskEmerald.value.toLong()) {
                                settingsStore.update { it.copy(accentColor = BskEmerald.value.toLong()) }
                            }
                            AccentChip("琥珀", BskAmber, settings.accentColor == BskAmber.value.toLong()) {
                                settingsStore.update { it.copy(accentColor = BskAmber.value.toLong()) }
                            }
                            AccentChip("玫红", BskRose, settings.accentColor == BskRose.value.toLong()) {
                                settingsStore.update { it.copy(accentColor = BskRose.value.toLong()) }
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                        Text("语言", Modifier.weight(1f))
                        Text(
                            settings.language.display,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item {
                SectionTitle("智能体行为")
                SettingCard {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text("自动批准工具调用", Modifier.weight(1f))
                        Switch(
                            checked = settings.autoApproveTools,
                            onCheckedChange = { checked ->
                                settingsStore.update { s -> s.copy(autoApproveTools = checked) }
                            }
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text("编排模式", Modifier.weight(1f))
                        FilterChip(
                            selected = settings.orchestrationMode == OrchestrationMode.SEQUENTIAL,
                            onClick = { settingsStore.update { it.copy(orchestrationMode = OrchestrationMode.SEQUENTIAL) } },
                            label = { Text("串行") }
                        )
                        Spacer(Modifier.width(8.dp))
                        FilterChip(
                            selected = settings.orchestrationMode == OrchestrationMode.PARALLEL,
                            onClick = { settingsStore.update { it.copy(orchestrationMode = OrchestrationMode.PARALLEL) } },
                            label = { Text("并行") }
                        )
                    }
                }
            }
            item {
                SectionTitle("模型服务商")
                SettingCard {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("配置 OpenAI 兼容服务商", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "自定义 Base URL / API Key / 模型列表",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(onClick = onOpenProviders) { Text("配置") }
                    }
                }
            }
            item {
                SectionTitle("公告")
                if (adminLoggedIn) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                        Text(
                            "管理员模式",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.weight(1f))
                        Button(onClick = { showNewEditor = true }) {
                            Icon(Icons.Outlined.Add, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("发布公告")
                        }
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = {
                            AdminAuth.logout()
                            adminLoggedIn = false
                        }) { Text("退出") }
                    }
                }
                if (announcements.value.isEmpty()) {
                    Text(
                        "暂无公告",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    announcements.value.forEach { ann ->
                        AnnouncementCard(
                            ann = ann,
                            canEdit = adminLoggedIn,
                            onEdit = { showEditor = ann },
                            onDelete = {
                                AnnouncementStore.delete(app, ann.id)
                                refreshAnnouncements()
                            }
                        )
                    }
                }
            }
            item {
                SectionTitle("关于")
                SettingCard {
                    AboutRow(
                        app,
                        onVersionTap = {
                            versionTaps.intValue++
                            if (versionTaps.intValue >= 3 && !adminLoggedIn) {
                                versionTaps.intValue = 0
                                showLogin = true
                            }
                        }
                    )
                }
            }
        }
    }

    if (showLogin) {
        LoginDialog(
            onDismiss = { showLogin = false },
            onSuccess = {
                adminLoggedIn = true
                showLogin = false
                refreshAnnouncements()
            }
        )
    }

    val editor = showNewEditor
    if (editor) {
        AnnouncementEditorDialog(
            title = "发布公告",
            initial = null,
            onDismiss = { showNewEditor = false },
            onConfirm = { t, c, p ->
                AnnouncementStore.publish(app, t, c, p)
                showNewEditor = false
                refreshAnnouncements()
            }
        )
    }

    showEditor?.let { ann ->
        AnnouncementEditorDialog(
            title = "编辑公告",
            initial = ann,
            onDismiss = { showEditor = null },
            onConfirm = { t, c, p ->
                AnnouncementStore.edit(app, ann.id, t, c, p)
                showEditor = null
                refreshAnnouncements()
            }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        content()
    }
}

@Composable
private fun AccentChip(label: String, color: Color, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .width(12.dp)
                        .height(12.dp)
                        .background(color, RoundedCornerShape(6.dp))
                )
                Spacer(Modifier.width(6.dp))
                Text(label)
            }
        }
    )
}

@Composable
private fun AboutRow(app: BskApp, onVersionTap: () -> Unit) {
    Column(Modifier.padding(12.dp)) {
        Text("BSK AI", style = MaterialTheme.typography.titleLarge)
        Text(
            "v1.0.6 · Claude Code × OpenClaw 技术 · 本地模型 · 设备端构建",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "版本号：1.0.6",
            style = MaterialTheme.typography.labelLarge,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(top = 6.dp)
                .clickableVersion(onVersionTap)
        )
        Text(
            "点击版本号 3 次进入隐藏入口",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun Modifier.clickableVersion(onTap: () -> Unit): Modifier =
    this.clickable(onClick = onTap)

@Composable
private fun AnnouncementCard(
    ann: Announcement,
    canEdit: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (ann.pinned) {
                    Text("置顶", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(6.dp))
                }
                Text(ann.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (canEdit) {
                    TextButton(onClick = onEdit) { Text("编辑") }
                    TextButton(onClick = onDelete) { Text("删除", color = MaterialTheme.colorScheme.error) }
                }
            }
            Text(ann.content, style = MaterialTheme.typography.bodyMedium)
            Text(
                java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                    .format(java.util.Date(ann.updatedAt)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun LoginDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var account by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.AdminPanelSettings, contentDescription = null) },
        title = { Text("管理员登录") },
        text = {
            Column {
                OutlinedTextField(
                    value = account,
                    onValueChange = { account = it },
                    label = { Text("账号") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("密码") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error) {
                    Text(
                        "账号或密码错误",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (AdminAuth.login(account, password)) {
                    onSuccess()
                } else {
                    error = true
                }
            }) {
                Text("登录")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun AnnouncementEditorDialog(
    title: String,
    initial: Announcement?,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Boolean) -> Unit
) {
    var t by remember { mutableStateOf(initial?.title ?: "") }
    var c by remember { mutableStateOf(initial?.content ?: "") }
    var pinned by remember { mutableStateOf(initial?.pinned ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(value = t, onValueChange = { t = it }, label = { Text("公告标题") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = c,
                    onValueChange = { c = it },
                    label = { Text("公告内容") },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("置顶", Modifier.weight(1f))
                    Switch(checked = pinned, onCheckedChange = { pinned = it })
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(t.trim(), c.trim(), pinned) }, enabled = t.isNotBlank() && c.isNotBlank()) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
