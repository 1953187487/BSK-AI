package com.floatai.ui.screens.settings

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SettingsApplications
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.floatai.App
import com.floatai.R
import com.floatai.data.model.AppLanguage
import com.floatai.data.model.UpdateInfo
import com.floatai.data.remote.ApkDownloadProgress
import com.floatai.data.remote.UpdateRepository
import com.floatai.service.FloatService
import com.floatai.ui.components.GlassCard
import com.floatai.ui.components.SectionTitle
import com.floatai.ui.components.SettingsRow
import com.floatai.ui.i18n.localStrings
import com.floatai.ui.theme.AccentOptions
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.io.File

/**
 * 设置页 v1.0.4 重构：
 *  - 「权限」整合为一个区块，每行点击直接跳转对应授权界面（无撤销按钮）
 *  - 「关于」融入设置（独立区域，包含检查更新 + 应用内直连下载 + 历史 release）
 *  - 输入即自动保存（debounce 500ms），无保存按钮
 *  - 比例优化：紧凑布局，圆角统一
 *  - 适配液态玻璃主题
 */
@OptIn(FlowPreview::class)
@Composable
fun SettingsScreen(
    onOpenAbout: () -> Unit,
    onOpenPackageHub: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as App
    val vm: SettingsViewModel = viewModel(
        key = "settings",
        factory = SettingsViewModel.factory(app.settingsRepository)
    )
    val settings by vm.settings.collectAsStateWithLifecycle()
    val strings = localStrings()
    val scope = rememberCoroutineScope()

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showFloatGrants by remember { mutableStateOf(false) }
    var showApiConfig by remember { mutableStateOf(false) }
    var showTokenDialog by remember { mutableStateOf(false) }
    var lastMessage by remember { mutableStateOf<String?>(null) }

    // —— 输入即保存：监听 apiKey / githubToken / model 的变化，debounce 500ms 后写入 ——
    var pendingApiKey by remember { mutableStateOf(app.settingsRepository.apiConfig.value.apiKey) }
    var pendingGithubToken by remember { mutableStateOf(settings.githubToken) }
    var pendingModel by remember { mutableStateOf(app.settingsRepository.apiConfig.value.model) }

    LaunchedEffect(pendingApiKey, pendingModel) {
        snapshotFlow { pendingApiKey to pendingModel }
            .distinctUntilChanged()
            .debounce(500)
            .collectLatest { (key, model) ->
                app.settingsRepository.updateApiConfig {
                    it.copy(apiKey = key, model = model)
                }
            }
    }
    LaunchedEffect(pendingGithubToken) {
        snapshotFlow { pendingGithubToken }
            .distinctUntilChanged()
            .debounce(500)
            .collectLatest { token ->
                vm.setGithubToken(token)
            }
    }

    // —— 权限整合：每个权限 = 一行，点击跳对应授权界面 ——
    val permRows = remember {
        listOf(
            PermissionRow(
                key = "overlay",
                display = strings.perm_overlay,
                rationale = strings.perm_overlay_desc,
                icon = Icons.Filled.SettingsApplications,
                checkGranted = { ctx -> Settings.canDrawOverlays(ctx) },
                grantIntent = { ctx ->
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${ctx.packageName}")
                    )
                }
            ),
            PermissionRow(
                key = "notifications",
                display = strings.perm_notifications,
                rationale = strings.perm_notifications_desc,
                icon = Icons.Filled.Notifications,
                manifestPermission = Manifest.permission.POST_NOTIFICATIONS,
                checkGranted = { ctx ->
                    if (Build.VERSION.SDK_INT >= 33) {
                        ContextCompat.checkSelfPermission(
                            ctx, Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                    } else true
                },
                grantIntent = { ctx ->
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                }
            ),
            PermissionRow(
                key = "microphone",
                display = strings.perm_microphone,
                rationale = strings.perm_microphone_desc,
                icon = Icons.Filled.Mic,
                manifestPermission = Manifest.permission.RECORD_AUDIO,
                checkGranted = { ctx ->
                    ContextCompat.checkSelfPermission(
                        ctx, Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                },
                grantIntent = { ctx ->
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(Uri.fromParts("package", ctx.packageName, null))
                }
            ),
            PermissionRow(
                key = "camera",
                display = strings.perm_camera,
                rationale = strings.perm_camera_desc,
                icon = Icons.Filled.Videocam,
                manifestPermission = Manifest.permission.CAMERA,
                checkGranted = { ctx ->
                    ContextCompat.checkSelfPermission(
                        ctx, Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                },
                grantIntent = { ctx ->
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(Uri.fromParts("package", ctx.packageName, null))
                }
            ),
            PermissionRow(
                key = "usage",
                display = strings.perm_usage,
                rationale = strings.perm_usage_desc,
                icon = Icons.Filled.PrivacyTip,
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
                grantIntent = { _ ->
                    Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                }
            ),
            PermissionRow(
                key = "vpn",
                display = strings.perm_vpn,
                rationale = strings.perm_vpn_desc,
                icon = Icons.Filled.VpnLock,
                checkGranted = { ctx ->
                    // 简单判断：是否存在活跃 VPN 接口
                    try {
                        val cm = ctx.getSystemService(android.net.ConnectivityManager::class.java)
                        val active = cm?.activeNetworkInfo
                        active?.type == android.net.ConnectivityManager.TYPE_VPN
                    } catch (_: Exception) { false }
                },
                grantIntent = { _ -> Intent(Settings.ACTION_VPN_SETTINGS) }
            ),
            PermissionRow(
                key = "lobster_accessibility",
                display = strings.perm_lobster,
                rationale = strings.perm_lobster_desc,
                icon = Icons.Filled.Pets,
                checkGranted = { ctx ->
                    val expected = "com.floatai/com.floatai.lobster.LobsterAccessibilityService"
                    try {
                        Settings.Secure.getString(
                            ctx.contentResolver,
                            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                        )?.contains(expected) == true
                    } catch (_: Exception) { false }
                },
                grantIntent = { _ ->
                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                }
            )
        )
    }
    val permRefreshTick = remember { mutableStateOf(0) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> permRefreshTick.value++ }

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        // —— Header ——
        Text(
            text = strings.app_name,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = strings.nav_settings,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )

        // —— 通用 ——
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
                    subtitle = if (settings.language == AppLanguage.ZH) strings.language_zh else strings.language_en,
                    onClick = { showLanguageDialog = true }
                ) {
                    Icon(
                        Icons.Filled.Language,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // —— 悬浮窗 ——
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
                                }
                            } else {
                                context.stopService(Intent(context, FloatService::class.java))
                            }
                        }
                    )
                }
            }
        }

        // —— 权限整合 ——
        SectionTitle(strings.settings_permission_section)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                permRows.forEach { row ->
                    val granted by remember(permRefreshTick.value) {
                        derivedStateOf { row.checkGranted(context) }
                    }
                    SettingsRow(
                        title = row.display,
                        subtitle = if (granted) strings.perm_granted else strings.perm_not_granted,
                        onClick = {
                            // 点击跳对应授权界面（统一行为，无撤销按钮）
                            try {
                                val intent = row.grantIntent(context)
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            } catch (e: ActivityNotFoundException) {
                                lastMessage = strings.perm_no_settings
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (granted) Icons.Filled.CheckCircle else row.icon,
                            contentDescription = null,
                            tint = if (granted) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.size(6.dp))
                        Icon(
                            Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                // 运行时权限需要弹系统弹框的（POST_NOTIFICATIONS 等）
                TextButton(onClick = {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.POST_NOTIFICATIONS,
                            Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.CAMERA
                        )
                    )
                }) {
                    Text("🔒 " + strings.settings_permission_request)
                }
            }
        }

        // —— AI 模型配置（输入即保存） ——
        SectionTitle(strings.settings_ai_section)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                SettingsRow(
                    title = strings.settings_api_base,
                    subtitle = app.settingsRepository.apiConfig.value.baseUrl.ifBlank { "(未设置)" },
                    onClick = { showApiConfig = true }
                )
                SettingsRow(
                    title = strings.settings_api_model,
                    subtitle = pendingModel.ifBlank { "auto" }
                )
                OutlinedTextField(
                    value = pendingModel,
                    onValueChange = { pendingModel = it },
                    label = { Text("model") },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = pendingApiKey,
                    onValueChange = { pendingApiKey = it },
                    label = { Text("apiKey (自动保存)") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    singleLine = true
                )
                // 保存状态指示
                val savedKey = app.settingsRepository.apiConfig.value.apiKey
                Text(
                    text = if (savedKey.isNotBlank()) "✓ ${strings.perm_granted}" else "○ ${strings.perm_not_granted}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }

        // —— GitHub Token ——
        SectionTitle(strings.settings_github_section)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                SettingsRow(
                    title = strings.settings_github_token,
                    subtitle = if (pendingGithubToken.isNotBlank())
                        "●●●●●●" + pendingGithubToken.takeLast(4)
                    else strings.settings_github_token_desc
                )
                OutlinedTextField(
                    value = pendingGithubToken,
                    onValueChange = { pendingGithubToken = it },
                    label = { Text("ghp_...") },
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    singleLine = true
                )
                Text(
                    text = strings.settings_github_token_desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // —— 关于（融入设置） ——
        SectionTitle(strings.settings_about_section)
        AboutBlock(onOpenDetail = onOpenAbout)

        // —— 包管理入口 ——
        SectionTitle("包与扩展")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                SettingsRow(
                    title = strings.nav_package_hub,
                    subtitle = "插件 / 技能 / 小龙虾扩展",
                    onClick = onOpenPackageHub
                ) {
                    Icon(Icons.Filled.Extension, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // —— 调试入口（开发期） ——
        SectionTitle(strings.settings_advanced_section)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                SettingsRow(
                    title = "联系作者",
                    subtitle = context.getString(R.string.contact_email),
                    onClick = {
                        // 点击 = 复制邮箱到剪贴板（不再调邮件客户端，避免部分设备无邮件 App 时无反应）
                        val email = context.getString(R.string.contact_email)
                        val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
                        clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("email", email))
                        android.widget.Toast.makeText(
                            context,
                            "✓ 已复制邮箱：$email",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                )
                SettingsRow(
                    title = "GitHub 仓库",
                    subtitle = context.getString(R.string.repo_url),
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.repo_url)))
                            )
                        }
                    }
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }

    // —— Dialogs ——
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(strings.language_choose_title) },
            text = {
                Column {
                    TextButton(onClick = {
                        vm.setLanguage(AppLanguage.ZH); showLanguageDialog = false
                    }) { Text("🇨🇳 " + strings.language_zh) }
                    TextButton(onClick = {
                        vm.setLanguage(AppLanguage.EN); showLanguageDialog = false
                    }) { Text("🇺🇸 " + strings.language_en) }
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
                    runCatching {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                        )
                    }
                }) { Text("→") }
            },
            dismissButton = {
                TextButton(onClick = { showFloatGrants = false }) { Text("✕") }
            }
        )
    }

    if (showApiConfig) {
        var baseUrl by remember { mutableStateOf(app.settingsRepository.apiConfig.value.baseUrl) }
        LaunchedEffect(baseUrl) {
            snapshotFlow { baseUrl }.distinctUntilChanged().debounce(500).collectLatest { url ->
                app.settingsRepository.updateApiConfig { it.copy(baseUrl = url) }
            }
        }
        AlertDialog(
            onDismissRequest = { showApiConfig = false },
            title = { Text(strings.settings_api_base) },
            text = {
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("https://api.openai.com/v1") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = { showApiConfig = false }) { Text("✓") }
            }
        )
    }
}

