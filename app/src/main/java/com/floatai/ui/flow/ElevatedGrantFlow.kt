package com.floatai.ui.flow

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.floatai.data.model.AppLanguage
import com.floatai.perm.ElevatedGrant
import com.floatai.perm.ElevatedGrantDetector
import com.floatai.perm.GrantState
import com.floatai.ui.components.GlassCard
import com.floatai.ui.i18n.localStrings
import com.floatai.ui.theme.liquidBackdrop
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

/**
 * 高权限协议流（v1.0.1 第三次重做）。
 *
 * 设计原则：
 *  - 状态机显式：每个授权来源展示明确的 GrantState，用户随时知道"现在卡在哪一步"
 *  - 错误可操作：失败消息直接告诉用户"下一步做什么"
 *  - 无静默跳转：除非用户主动点击"手动下载"，绝不离开当前屏幕
 *  - listener 全局注册：在 ElevatedGrantFlow 进入屏幕期间注册一次，组件离开时移除，
 *    避免 Composable 重建导致重复回调
 */
@Composable
fun ElevatedGrantFlow(
    language: AppLanguage,
    onGranted: () -> Unit
) {
    val strings = localStrings()
    val context = LocalContext.current
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    var shizukuState by remember { mutableStateOf<GrantState>(GrantState.Detecting) }
    var dhizukuState by remember { mutableStateOf<GrantState>(GrantState.Detecting) }
    var rootState by remember { mutableStateOf<GrantState>(GrantState.Detecting) }

    var infoDialog by remember { mutableStateOf<InfoDialogKind?>(null) }

    // 进入屏幕时主动检测一次
    LaunchedEffect(Unit) {
        refreshAll(context, shizukuSetter = { shizukuState = it },
            dhizukuSetter = { dhizukuState = it },
            rootSetter = { rootState = it })
    }

    // 全局注册 Shizuku permission listener
    DisposableEffect(Unit) {
        val listener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
            val ok = grantResult == PackageManager.PERMISSION_GRANTED
            shizukuState = if (ok) GrantState.Granted
            else GrantState.Failed("Shizuku 权限被拒绝。可在 Shizuku App 中重新授权本应用。")
        }
        Shizuku.addRequestPermissionResultListener(listener)
        onDispose {
            Shizuku.removeRequestPermissionResultListener(listener)
        }
    }

    val anyGranted = shizukuState is GrantState.Granted
            || dhizukuState is GrantState.Granted
            || rootState is GrantState.Granted

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(liquidBackdrop(dark))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = strings.elevated_title,
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = strings.elevated_intro,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                AssistChip(
                    onClick = {
                        refreshAll(context, { shizukuState = it },
                            { dhizukuState = it }, { rootState = it })
                    },
                    label = { Text(strings.elevated_recheck) },
                    leadingIcon = { Icon(Icons.Filled.Refresh, contentDescription = null) }
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (anyGranted) strings.elevated_status("已就绪")
                    else strings.elevated_status("未授权"),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (anyGranted) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            ShizukuCard(
                state = shizukuState,
                context = context,
                onStateChange = { shizukuState = it },
                onInfo = { infoDialog = InfoDialogKind.SHIZUKU }
            )
            DhizukuCard(
                state = dhizukuState,
                context = context,
                onStateChange = { dhizukuState = it },
                onInfo = { infoDialog = InfoDialogKind.DHIZUKU }
            )
            RootCard(
                state = rootState,
                onStateChange = { rootState = it },
                onInfo = { infoDialog = InfoDialogKind.ROOT }
            )

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    refreshAll(context, { shizukuState = it },
                        { dhizukuState = it }, { rootState = it })
                    if (shizukuState is GrantState.Granted
                        || dhizukuState is GrantState.Granted
                        || rootState is GrantState.Granted
                    ) onGranted()
                },
                enabled = anyGranted,
                modifier = Modifier.fillMaxWidth()
            ) { Text(strings.elevated_continue) }

            if (!anyGranted) {
                Text(
                    text = strings.elevated_required,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    infoDialog?.let { kind ->
        InfoDialog(kind = kind, onDismiss = { infoDialog = null })
    }
}

private fun refreshAll(
    context: Context,
    shizukuSetter: (GrantState) -> Unit,
    dhizukuSetter: (GrantState) -> Unit,
    rootSetter: (GrantState) -> Unit
) {
    // Shizuku
    shizukuSetter(GrantState.Detecting)
    shizukuSetter(evalShizukuState(context))
    // Dhizuku
    dhizukuSetter(GrantState.Detecting)
    dhizukuSetter(evalDhizukuState(context))
    // Root
    rootSetter(GrantState.Detecting)
    rootSetter(evalRootState())
}

private fun evalShizukuState(context: Context): GrantState {
    if (!ElevatedGrantDetector.isShizukuInstalled(context)) {
        return GrantState.Unavailable(
            "设备未安装 Shizuku App。请点击下方「手动下载」安装，或前往 https://shizuku.rikka.app/download/ 下载并启动后回到此页面。"
        )
    }
    return runCatching {
        if (Shizuku.isPreV11()) {
            return@runCatching GrantState.Unavailable("Shizuku 版本过低（< v11），请更新 Shizuku App")
        }
        if (!Shizuku.pingBinder()) {
            return@runCatching GrantState.Unavailable(
                "Shizuku 服务未运行。请打开 Shizuku App 点击「启动」，然后回到此页面点击「重新检测」。"
            )
        }
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            GrantState.Granted
        } else {
            GrantState.NeedsPermission
        }
    }.getOrElse { t ->
        GrantState.Failed("检测失败：${t.message ?: t.javaClass.simpleName}")
    }
}

