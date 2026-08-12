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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.floatai.BuildConfig
import com.floatai.data.model.AppLanguage
import com.floatai.ui.i18n.localStrings
import com.floatai.ui.theme.liquidBackdrop
import com.floatai.ui.theme.relativeLuminance

/**
 * 协议流 v2：
 *  - 顶部 2 段进度条
 *  - 第 1 步：用户须知 + 勾选「我已阅读并理解」
 *  - 第 2 步：权限协议 + 勾选「我同意」
 *  - 每发布新版本 (BuildConfig.PROTOCOL_VERSION 递增) 都会强制重新签署
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
    var step1Checked by remember { mutableStateOf(false) }
    var step2Checked by remember { mutableStateOf(false) }

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
                repeat(2) { i ->
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

            // 标题 + 副标题
            Text(
                text = if (step == 1) strings.user_notice_title
                else strings.permission_notice_title,
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
                Text(
                    text = if (step == 1) strings.user_notice_body
                    else strings.permission_notice_body,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // 勾选项
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = if (step == 1) step1Checked else step2Checked,
                    onCheckedChange = {
                        if (step == 1) step1Checked = it else step2Checked = it
                    }
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = if (step == 1) "我已阅读并理解上述内容"
                    else "我同意上述协议，并知晓相关风险",
                    fontSize = 14.sp
                )
            }

            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (step == 2) {
                    OutlinedButton(
                        onClick = { step = 1 },
                        modifier = Modifier.weight(1f)
                    ) { Text("← ${strings.user_notice_title}") }
                }
                Button(
                    onClick = {
                        if (step == 1) {
                            step = 2
                        } else {
                            onAgree()
                        }
                    },
                    enabled = if (step == 1) step1Checked else step2Checked,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null)
                    Spacer(Modifier.size(6.dp))
                    Text(if (step == 1) "下一步" else "同意并继续")
                }
            }

            // 提示
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
