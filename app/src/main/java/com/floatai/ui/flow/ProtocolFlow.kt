package com.floatai.ui.flow

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.SettingsApplications
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.floatai.BuildConfig
import com.floatai.data.model.AppLanguage
import com.floatai.ui.i18n.localStrings
import com.floatai.ui.theme.liquidBackdrop
import com.floatai.ui.theme.relativeLuminance

private data class PermissionEntry(
    val key: String,
    val display: String,
    val rationale: String,
    val icon: ImageVector,
    val required: Boolean
)

/**
 * 协议流 v1.0.3：三步
 *  - 第 1 步：用户须知（隐私、用途）
 *  - 第 2 步：开源协议（Apache 2.0 + 第三方依赖清单）
 *  - 第 3 步：运行时权限清单（悬浮窗 / 通知 / 麦克风 / 相机 / 使用情况 / VPN）
 *
 *  每发布新版本 PROTOCOL_VERSION 递增都会强制重签。
 */
@Composable
fun ProtocolFlow(
    language: AppLanguage,
    onAgree: () -> Unit,
    onLanguage: (AppLanguage) -> Unit
) {
    val strings = localStrings()
    val dark = MaterialTheme.colorScheme.background.relativeLuminance() < 0.5f
    var step by remember { mutableIntStateOf(1) }
    var showLanguage by remember { mutableStateOf(false) }

    // 三组 Checkbox：用户须知 / 开源协议 / 权限
    var ackNotice by remember { mutableStateOf(false) }
    var ackOss by remember { mutableStateOf(false) }
    var ackPerms by remember { mutableStateOf(false) }

    // 权限列表
    val permissions = remember {
        listOf(
            PermissionEntry(
                key = "overlay",
                display = "悬浮窗 (SYSTEM_ALERT_WINDOW)",
                rationale = "用于在屏幕边缘显示快捷按钮与 AI 聊天面板。可在设置中随时撤销。",
                icon = Icons.Filled.SettingsApplications,
                required = false
            ),
            PermissionEntry(
                key = "notifications",
                display = "通知 (POST_NOTIFICATIONS)",
                rationale = "用于显示后台服务运行通知。Android 13+ 必须用户主动授权。",
                icon = Icons.Filled.Notifications,
                required = false
            ),
            PermissionEntry(
                key = "microphone",
                display = "麦克风 (RECORD_AUDIO)",
                rationale = "用于语音输入。可在设置中随时撤销。",
                icon = Icons.Filled.Mic,
                required = false
            ),
            PermissionEntry(
                key = "camera",
                display = "相机 (CAMERA)",
                rationale = "用于拍摄附件与 OCR。可在设置中随时撤销。",
                icon = Icons.Filled.Videocam,
                required = false
            ),
            PermissionEntry(
                key = "usage",
                display = "使用情况访问 (PACKAGE_USAGE_STATS)",
                rationale = "用于读取最近使用的应用列表与悬浮窗面板。需要在系统设置中手动开启。",
                icon = Icons.Filled.PrivacyTip,
                required = false
            ),
            PermissionEntry(
                key = "vpn",
                display = "VPN 服务 (VpnService)",
                rationale = "用于抓包功能。流量仅在本机处理，不上传。可在设置中随时撤销。",
                icon = Icons.Filled.VpnLock,
                required = false
            )
        )
    }
    val grantedPerms = remember { mutableStateOf(setOf<String>()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(liquidBackdrop(dark))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 顶部：版本号 + 语言切换
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        "FloatAI v${BuildConfig.VERSION_NAME} · 协议 v${BuildConfig.PROTOCOL_VERSION}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                AssistChip(
                    onClick = { showLanguage = true },
                    label = {
                        Text(
                            if (language == AppLanguage.ZH) strings.language_zh
                            else strings.language_en
                        )
                    },
                    leadingIcon = { Icon(Icons.Filled.Language, contentDescription = null) }
                )
            }

            // 进度指示
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(3) { i ->
                    val active = (i + 1) <= step
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .background(
                                if (active) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                CircleShape
                            )
                    )
                }
            }

            // 步骤标题
            Text(
                text = when (step) {
                    1 -> "用户须知"
                    2 -> "开源协议"
                    else -> "运行时权限声明"
                },
                style = MaterialTheme.typography.headlineMedium
            )

            // 内容卡片
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(20.dp)
            ) {
                when (step) {
                    1 -> NoticeStepBody()
                    2 -> OssStepBody()
                    else -> PermissionStepBody(permissions, grantedPerms)
                }
            }

            // 勾选项
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = when (step) {
                        1 -> ackNotice
                        2 -> ackOss
                        else -> ackPerms
                    },
                    onCheckedChange = {
                        when (step) {
                            1 -> ackNotice = it
                            2 -> ackOss = it
                            else -> ackPerms = it
                        }
                    }
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = when (step) {
                        1 -> "我已阅读并理解上述内容"
                        2 -> "我知悉本应用使用 Apache 2.0 与第三方开源组件"
                        else -> "我同意上述权限声明，并知晓每项可在设置中撤销"
                    },
                    fontSize = 14.sp
                )
            }

            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (step > 1) {
                    OutlinedButton(
                        onClick = { step-- },
                        modifier = Modifier.weight(1f)
                    ) { Text("← 上一步") }
                }
                Button(
                    onClick = {
                        when (step) {
                            1 -> step = 2
                            2 -> step = 3
                            else -> onAgree()
                        }
                    },
                    enabled = when (step) {
                        1 -> ackNotice
                        2 -> ackOss
                        else -> ackPerms
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null)
                    Spacer(Modifier.size(6.dp))
                    Text(if (step < 3) "下一步" else "同意并继续")
                }
            }

            Text(
                text = "提示：每次发布新版本时，协议内容可能更新，您需要重新签署。",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showLanguage) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showLanguage = false },
            title = { Text(strings.language_choose_title) },
            text = {
                Column {
                    androidx.compose.material3.TextButton(onClick = {
                        onLanguage(AppLanguage.ZH); showLanguage = false
                    }) { Text(strings.language_zh) }
                    androidx.compose.material3.TextButton(onClick = {
                        onLanguage(AppLanguage.EN); showLanguage = false
                    }) { Text(strings.language_en) }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showLanguage = false }) {
                    Text("关闭")
                }
            }
        )
    }
}