private fun evalDhizukuState(context: Context): GrantState {
    if (!ElevatedGrantDetector.isDhizukuInstalled(context)) {
        return GrantState.Unavailable("设备未安装 Dhizuku 客户端。Dhizuku 需要 Device Owner 激活，请点击「手动下载」获取详细指引。")
    }
    return runCatching {
        val cls = Class.forName("com.itsaky.androidide.dhizukudav.Dhizuku")
        val isAvailable = cls.getMethod("isAvailable").invoke(null) as? Boolean ?: false
        if (!isAvailable) {
            return@runCatching GrantState.Unavailable(
                "Dhizuku 服务未激活。请打开 Dhizuku 客户端完成 DeviceOwner 激活，然后回到此页面。"
            )
        }
        val isGranted = cls.getMethod("isPermissionGranted").invoke(null) as? Boolean ?: false
        if (isGranted) GrantState.Granted else GrantState.NeedsPermission
    }.getOrElse { t ->
        GrantState.Failed("检测失败：${t.message ?: t.javaClass.simpleName}")
    }
}

private fun evalRootState(): GrantState = runCatching {
    val process = ProcessBuilder("su", "-c", "true").start()
    val exit = process.waitFor()
    if (exit == 0) GrantState.Granted
    else GrantState.Unavailable("未检测到 su。请确认设备已 Root（Magisk / KernelSU 等）并授予本应用 Root 权限。")
}.getOrElse {
    GrantState.Unavailable("未检测到 Root。点击「手动下载」可查看 Root 方案。")
}

private enum class InfoDialogKind { SHIZUKU, DHIZUKU, ROOT }

@Composable
private fun ShizukuCard(
    state: GrantState,
    context: Context,
    onStateChange: (GrantState) -> Unit,
    onInfo: () -> Unit
) {
    val scope = rememberCoroutineScope()
    GrantCard(
        title = "Shizuku",
        subtitle = "ADB 级权限，无需 Root",
        state = state,
        onInfo = onInfo,
        primaryAction = when (state) {
            is GrantState.Detecting, GrantState.Granted, GrantState.Requesting -> null
            is GrantState.NeedsPermission -> PrimaryAction("请求授权") {
                runCatching {
                    if (Shizuku.pingBinder()) {
                        onStateChange(GrantState.Requesting)
                        val reqCode = (System.currentTimeMillis() and 0x7FFFFFFF).toInt()
                        Shizuku.requestPermission(reqCode)
                    } else {
                        onStateChange(GrantState.Unavailable("Shizuku 服务未运行"))
                    }
                }.onFailure { t ->
                    onStateChange(GrantState.Failed("请求失败：${t.message ?: t.javaClass.simpleName}"))
                }
            }
            is GrantState.Unavailable -> PrimaryAction("重新检测") {
                scope.launch {
                    onStateChange(GrantState.Detecting)
                    delay(300)
                    onStateChange(evalShizukuState(context))
                }
            }
            is GrantState.Failed -> PrimaryAction("重试") {
                scope.launch {
                    onStateChange(GrantState.Detecting)
                    delay(300)
                    onStateChange(evalShizukuState(context))
                }
            }
        },
        downloadUrl = "https://shizuku.rikka.app/download/"
    )
}

