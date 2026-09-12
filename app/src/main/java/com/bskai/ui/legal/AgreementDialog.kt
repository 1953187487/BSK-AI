package com.bskai.ui.legal

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bskai.AuraApp
import com.bskai.data.AgreementSection
import com.bskai.data.Agreements
import com.bskai.data.DefaultApiUrlPresets
import com.bskai.permission.ShizukuBridge
import com.bskai.ui.glass.GlassButton
import com.bskai.ui.glass.GlassPanel
import com.bskai.ui.glass.rememberGlassColors

/**
 * 2.1.1 首次启动引导：API 配置（真实持久化）→ Shizuku/Dhizuku 授权（可选）→ 协议确认。
 * 无强制步骤校验：除协议勾选外，所有步骤均可跳过。
 */
@Composable
fun OnboardingDialog(
    app: AuraApp,
    onComplete: () -> Unit
) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var apiUrl by rememberSaveable { mutableStateOf(app.settings.settings.value.apiProviderUrl) }
    var apiKey by rememberSaveable { mutableStateOf(app.settings.settings.value.apiProviderKey) }
    var agreedOpenSource by rememberSaveable { mutableStateOf(false) }
    var agreedUserNotice by rememberSaveable { mutableStateOf(false) }

    val glass = rememberGlassColors()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AURA",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(3) { i ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 2.dp)
                                .size(8.dp)
                                .background(
                                    if (i <= step) glass.accent else MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(4.dp)
                                )
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Box(modifier = Modifier.weight(1f)) {
                when (step) {
                    0 -> ApiConfigStepContent(
                        apiUrl = apiUrl, apiKey = apiKey,
                        onUrlChange = { apiUrl = it }, onKeyChange = { apiKey = it }
                    )
                    1 -> ShizukuStepContent(shizuku = app.shizuku)
                    else -> AgreementStepContent(
                        agreedOpenSource = agreedOpenSource, agreedUserNotice = agreedUserNotice,
                        onToggleOpenSource = { agreedOpenSource = it },
                        onToggleUserNotice = { agreedUserNotice = it }
                    )
                }
            }

            androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { if (step > 0) step -= 1 }, enabled = step > 0) {
                    Text(if (step == 0) "退出" else "上一步")
                }
                GlassButton(
                    text = when (step) {
                        0 -> "下一步"
                        1 -> "下一步"
                        else -> "同意并开始使用"
                    },
                    enabled = step < 2 || (agreedOpenSource && agreedUserNotice),
                    onClick = {
                        when (step) {
                            0 -> {
                                app.settings.update {
                                    it.copy(
                                        apiProviderUrl = apiUrl.trim(),
                                        apiProviderKey = apiKey.trim()
                                    )
                                }
                                step = 1
                            }
                            1 -> step = 2
                            else -> onComplete()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ApiConfigStepContent(
    apiUrl: String, apiKey: String,
    onUrlChange: (String) -> Unit, onKeyChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text("配置 AI 服务", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(
            "填写 AI 服务地址和密钥，本设置将被保存，稍后可在设置中更改。可直接跳过。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = apiUrl, onValueChange = onUrlChange,
            label = { Text("API 地址") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("https://api.example.com/v1") }
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = apiKey, onValueChange = onKeyChange,
            label = { Text("API Key") }, singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        Text("快速选择:", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            DefaultApiUrlPresets.forEach { preset ->
                Surface(
                    modifier = Modifier.androidClickable { onUrlChange(preset) },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Text(
                        preset.removePrefix("https://").removeSuffix("/v1"),
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun ShizukuStepContent(shizuku: ShizukuBridge) {
    val state by shizuku.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text("授权 Shizuku（Dhizuku）", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(
            "授权后，终端与 IDE 将以 root 级权限运行；未授权时这些功能保持不可用。此步骤可跳过。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(14.dp))

        when (state) {
            ShizukuBridge.State.GRANTED -> {
                GlassPanel(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CheckCircle, contentDescription = null,
                            tint = Color(0xFF3DBE7B), modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("已授权", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(
                                "终端与 IDE 已解锁 root 级执行能力",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            ShizukuBridge.State.NEED_PERMISSION -> {
                GlassPanel(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Security, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("待授权", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(
                                "检测到 Shizuku / Dhizuku 服务，点击下方按钮完成授权",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    GlassButton(
                        text = "立即授权",
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp).padding(bottom = 14.dp),
                        onClick = { shizuku.requestPermission() }
                    )
                }
            }

            else -> {
                GlassPanel(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Info, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("未检测到服务", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(
                                "请先安装并启动 Dhizuku 或 Shizuku 应用（可通过无线调试或 Root 启动），完成后点击重试。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    GlassButton(
                        text = "重试检测",
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp).padding(bottom = 14.dp),
                        onClick = { shizuku.refresh() }
                    )
                }
            }
        }
    }
}

@Composable
private fun AgreementStepContent(
    agreedOpenSource: Boolean, agreedUserNotice: Boolean,
    onToggleOpenSource: (Boolean) -> Unit, onToggleUserNotice: (Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text("开源协议与用户须知", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(
            "请阅读并同意以下两份协议后继续使用 AURA。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(14.dp))

        AgreementCard(section = Agreements.openSource, checked = agreedOpenSource, onCheckedChange = onToggleOpenSource)
        Spacer(Modifier.height(12.dp))
        AgreementCard(
            section = Agreements.userNotice.copy(body = Agreements.renderUserNotice()),
            checked = agreedUserNotice, onCheckedChange = onToggleUserNotice
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun AgreementCard(section: AgreementSection, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val glass = rememberGlassColors()
    GlassPanel(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(section.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = glass.content)
            Spacer(Modifier.height(8.dp))
            Text(
                section.body,
                style = MaterialTheme.typography.bodySmall,
                color = glass.contentMuted,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth().androidClickable { onCheckedChange(!checked) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    colors = CheckboxDefaults.colors(checkedColor = glass.accent)
                )
                Text(
                    "我已阅读并同意",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (checked) glass.accent else glass.content
                )
            }
        }
    }
}

private fun Modifier.androidClickable(onClick: () -> Unit): Modifier =
    this.clickable(
        interactionSource = MutableInteractionSource(),
        indication = null,
        onClick = onClick
    )
