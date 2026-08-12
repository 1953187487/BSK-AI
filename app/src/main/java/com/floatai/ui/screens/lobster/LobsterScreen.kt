package com.floatai.ui.screens.lobster

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.floatai.lobster.bridge.LobsterAction
import com.floatai.lobster.bridge.LobsterBridge
import com.floatai.ui.components.GlassCard
import com.floatai.ui.components.SectionTitle
import com.floatai.ui.i18n.localStrings
import kotlinx.coroutines.launch

/**
 * 小龙虾面板 v1.0.4：
 *  - 显示小龙虾（无障碍服务）启用状态
 *  - 快速测试：dump / click-by-text / input / back / home
 *  - 一键跳系统设置启用无障碍服务
 *
 * 实际"AI 聊天里调用小龙虾"由 [LobsterBridge.perform] 暴露给 AI 模型。
 */
@Composable
fun LobsterScreen() {
    val strings = localStrings()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var ready by remember { mutableStateOf(LobsterBridge.isReady()) }
    var lastResult by remember { mutableStateOf("") }
    var testText by remember { mutableStateOf("") }

    // 定时检查服务状态（用户可能从系统设置启用后切回）
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            ready = LobsterBridge.isReady()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Pets,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.size(12.dp))
            Column {
                Text(
                    strings.lobster_panel_title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    if (ready) strings.lobster_status_on else strings.lobster_status_off,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (ready) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            strings.lobster_panel_desc,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        SectionTitle("启用状态")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (ready) "✓ " + strings.lobster_status_on
                        else "○ " + strings.lobster_status_off,
                        fontWeight = FontWeight.SemiBold,
                        color = if (ready) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "开启后，AI 聊天可通过 function calling 模拟点击、输入、启动应用。",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }) {
                        Icon(Icons.Filled.Settings, contentDescription = null)
                        Spacer(Modifier.size(6.dp))
                        Text("系统无障碍设置")
                    }
                    OutlinedButton(onClick = { ready = LobsterBridge.isReady() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = null)
                        Spacer(Modifier.size(4.dp))
                        Text("刷新状态")
                    }
                }
            }
        }

        SectionTitle("快速测试")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("测试输入：", fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                androidx.compose.material3.OutlinedTextField(
                    value = testText,
                    onValueChange = { testText = it },
                    label = { Text("点击目标文本 / 输入文本") },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val r = LobsterBridge.perform(
                                    LobsterAction.ClickByText(testText)
                                )
                                lastResult = "${r.ok}: ${r.message}"
                            }
                        },
                        enabled = ready
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(Modifier.size(4.dp))
                        Text("点击文本")
                    }
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val r = LobsterBridge.perform(
                                    LobsterAction.InputText(testText)
                                )
                                lastResult = "${r.ok}: ${r.message}"
                            }
                        },
                        enabled = ready
                    ) {
                        Text("输入文本")
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val r = LobsterBridge.perform(LobsterAction.GlobalBack)
                                lastResult = "${r.ok}: ${r.message}"
                            }
                        },
                        enabled = ready
                    ) { Text("← 返回") }
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val r = LobsterBridge.perform(LobsterAction.GlobalHome)
                                lastResult = "${r.ok}: ${r.message}"
                            }
                        },
                        enabled = ready
                    ) { Text("⌂ Home") }
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val r = LobsterBridge.perform(LobsterAction.Dump())
                                lastResult = r.message
                            }
                        },
                        enabled = ready
                    ) { Text("导出界面") }
                }
                if (lastResult.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "结果：$lastResult",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        SectionTitle("AI 调用说明")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(
                    "在 AI 聊天中可直接说：\"打开微信\"、\"给张三发消息\"、\"点击确定按钮\"。\n" +
                        "AI 会自动调用 LobsterBridge 完成对应操作。",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "支持动作：dump / clickByText / inputText / back / home / launchApp",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}