@Composable
private fun DhizukuCard(
    state: GrantState,
    context: Context,
    onStateChange: (GrantState) -> Unit,
    onInfo: () -> Unit
) {
    val scope = rememberCoroutineScope()
    GrantCard(
        title = "Dhizuku",
        subtitle = "Device-Owner 授权，无需 Root",
        state = state,
        onInfo = onInfo,
        primaryAction = when (state) {
            is GrantState.Detecting, GrantState.Granted, GrantState.Requesting -> null
            is GrantState.NeedsPermission -> PrimaryAction("请求授权") {
                scope.launch {
                    val activity = context as? android.app.Activity
                    if (activity == null) {
                        onStateChange(GrantState.Failed("无法获取 Activity，请从主界面进入"))
                        return@launch
                    }
                    try {
                        val cls = Class.forName("com.itsaky.androidide.dhizukudav.Dhizuku")
                        val listenerClass = Class.forName("com.itsaky.androidide.dhizukudav.Dhizuku\$OnRequestPermissionResultListener")
                        val listener = java.lang.reflect.Proxy.newProxyInstance(
                            listenerClass.classLoader,
                            arrayOf(listenerClass)
                        ) { _, method, args ->
                            if (method.name == "onRequestPermissionResult" && args?.isNotEmpty() == true) {
                                val granted = args[0] as? Boolean ?: false
                                onStateChange(if (granted) GrantState.Granted
                                else GrantState.Failed("Dhizuku 权限被拒绝"))
                            }
                            null
                        }
                        val reqCode = (System.currentTimeMillis() and 0x7FFFFFFF).toInt()
                        cls.getMethod("requestPermission", android.app.Activity::class.java,
                            Int::class.java, listenerClass)
                            .invoke(null, activity, reqCode, listener)
                        onStateChange(GrantState.Requesting)
                    } catch (t: Throwable) {
                        onStateChange(GrantState.Failed("请求失败：${t.message ?: t.javaClass.simpleName}"))
                    }
                }
            }
            is GrantState.Unavailable -> PrimaryAction("重新检测") {
                scope.launch {
                    onStateChange(GrantState.Detecting)
                    delay(300)
                    onStateChange(evalDhizukuState(context))
                }
            }
            is GrantState.Failed -> PrimaryAction("重试") {
                scope.launch {
                    onStateChange(GrantState.Detecting)
                    delay(300)
                    onStateChange(evalDhizukuState(context))
                }
            }
        },
        downloadUrl = "https://github.com/iamr0s/Dhizuku/releases"
    )
}

@Composable
private fun RootCard(
    state: GrantState,
    onStateChange: (GrantState) -> Unit,
    onInfo: () -> Unit
) {
    GrantCard(
        title = "Root",
        subtitle = "完整设备控制，需要 su 二进制",
        state = state,
        onInfo = onInfo,
        primaryAction = when (state) {
            is GrantState.Detecting, GrantState.Granted -> null
            is GrantState.Requesting -> null
            is GrantState.NeedsPermission -> PrimaryAction("探测 Root") {
                onStateChange(GrantState.Detecting)
                onStateChange(evalRootState())
            }
            is GrantState.Unavailable -> PrimaryAction("重新检测") {
                onStateChange(GrantState.Detecting)
                onStateChange(evalRootState())
            }
            is GrantState.Failed -> PrimaryAction("重试") {
                onStateChange(GrantState.Detecting)
                onStateChange(evalRootState())
            }
        },
        downloadUrl = "https://github.com/topjohnwu/Magisk"
    )
}

private data class PrimaryAction(val label: String, val action: () -> Unit)

@Composable
private fun GrantCard(
    title: String,
    subtitle: String,
    state: GrantState,
    onInfo: () -> Unit,
    primaryAction: PrimaryAction?,
    downloadUrl: String?
) {
    val context = LocalContext.current
    val (statusText, statusColor, icon) = describeState(state)
    val granted = state is GrantState.Granted

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(icon = icon, tint = statusColor)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onInfo) {
                    Icon(Icons.Filled.HelpOutline, contentDescription = "查看详情")
                }
            }
            Spacer(Modifier.height(8.dp))
            // 状态条
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        statusColor.copy(alpha = 0.10f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon, contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor
                )
            }
            if (!granted && primaryAction != null) {
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = primaryAction.action,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = statusColor
                    )
                ) { Text(primaryAction.label) }
            }
            if (downloadUrl != null) {
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("手动下载开源客户端", fontSize = 12.sp) }
            }
        }
    }
}