private data class PermissionRow(
    val key: String,
    val display: String,
    val rationale: String,
    val icon: ImageVector,
    val manifestPermission: String? = null,
    val checkGranted: (android.content.Context) -> Boolean,
    val grantIntent: (android.content.Context) -> Intent
)

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

/**
 * 关于区块（融入设置）：
 *  - 版本信息 + 协议版本 + 构建类型
 *  - 「检查更新」按钮：检查后若有新版，「下载」按钮直接调用 GitHub 直链下载
 *  - 下载进度条 + 速度 + 校验和显示
 *  - 「立即安装」按钮触发 PackageInstaller
 *  - 「查看完整更新日志」跳独立 About 页
 */
@Composable
private fun AboutBlock(onOpenDetail: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val strings = localStrings()
    var checking by remember { mutableStateOf(false) }
    var info by remember { mutableStateOf<UpdateInfo?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var downloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf<ApkDownloadProgress?>(null) }
    var downloadedApk by remember { mutableStateOf<File?>(null) }
    var verifying by remember { mutableStateOf(false) }

    val apkFile = remember { File(context.cacheDir, "update.apk") }

    fun startDownload(url: String) {
        downloading = true
        downloadProgress = ApkDownloadProgress.Started(url, -1L)
        scope.launch {
            UpdateRepository.downloadApk(apkFile, url).collect { p ->
                downloadProgress = p
                if (p is ApkDownloadProgress.Verifying) verifying = true
                if (p is ApkDownloadProgress.Completed) {
                    downloading = false
                    verifying = false
                    downloadedApk = p.file
                    message = "✓ ${strings.update_ready}"
                }
                if (p is ApkDownloadProgress.Error) {
                    downloading = false
                    verifying = false
                    message = strings.update_failed + ": ${p.message}"
                }
            }
        }
    }

    fun checkUpdate() {
        checking = true
        message = null
        scope.launch {
            val currentTag = "v${com.floatai.BuildConfig.VERSION_NAME}"
            val latest = UpdateRepository.checkLatest(currentTag)
            info = latest
            checking = false
            message = when {
                latest.latestTag.isEmpty() -> latest.changelog
                latest.isNewer -> "${latest.latestTag}: ${latest.changelog.take(80)}"
                else -> strings.update_latest
            }
        }
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Info, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.size(8.dp))
                Column {
                    Text(strings.settings_about_app, style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "v${com.floatai.BuildConfig.VERSION_NAME} · 协议 v${com.floatai.BuildConfig.PROTOCOL_VERSION}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                strings.settings_about_desc,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                strings.settings_about_license,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(Modifier.height(12.dp))

            // 检查更新
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { checkUpdate() }, enabled = !checking) {
                    if (checking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Filled.Refresh, contentDescription = null)
                    }
                    Spacer(Modifier.size(6.dp))
                    Text(strings.update_check)
                }
                if (info?.isNewer == true) {
                    Spacer(Modifier.size(6.dp))
                    AssistChip(
                        onClick = {
                            // 直连下载
                            val tag = info?.latestTag ?: return@AssistChip
                            scope.launch {
                                val asset = UpdateRepository.findApkAsset(tag)
                                if (asset != null) startDownload(asset.url)
                                else message = strings.update_no_apk
                            }
                        },
                        label = { Text(strings.update_download) },
                        leadingIcon = {
                            if (downloading) CircularProgressIndicator(
                                modifier = Modifier.size(14.dp), strokeWidth = 2.dp
                            ) else Icon(Icons.Filled.CloudDownload, contentDescription = null)
                        }
                    )
                }
            }

            // 下载进度
            downloadProgress?.let { p ->
                Spacer(Modifier.height(8.dp))
                when (p) {
                    is ApkDownloadProgress.Started -> {
                        Text("↓ ${strings.update_downloading}", fontSize = 11.sp)
                    }
                    is ApkDownloadProgress.Progress -> {
                        val frac = if (p.total > 0) (p.downloaded.toFloat() / p.total) else 0f
                        LinearProgressIndicator(
                            progress = { frac },
                            modifier = Modifier.fillMaxWidth()
                        )
                        val speedKb = p.speed / 1024
                        Text(
                            text = "${formatSize(p.downloaded)} / ${formatSize(p.total)}  ·  $speedKb KB/s",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    is ApkDownloadProgress.Verifying -> {
                        // 校验动画：旋转
                        val transition = rememberInfiniteTransition(label = "verify")
                        val angle by transition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(900, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "verify-angle"
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(14.dp)
                            )
                            Spacer(Modifier.size(6.dp))
                            Text(
                                "校验中 ${p.sha256.take(12)}...",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    is ApkDownloadProgress.Completed -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.size(6.dp))
                            Text(strings.update_ready, fontSize = 11.sp)
                        }
                    }
                    is ApkDownloadProgress.Error -> {
                        Text(
                            text = strings.update_failed + ": ${p.message}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // 安装按钮
            downloadedApk?.let { file ->
                Spacer(Modifier.height(6.dp))
                TextButton(onClick = {
                    installApk(context, file)
                }) {
                    Icon(Icons.Filled.Update, contentDescription = null)
                    Spacer(Modifier.size(4.dp))
                    Text(strings.update_install)
                }
            }

            message?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onOpenDetail) {
                Icon(Icons.Filled.ChevronRight, contentDescription = null)
                Spacer(Modifier.size(4.dp))
                Text(strings.update_history)
            }
        }
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes < 0) return "?"
    val units = arrayOf("B", "KB", "MB", "GB")
    var size = bytes.toDouble()
    var i = 0
    while (size >= 1024 && i < units.size - 1) { size /= 1024; i++ }
    return if (i == 0) "$bytes B" else "%.1f %s".format(size, units[i])
}

/** 调起系统 PackageInstaller 安装 APK。 */
private fun installApk(context: android.content.Context, file: File) {
    try {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        android.widget.Toast.makeText(
            context,
            "请先打开「安装未知应用」权限",
            android.widget.Toast.LENGTH_LONG
        ).show()
    }
}
