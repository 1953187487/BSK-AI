package com.floatai.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.floatai.App
import com.floatai.R
import com.floatai.data.model.AppSettings
import com.floatai.ui.components.GlassCard
import com.floatai.ui.components.SectionTitle
import com.floatai.ui.components.SettingsRow
import com.floatai.ui.theme.AccentOptions
import com.floatai.ui.theme.accentColorByName
import com.floatai.service.FloatService

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.applicationContext as App
    val vm: SettingsViewModel = viewModel(
        key = "settings",
        factory = SettingsViewModel.factory(app.settingsRepository)
    )
    val settings by vm.settings.collectAsStateWithLifecycle()
    val updateState by vm.update.collectAsStateWithLifecycle()

    var showPermissionNotice by remember { mutableStateOf(false) }
    var showFloatGrants by remember { mutableStateOf(false) }
    var localMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("设置", style = MaterialTheme.typography.headlineMedium)

        SectionTitle("常规设置")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                SettingsRow(
                    title = "界面主题",
                    subtitle = "切换亮色 / 深色外观"
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (settings.darkTheme) "深色" else "亮色",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Switch(
                            checked = settings.darkTheme,
                            onCheckedChange = vm::setDarkTheme
                        )
                    }
                }
                SettingsRow(
                    title = "动态取色",
                    subtitle = "Android 12+ 跟随壁纸自动配色"
                ) {
                    Switch(
                        checked = settings.dynamicColor,
                        onCheckedChange = vm::setDynamicColor
                    )
                }
                SettingsRow(
                    title = "主题色",
                    subtitle = "选择主色调，立即生效"
                ) {
                    ThemeColorPicker(
                        selected = settings.accentColor,
                        onSelect = vm::setAccentColor
                    )
                }
            }
        }

        SectionTitle("悬浮窗控制")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                SettingsRow(
                    title = "启用悬浮窗按钮",
                    subtitle = "在其他应用上层显示快捷操作按钮"
                ) {
                    Switch(
                        checked = settings.floatEnabled,
                        onCheckedChange = { enabled ->
                            vm.setFloatEnabled(enabled)
                            if (enabled) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                                    !Settings.canDrawOverlays(context)
                                ) {
                                    showFloatGrants = true
                                } else {
                                    context.startForegroundService(
                                        Intent(context, FloatService::class.java)
                                    )
                                    localMessage = "悬浮窗已启动"
                                }
                            } else {
                                context.stopService(Intent(context, FloatService::class.java))
                                localMessage = "悬浮窗已关闭"
                            }
                        }
                    )
                }
            }
        }

        SectionTitle("权限管理")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                SettingsRow(
                    title = "悬浮窗权限",
                    subtitle = "用于 AI 悬浮窗显示在其他应用之上"
                ) {
                    TextButton(onClick = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    }) { Text("去授权") }
                }
                TextButton(onClick = { showPermissionNotice = true }) {
                    Text("查看权限说明")
                }
            }
        }

        SectionTitle("系统维护")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                SettingsRow(
                    title = "检查更新",
                    subtitle = "自动从 GitHub Release 拉取最新版本"
                ) {
                    if (updateState.checking) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp))
                    } else {
                        Button(onClick = { vm.checkUpdate("v1.0.0") }) { Text("点击检测") }
                    }
                }
                if (updateState.message.isNotEmpty()) {
                    Text(
                        updateState.message,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                localMessage?.let { msg ->
                    Text(
                        msg,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        SectionTitle("关于")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(
                    "FloatAI v1.0.0",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    "开源 Android AI 悬浮助手",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    "MIT License",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Button(
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:" + context.getString(R.string.contact_email))
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                ) { Text("发送邮件: 1953187487@qq.com") }
                Button(
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.repo_url)))
                        )
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) { Text("访问 GitHub 开源仓库") }
            }
        }
    }

    if (showFloatGrants) {
        AlertDialog(
            onDismissRequest = { showFloatGrants = false },
            title = { Text("需要悬浮窗权限") },
            text = {
                Text("FloatAI 需要在其他应用上层显示以启动悬浮窗，请前往系统设置授权。")
            },
            confirmButton = {
                TextButton(onClick = {
                    showFloatGrants = false
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                }) { Text("去授权") }
            },
            dismissButton = {
                TextButton(onClick = { showFloatGrants = false }) { Text("取消") }
            }
        )
    }

    if (showPermissionNotice) {
        AlertDialog(
            onDismissRequest = { showPermissionNotice = false },
            title = { Text("权限说明") },
            text = { Text(context.getString(R.string.permission_notice), fontSize = 12.sp) },
            confirmButton = {
                TextButton(onClick = { showPermissionNotice = false }) { Text("知道了") }
            }
        )
    }

    updateState.notice?.let { notice ->
        AlertDialog(
            onDismissRequest = vm::dismissUpdate,
            title = { Text("发现新版本 ${notice.latestTag}", color = MaterialTheme.colorScheme.primary) },
            text = {
                Column {
                    Text("请前往 GitHub Releases 下载更新", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        notice.changelog.ifEmpty { "无更新说明" },
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.repo_releases_url)))
                    )
                    vm.dismissUpdate()
                }) { Text("前往下载") }
            },
            dismissButton = {
                TextButton(onClick = vm::dismissUpdate) { Text("稍后") }
            }
        )
    }
}

@Composable
fun ThemeColorPicker(selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)
    ) {
        AccentOptions.forEach { option ->
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(option.color, CircleShape)
                    .clickable { onSelect(option.name) }
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                if (option.name == selected) {
                    Icon(
                        Icons.Filled.Circle,
                        contentDescription = option.name,
                        tint = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