@Composable
private fun StatusDot(icon: ImageVector, tint: Color) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(tint.copy(alpha = 0.15f), CircleShape)
            .border(1.5.dp, tint, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun describeState(state: GrantState): Triple<String, Color, ImageVector> {
    val cs = currentThemeColors()
    return when (state) {
        is GrantState.Detecting -> Triple(
            "正在检测...", cs.outline, Icons.Filled.HourglassEmpty
        )
        is GrantState.Unavailable -> Triple(
            state.reason, cs.outline, Icons.Filled.Cancel
        )
        is GrantState.NeedsPermission -> Triple(
            "服务已就绪，请点击下方按钮请求授权", cs.primary, Icons.Filled.Lock
        )
        is GrantState.Requesting -> Triple(
            "等待授权确认...", cs.primary, Icons.Filled.HourglassEmpty
        )
        is GrantState.Granted -> Triple(
            "已授权", cs.primary, Icons.Filled.CheckCircle
        )
        is GrantState.Failed -> Triple(
            state.message, cs.error, Icons.Filled.Error
        )
    }
}

private data class ThemeColors(
    val primary: Color,
    val outline: Color,
    val error: Color
)

@Composable
private fun currentThemeColors(): ThemeColors = ThemeColors(
    primary = MaterialTheme.colorScheme.primary,
    outline = MaterialTheme.colorScheme.outline,
    error = MaterialTheme.colorScheme.error
)

@Composable
private fun InfoDialog(kind: InfoDialogKind, onDismiss: () -> Unit) {
    val (title, body, url) = when (kind) {
        InfoDialogKind.SHIZUKU -> Triple(
            "关于 Shizuku",
            """
            Shizuku 是一个开源项目，通过 ADB 启动一个系统级服务，让普通应用能够以 ADB 权限调用系统 API，无需 Root。

            授权步骤：
            1. 在设备上安装 Shizuku App（点击下方「手动下载」）
            2. 打开 Shizuku，根据你的设备类型选择启动方式：
               • 无 Root：通过 ADB 或无线调试启动
               • 已 Root / Magisk：直接点击「启动」
            3. Shizuku 启动后回到本页面，点击「重新检测」

            本应用在检测到 Shizuku 服务后，会调用官方 SDK 请求本应用使用 Shizuku 的权限，你只需在 Shizuku 的弹窗中点击「允许」。
            """.trimIndent(),
            "https://shizuku.rikka.app/download/"
        )
        InfoDialogKind.DHIZUKU -> Triple(
            "关于 Dhizuku",
            """
            Dhizuku 通过 Device Owner 机制把设备管理员权限共享给普通应用，无需 Root。

            授权步骤：
            1. 在设备上安装 Dhizuku 客户端（点击下方「手动下载」）
            2. 通过 ADB 命令将 Dhizuku 激活为 Device Owner：
               adb shell dpm set-device-owner com.itsaky.androidide.dhizukudav/.DhizukuDAReceiver
            3. 在 Dhizuku 客户端中点击「激活」
            4. 回到本页面，点击「重新检测」

            注意：激活 Device Owner 前请确保设备未绑定任何 Google 账号 / 工作资料。
            """.trimIndent(),
            "https://github.com/iamr0s/Dhizuku/releases"
        )
        InfoDialogKind.ROOT -> Triple(
            "关于 Root",
            """
            Root 授权要求设备已通过 Magisk / KernelSU / 其他方案获取 su 权限。

            探测步骤：
            1. 确认设备已 Root：在终端运行 su 命令，返回 # 即表示已 Root
            2. 打开 Magisk / KernelSU Manager，确保本应用被授予 Root 权限
            3. 回到本页面，点击「探测 Root」

            本应用通过执行 su -c true 验证 Root 可用性。如失败，请检查：
            • Magisk Manager → 超级用户列表中是否包含本应用
            • 设备的 SELinux 策略是否允许 su 执行
            """.trimIndent(),
            "https://github.com/topjohnwu/Magisk"
        )
    }
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.HelpOutline, contentDescription = null) },
        title = { Text(title) },
        text = { Text(body, fontSize = 13.sp) },
        confirmButton = {
            TextButton(onClick = {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
                onDismiss()
            }) { Text("手动下载") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

private fun androidx.compose.ui.graphics.Color.luminance(): Float {
    return 0.2126f * red + 0.7152f * green + 0.0722f * blue
}
