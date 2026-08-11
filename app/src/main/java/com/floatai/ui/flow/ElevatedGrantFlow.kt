package com.floatai.ui.flow

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.floatai.data.model.AppLanguage
import com.floatai.perm.ElevatedGrant
import com.floatai.perm.ElevatedGrantDetector
import com.floatai.ui.components.GlassCard
import com.floatai.ui.i18n.localStrings
import com.floatai.ui.theme.liquidBackdrop
import rikka.shizuku.Shizuku
import kotlinx.coroutines.launch

/**
 * 高权限协议流：必须授予 Shizuku / Dhizuku / Root 至少一个才能进入主界面。
 *
 * 修复（v1.0.1 → v1.0.1 re-publish）：
 *  点击授权按钮改为「应用内执行授权流程」，不再自动跳转到开源项目下载页。
 *  Shizuku / Dhizuku 在设备上已安装对应 App 时，调用官方 API 发起权限请求；
 *  Root 在应用内执行探测并返回结果。
 *  「手动下载开源客户端」入口保留为次要按钮，仅在用户主动需要时使用。
 */
@Composable
fun ElevatedGrantFlow(
    language: AppLanguage,
    onGranted: () -> Unit
) {
    val strings = localStrings()
    val context = LocalContext.current
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    var detected by remember { mutableStateOf(ElevatedGrantDetector.detect()) }
    var shizukuInfo by remember { mutableStateOf(false) }
    var dhizukuInfo by remember { mutableStateOf(false) }
    var rootInfo by remember { mutableStateOf(false) }
    var statusMsg by remember { mutableStateOf<String?>(null) }
    var statusError by remember { mutableStateOf(false) }

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
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        detected = ElevatedGrantDetector.detect()
                        statusMsg = null
                    },
                    label = { Text(strings.elevated_recheck) },
                    leadingIcon = { Icon(Icons.Filled.CheckCircle, contentDescription = null) }
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = strings.elevated_status(ElevatedGrantDetector.describe(detected)),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (detected != ElevatedGrant.NONE) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            statusMsg?.let { msg ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (statusError) Icons.Filled.Error else Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = if (statusError) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (statusError) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            ShizukuOptionCard(
                granted = detected == ElevatedGrant.SHIZUKU,
                onGranted = { detected = ElevatedGrant.SHIZUKU; statusError = false; statusMsg = "Shizuku 已授权" },
                onInfo = { shizukuInfo = true },
                onRequestResult = { ok, msg ->
                    if (ok) {
                        detected = ElevatedGrant.SHIZUKU
                        statusError = false
                        statusMsg = "Shizuku 已授权"
                    } else {
                        statusError = true
                        statusMsg = msg
                    }
                }
            )

            DhizukuOptionCard(
                granted = detected == ElevatedGrant.DHIZUKU,
                onGranted = { detected = ElevatedGrant.DHIZUKU; statusError = false; statusMsg = "Dhizuku 已授权" },
                onInfo = { dhizukuInfo = true },
                onRequestResult = { ok, msg ->
                    if (ok) {
                        detected = ElevatedGrant.DHIZUKU
                        statusError = false
                        statusMsg = "Dhizuku 已授权"
                    } else {
                        statusError = true
                        statusMsg = msg
                    }
                }
            )

            RootOptionCard(
                granted = detected == ElevatedGrant.ROOT,
                onGranted = { detected = ElevatedGrant.ROOT; statusError = false; statusMsg = "Root 已检测" },
                onInfo = { rootInfo = true },
                onResult = { ok, msg ->
                    if (ok) {
                        detected = ElevatedGrant.ROOT
                        statusError = false
                        statusMsg = "Root 已检测"
                    } else {
                        statusError = true
                        statusMsg = msg
                    }
                }
            )

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    detected = ElevatedGrantDetector.detect()
                    if (detected != ElevatedGrant.NONE) onGranted()
                },
                enabled = detected != ElevatedGrant.NONE,
                modifier = Modifier.fillMaxWidth()
            ) { Text(strings.elevated_continue) }

            if (detected == ElevatedGrant.NONE) {
                Text(
                    text = strings.elevated_required,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    if (shizukuInfo) {
        AuthorInfoDialog(
            title = strings.shizuku_intro_title,
            body = strings.shizuku_intro_body,
            downloadUrl = "https://shizuku.rikka.app/download/",
            onDismiss = { shizukuInfo = false }
        )
    }
    if (dhizukuInfo) {
        AuthorInfoDialog(
            title = strings.dhizuku_intro_title,
            body = strings.dhizuku_intro_body,
            downloadUrl = "https://github.com/iamr0s/Dhizuku/releases",
            onDismiss = { dhizukuInfo = false }
        )
    }
    if (rootInfo) {
        AuthorInfoDialog(
            title = "关于 Root",
            body = "Root 授权依赖设备已刷入 su（Magisk / KernelSU / 其他方案）。\n\n点击「授权」会在应用内执行探测命令验证 su 可用性；如失败，请确认已正确安装 Root 方案并授予本应用 Root 权限。\n\n如需 Root 方案，请点击下方「手动下载开源客户端」。",
            downloadUrl = "https://github.com/topjohnwu/Magisk",
            onDismiss = { rootInfo = false }
        )
    }
}

@Composable
private fun ShizukuOptionCard(
    granted: Boolean,
    onGranted: () -> Unit,
    onInfo: () -> Unit,
    onRequestResult: (Boolean, String) -> Unit
) {
    val context = LocalContext.current
    val requestCode = remember { mutableIntStateOf(1001) }
    val listener = remember<Shizuku.OnRequestPermissionResultListener> {
        Shizuku.OnRequestPermissionResultListener { _, grantResult ->
            val ok = grantResult == PackageManager.PERMISSION_GRANTED
            onRequestResult(ok, if (ok) "" else "Shizuku 权限被拒绝")
        }
    }

    LaunchedEffect(Unit) {
        Shizuku.addRequestPermissionResultListener(listener)
    }
    DisposableEffect(Unit) {
        onDispose { Shizuku.removeRequestPermissionResultListener(listener) }
    }

    ElevatedOptionCard(
        title = "Shizuku",
        desc = "ADB-level permissions, no root required.",
        granted = granted,
        onAuthorize = {
            try {
                if (!Shizuku.pingBinder()) {
                    // 应用未安装 / 服务未启动
                    onRequestResult(false, "未检测到 Shizuku 服务，请先安装并启动 Shizuku App")
                    return@ElevatedOptionCard
                }
                if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                    onGranted()
                } else {
                    Shizuku.requestPermission(requestCode.intValue)
                }
            } catch (t: Throwable) {
                onRequestResult(false, "Shizuku 授权失败：${t.message ?: t.javaClass.simpleName}")
            }
        },
        onInfo = onInfo,
        onDownload = {
            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/download/")))
            }
        }
    )
}

