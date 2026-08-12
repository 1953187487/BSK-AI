package com.floatai.ui.screens.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.SettingsApplications
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.floatai.App
import com.floatai.R
import com.floatai.data.model.AppLanguage
import com.floatai.service.FloatService
import com.floatai.ui.components.GlassCard
import com.floatai.ui.components.SectionTitle
import com.floatai.ui.components.SettingsRow
import com.floatai.ui.i18n.localStrings
import com.floatai.ui.theme.AccentOptions

private data class PermissionRow(
    val key: String,
    val display: String,
    val rationale: String,
    val icon: ImageVector,
    val manifestPermission: String?,
    val checkGranted: (android.content.Context) -> Boolean,
    val revoke: (android.app.Activity) -> Unit
)

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
    val strings = localStrings()

    var showPermissionNotice by remember { mutableStateOf(false) }
    var showFloatGrants by remember { mutableStateOf(false) }
    var showTokenDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var localMessage by remember { mutableStateOf<String?>(null) }
    var permissionRevokedTick by remember { mutableStateOf(0) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        permissionRevokedTick++
    }

    // 权限列表（运行时权限 + 特殊权限）
    val permRows = remember(permissionRevokedTick) {
        listOf(
            PermissionRow(
                key = "notifications",
                display = "通知 (POST_NOTIFICATIONS)",
                rationale = "Android 13+ 通知权限",
                icon = Icons.Filled.Notifications,
                manifestPermission = Manifest.permission.POST_NOTIFICATIONS,
                checkGranted = { ctx ->
                    if (Build.VERSION.SDK_INT >= 33) {
                        ContextCompat.checkSelfPermission(
                            ctx, Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                    } else true
                },
                revoke = { activity ->
                    if (Build.VERSION.SDK_INT >= 33) {
                        activity.revokeSelfPermissionOnKill(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            ),
            PermissionRow(
                key = "microphone",
                display = "麦克风 (RECORD_AUDIO)",
                rationale = "语音输入",
                icon = Icons.Filled.Mic,
                manifestPermission = Manifest.permission.RECORD_AUDIO,
                checkGranted = { ctx ->
                    ContextCompat.checkSelfPermission(
                        ctx, Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                },
                revoke = { activity ->
                    activity.revokeSelfPermissionOnKill(Manifest.permission.RECORD_AUDIO)
                }
            ),
            PermissionRow(
                key = "camera",
                display = "相机 (CAMERA)",
                rationale = "拍照 / OCR",
                icon = Icons.Filled.Videocam,
                manifestPermission = Manifest.permission.CAMERA,
                checkGranted = { ctx ->
                    ContextCompat.checkSelfPermission(
                        ctx, Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                },
                revoke = { activity ->
                    activity.revokeSelfPermissionOnKill(Manifest.permission.CAMERA)
                }
            ),
            PermissionRow(
                key = "overlay",
                display = "悬浮窗 (SYSTEM_ALERT_WINDOW)",
                rationale = "需到系统设置中关闭「显示在其他应用上层」",
                icon = Icons.Filled.SettingsApplications,
                manifestPermission = null,
                checkGranted = { ctx -> Settings.canDrawOverlays(ctx) },
                revoke = { _ ->
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                }
            ),
            PermissionRow(
                key = "usage",
                display = "使用情况访问 (PACKAGE_USAGE_STATS)",
                rationale = "需到系统设置中关闭「使用权访问」",
                icon = Icons.Filled.PrivacyTip,
                manifestPermission = null,
                checkGranted = { ctx ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        val mode = ctx.getSystemService(android.app.AppOpsManager::class.java)
                            ?.unsafeCheckOpNoThrow(
                                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                                android.os.Process.myUid(),
                                ctx.packageName
                            ) ?: android.app.AppOpsManager.MODE_DEFAULT
                        mode == android.app.AppOpsManager.MODE_ALLOWED
                    } else true
                },
                revoke = { _ ->
                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    context.startActivity(intent)
                }
            ),
            PermissionRow(
                key = "vpn",
                display = "VPN 服务 (VpnService)",
                rationale = "需到系统设置中关闭对应的 VPN 配置",
                icon = Icons.Filled.VpnLock,
                manifestPermission = null,
                checkGranted = { _ -> false },
                revoke = { _ ->
                    val intent = Intent(Settings.ACTION_VPN_SETTINGS)
                    context.startActivity(intent)
                }
            )
        )
    }

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(strings.app_name + " · " + strings.nav_settings, style = MaterialTheme.typography.headlineMedium)

        SectionTitle(strings.settings_general)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                SettingsRow(
                    title = strings.settings_theme_title,
                    subtitle = if (settings.darkTheme) "Dark" else "Light"
                ) {
                    Switch(checked = settings.darkTheme, onCheckedChange = vm::setDarkTheme)
                }
                SettingsRow(
                    title = strings.settings_dynamic_color,
                    subtitle = "Android 12+ wallpaper-based"
                ) {
                    Switch(checked = settings.dynamicColor, onCheckedChange = vm::setDynamicColor)
                }
                SettingsRow(
                    title = strings.settings_theme_color,
                    subtitle = settings.accentColor
                ) {
                    ThemeColorPicker(selected = settings.accentColor, onSelect = vm::setAccentColor)
                }
                SettingsRow(
                    title = strings.settings_language,
                    subtitle = if (settings.language == AppLanguage.ZH) strings.language_zh else strings.language_en
                ) {
                    Icon(
                        Icons.Filled.Language,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { showLanguageDialog = true }
                    )
                }
            }
        }

        SectionTitle(strings.settings_float_section)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                SettingsRow(
                    title = strings.settings_float_title,
                    subtitle = strings.settings_float_desc
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
                                    localMessage = strings.settings_float_title
                                }
                            } else {
                                context.stopService(Intent(context, FloatService::class.java))
                                localMessage = strings.settings_float_title
                            }
                        }
                    )
                }
            }
        }

        SectionTitle(strings.settings_permission_section)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                permRows.forEach { row ->
                    val granted = row.checkGranted(context)
                    SettingsRow(
                        title = row.display,
                        subtitle = if (granted) "已授权 — 点击撤销" else "未授权"
                    ) {
                        TextButton(onClick = {
                            (context as? android.app.Activity)?.let { row.revoke(it) }
                                ?: run {
                                    row.manifestPermission?.let {
                                        permissionLauncher.launch(arrayOf(it))
                                    }
                                }
                            permissionRevokedTick++
                            localMessage = "已尝试撤销：${row.display}"
                        }) { Text("撤销") }
                    }
                }
                TextButton(onClick = { showPermissionNotice = true }) {
                    Text(strings.settings_permission_notice)
                }
            }
        }

        SectionTitle(strings.settings_github_section)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                SettingsRow(
                    title = strings.settings_github_token,
                    subtitle = if (settings.githubToken.isNotBlank()) "●●●●●●●●" + settings.githubToken.takeLast(4)
                    else strings.settings_github_token_desc
                ) {
                    TextButton(onClick = { showTokenDialog = true }) { Text("✎") }
                }
                Text(
                    text = strings.settings_github_token_desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }

        SectionTitle(strings.settings_about_section)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Spacer4dp()
                Text(strings.settings_about_app, style = MaterialTheme.typography.titleLarge)
                Text(
                    strings.settings_about_desc,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    strings.settings_about_license,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall
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
                ) { Text(strings.settings_contact_email) }
                Button(
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.repo_url)))
                        )
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) { Text(strings.settings_open_repo) }
            }
        }
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(strings.language_choose_title) },
            text = {
                Column {
                    TextButton(onClick = {
                        vm.setLanguage(AppLanguage.ZH); showLanguageDialog = false
                    }) { Text(strings.language_zh) }
                    TextButton(onClick = {
                        vm.setLanguage(AppLanguage.EN); showLanguageDialog = false
                    }) { Text(strings.language_en) }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) { Text("关闭") }
            }
        )
    }

    if (showFloatGrants) {
        AlertDialog(
            onDismissRequest = { showFloatGrants = false },
            title = { Text(strings.settings_float_title) },
            text = { Text(strings.settings_float_desc) },
            confirmButton = {
                TextButton(onClick = {
                    showFloatGrants = false
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                }) { Text("→") }
            },
            dismissButton = {
                TextButton(onClick = { showFloatGrants = false }) { Text("✕") }
            }
        )
    }

    if (showPermissionNotice) {
        AlertDialog(
            onDismissRequest = { showPermissionNotice = false },
            title = { Text(strings.permission_notice_title) },
            text = { Text(strings.permission_notice_body, fontSize = 12.sp) },
            confirmButton = {
                TextButton(onClick = { showPermissionNotice = false }) { Text("✕") }
            }
        )
    }

    if (showTokenDialog) {
        var token by remember { mutableStateOf(settings.githubToken) }
        AlertDialog(
            onDismissRequest = { showTokenDialog = false },
            title = { Text(strings.settings_github_token) },
            text = {
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text("ghp_...") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.setGithubToken(token); showTokenDialog = false
                }) { Text("✓") }
            },
            dismissButton = {
                TextButton(onClick = { showTokenDialog = false }) { Text("✕") }
            }
        )
    }
}

@Composable
private fun Spacer4dp() {
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun ThemeColorPicker(selected: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AccentOptions.forEach { option ->
            val isSelected = option.name == selected
            Box(
                modifier = Modifier
                    .size(if (isSelected) 26.dp else 20.dp)
                    .background(option.color, CircleShape)
                    .clickable { onSelect(option.name) }
            )
        }
    }
}
