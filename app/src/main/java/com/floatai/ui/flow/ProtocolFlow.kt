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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.SettingsApplications
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

/**
 * 协议流 v1.0.4：5 步
 *  - 第 1 步：用户须知（隐私、用途）
 *  - 第 2 步：开源协议（Apache 2.0 + 第三方依赖清单）
 *  - 第 3 步：应用须知（应用内能力范围、数据流）
 *  - 第 4 步：小龙虾须知（AI 模拟操作的边界）
 *  - 第 5 步：运行时权限清单 + 同意
 *
 * 每发布新版本 PROTOCOL_VERSION 递增都会强制重签。
 */
@Composable
fun ProtocolFlow(
    language: AppLanguage,
    onAgree: () -> Unit,
    onLanguage: (AppLanguage) -> Unit
) {
    val strings = localStrings()
    val dark = MaterialTheme.colorScheme.background.relativeLuminance() < 0.5f
    val totalSteps = 5
    var step by remember { mutableIntStateOf(1) }
    var showLanguage by remember { mutableStateOf(false) }

    // 各步勾选（默认全部勾选，避免按钮 disabled）
    var ackNotice by remember { mutableStateOf(true) }
    var ackOss by remember { mutableStateOf(true) }
    var ackApp by remember { mutableStateOf(true) }
    var ackLobster by remember { mutableStateOf(true) }
    var ackPerms by remember { mutableStateOf(true) }

    val ackCurrent: Boolean = when (step) {
        1 -> ackNotice
        2 -> ackOss
        3 -> ackApp
        4 -> ackLobster
        else -> ackPerms
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(liquidBackdrop(dark))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 14.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 顶部：版本 + 语言
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

            // 进度条（5 段）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(totalSteps) { i ->
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

            // 标题 + 步骤计数
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = when (step) {
                        1 -> strings.user_notice_title
                        2 -> strings.oss_title
                        3 -> "应用须知"
                        4 -> "小龙虾须知"
                        else -> strings.permission_notice_title
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "$step / $totalSteps",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 内容卡片
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(18.dp)
            ) {
                when (step) {
                    1 -> NoticeStepBody()
                    2 -> OssStepBody()
                    3 -> AppNoticeStepBody()
                    4 -> LobsterNoticeStepBody()
                    else -> PermissionStepBody()
                }
            }

            // 勾选
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = ackCurrent,
                    onCheckedChange = { v ->
                        when (step) {
                            1 -> ackNotice = v
                            2 -> ackOss = v
                            3 -> ackApp = v
                            4 -> ackLobster = v
                            else -> ackPerms = v
                        }
                    }
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = when (step) {
                        1 -> "我已阅读并理解上述内容"
                        2 -> "我知悉本应用使用 Apache 2.0 与第三方开源组件"
                        3 -> "我了解本应用的能力范围与数据流向"
                        4 -> "我知悉小龙虾功能的边界与风险"
                        else -> "我同意上述权限声明，并知晓每项可在设置中撤销"
                    },
                    fontSize = 13.sp
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
                        if (step < totalSteps) step++
                        else onAgree()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (step < totalSteps) {
                        Text("下一步")
                        Spacer(Modifier.size(4.dp))
                        Icon(Icons.Filled.ArrowForward, contentDescription = null)
                    } else {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null)
                        Spacer(Modifier.size(6.dp))
                        Text("同意并继续")
                    }
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
• 小龙虾（可选）：AI 通过无障碍服务模拟点击 / 输入 / 启动应用

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
private fun AppNoticeStepBody() {
    Column {
        Text(
            "能力范围：",
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        listOf(
            Icons.Filled.CheckCircle to "AI 对话：调用您配置的 OpenAI 兼容 API",
            Icons.Filled.CheckCircle to "悬浮窗：在其他应用上层显示快捷面板",
            Icons.Filled.SettingsApplications to "本地构建：在应用沙箱内下载 JDK/SDK/Gradle 生成 APK",
            Icons.Filled.PrivacyTip to "抓包：通过标准 VpnService 接口抓取本机流量",
        ).forEach { (icon, text) ->
            Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 4.dp)) {
                Icon(
                    icon, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text(text, fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "数据流向：",
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            "• 对话内容：本地 → AI 提供商 API（按您的 baseUrl 配置）\n" +
                "• 抓包流量：网卡 → 本机解析 → 仅落盘 filesDir/captures/\n" +
                "• 配置 / 偏好：仅本机 SharedPreferences\n" +
                "• 项目脚手架：filesDir/projects/",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun LobsterNoticeStepBody() {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Pets,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.size(6.dp))
            Text(
                "小龙虾 (AI 模拟操作)",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = """
小龙虾让 AI 通过 Android 无障碍服务模拟您在手机上的操作，例如：

• 点击屏幕上的特定文本/按钮
• 在输入框中输入文本
• 启动指定应用
• 读取当前界面节点树

边界与风险：
• 必须您主动在「设置 → 无障碍」中授予权限，应用无法绕过系统授权
• AI 仅执行您聊天中明确请求的动作，不会自动后台运行
• 所有操作日志记录在本地，可随时在「小龙虾面板」中查看
• 涉及支付、隐私、金融类操作时 AI 会主动请求二次确认

关闭路径：设置 → 无障碍 → 找到「小龙虾」→ 关闭
            """.trimIndent(),
            fontSize = 12.sp,
            lineHeight = 18.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private data class PermissionEntry(
    val key: String,
    val display: String,
    val rationale: String,
    val icon: ImageVector,
    val required: Boolean
)

@Composable
private fun PermissionStepBody() {
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
                icon = Icons.Filled.Warning,
                required = false
            ),
            PermissionEntry(
                key = "microphone",
                display = "麦克风 (RECORD_AUDIO)",
                rationale = "用于语音输入。可在设置中随时撤销。",
                icon = Icons.Filled.Lock,
                required = false
            ),
            PermissionEntry(
                key = "camera",
                display = "相机 (CAMERA)",
                rationale = "用于拍摄附件与 OCR。可在设置中随时撤销。",
                icon = Icons.Filled.Lock,
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
                icon = Icons.Filled.Lock,
                required = false
            ),
            PermissionEntry(
                key = "lobster",
                display = "无障碍服务 (小龙虾)",
                rationale = "允许 AI 模拟点击 / 输入 / 启动应用。默认关闭，需在系统设置中手动开启。",
                icon = Icons.Filled.Pets,
                required = false
            )
        )
    }
    val grantedPerms = remember { mutableStateOf(setOf<String>()) }

    Column {
        Text(
            text = "以下权限仅在您主动使用对应功能时才会请求。" +
                "您可以随时在「设置 → 权限」中跳转到对应授权界面。" +
                "撤销后，对应功能将不可用，但不会影响其他功能。",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        Spacer(Modifier.height(6.dp))
        permissions.forEach { perm ->
            val checked = perm.key in grantedPerms.value
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.Top
            ) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = { v ->
                        grantedPerms.value = if (v) grantedPerms.value + perm.key
                        else grantedPerms.value - perm.key
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
    }
}