@Composable
private fun DhizukuOptionCard(
    granted: Boolean,
    onGranted: () -> Unit,
    onInfo: () -> Unit,
    onRequestResult: (Boolean, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    ElevatedOptionCard(
        title = "Dhizuku",
        desc = "Device-owner based, no root alternative.",
        granted = granted,
        onAuthorize = {
            try {
                val cls = Class.forName("com.itsaky.androidide.dhizukudav.Dhizuku")
                val isAvailable = cls.getMethod("isAvailable").invoke(null) as? Boolean ?: false
                if (!isAvailable) {
                    onRequestResult(false, "未检测到 Dhizuku 服务，请先安装并激活 Dhizuku")
                    return@ElevatedOptionCard
                }
                val isGranted = cls.getMethod("isPermissionGranted").invoke(null) as? Boolean ?: false
                if (isGranted) {
                    onGranted()
                } else {
                    // 反射调用 requestPermission(Activity, int)；用户需在 Dhizuku 弹窗中确认授权。
                    val currentActivity = context as? android.app.Activity
                    if (currentActivity == null) {
                        onRequestResult(false, "无法获取 Activity 实例发起授权")
                        return@ElevatedOptionCard
                    }
                    val reqCode = (System.currentTimeMillis() and 0x7FFFFFFF).toInt()
                    val listenerClass = Class.forName("com.itsaky.androidide.dhizukudav.Dhizuku\$OnRequestPermissionResultListener")
                    val listener = java.lang.reflect.Proxy.newProxyInstance(
                        listenerClass.classLoader,
                        arrayOf(listenerClass)
                    ) { _, _, _ -> true }
                    val reqPerm = cls.getMethod(
                        "requestPermission",
                        android.app.Activity::class.java,
                        Int::class.java,
                        listenerClass
                    )
                    reqPerm.invoke(null, currentActivity, reqCode, listener)
                    // Dhizuku 的回调是同步 UI 弹窗，返回后再次检测
                    scope.launch {
                        kotlinx.coroutines.delay(800)
                        val grantedNow = cls.getMethod("isPermissionGranted").invoke(null) as? Boolean ?: false
                        onRequestResult(grantedNow, if (grantedNow) "" else "Dhizuku 授权被拒绝或超时")
                    }
                }
            } catch (t: Throwable) {
                onRequestResult(false, "Dhizuku 授权失败：${t.message ?: t.javaClass.simpleName}")
            }
        },
        onInfo = onInfo,
        onDownload = {
            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/iamr0s/Dhizuku/releases")))
            }
        }
    )
}

@Composable
private fun RootOptionCard(
    granted: Boolean,
    onGranted: () -> Unit,
    onInfo: () -> Unit,
    onResult: (Boolean, String) -> Unit
) {
    ElevatedOptionCard(
        title = "Root",
        desc = "Full device control, requires su binary.",
        granted = granted,
        onAuthorize = {
            val ok = runCatching {
                val process = ProcessBuilder("su", "-c", "true").start()
                process.waitFor() == 0
            }.getOrDefault(false)
            if (ok) onGranted() else onResult(false, "未检测到 Root，请确认已安装并授权 su")
        },
        onInfo = onInfo,
        onDownload = null
    )
}

@Composable
private fun ElevatedOptionCard(
    title: String,
    desc: String,
    granted: Boolean,
    onAuthorize: () -> Unit,
    onInfo: () -> Unit,
    onDownload: (() -> Unit)?
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (granted) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onInfo) { Text("?") }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAuthorize,
                    modifier = Modifier.weight(1f),
                    enabled = !granted
                ) {
                    Text(if (granted) "已授权" else "授权")
                }
                if (onDownload != null) {
                    TextButton(
                        onClick = onDownload,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("手动下载开源客户端", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthorInfoDialog(
    title: String,
    body: String,
    downloadUrl: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body, fontSize = 13.sp) },
        confirmButton = {
            TextButton(onClick = {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)))
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