@Composable
private fun NoticeStepBody() {
    Text(
        text = """
FloatAI 是一个本地优先的 AI 助手应用，主要功能包括：

• AI 对话：使用您配置的 OpenAI 兼容 API（API Key 仅存储在本地 SharedPreferences，不上传）
• 悬浮窗：在屏幕边缘提供快速访问入口，需悬浮窗权限
• 项目创建：本地生成 Android Gradle 项目脚手架
• 抓包（可选）：使用标准 VpnService 在本机抓取进出流量，数据仅落盘本地

数据安全：
• 所有对话历史与配置仅保存到应用私有目录 (filesDir)
• 不会向任何第三方服务器上传您的 API Key 或对话内容
• 您可以随时在「设置」中清空所有数据

您可以随时通过「设置」-「权限」撤销已授权的运行时权限。
        """.trimIndent(),
        fontSize = 13.sp,
        lineHeight = 20.sp,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun OssStepBody() {
    Column {
        Text(
            text = "本应用基于 Apache License 2.0 开源。",
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(10.dp))
        Text("使用的第三方组件：", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Spacer(Modifier.height(6.dp))
        val deps = listOf(
            "AndroidX Core / Compose / Material 3 — Apache 2.0",
            "Kotlin / JetBrains — Apache 2.0",
            "NanoHTTPD — BSD-3-Clause",
            "OkHttp — Apache 2.0 (transitively)",
            "GitHub API 客户端 — MIT"
        )
        deps.forEach {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.size(6.dp))
                Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "完整源码：github.com/1953187487/FloatAI",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PermissionStepBody(
    permissions: List<PermissionEntry>,
    granted: androidx.compose.runtime.MutableState<Set<String>>
) {
    Column {
        Text(
            text = "以下权限仅在您主动使用对应功能时才会请求。" +
                "您可以随时在「设置」中撤销。" +
                "撤销后，对应功能将不可用，但不会影响其他功能。",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        Spacer(Modifier.height(6.dp))
        permissions.forEach { perm ->
            val checked = perm.key in granted.value
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.Top
            ) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = { v ->
                        granted.value = if (v) granted.value + perm.key
                        else granted.value - perm.key
                    }
                )
                Spacer(Modifier.size(6.dp))
                Icon(
                    perm.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.size(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        perm.display,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        perm.rationale,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = granted.value.size == permissions.size,
                onCheckedChange = { v ->
                    granted.value = if (v) permissions.map { it.key }.toSet() else emptySet()
                }
            )
            Spacer(Modifier.size(4.dp))
            Text("全部同意", fontSize = 13.sp)
        }
    }
}
