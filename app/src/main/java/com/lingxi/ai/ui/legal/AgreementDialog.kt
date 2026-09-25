package com.lingxi.ai.ui.legal

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.style.TextOverflow
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lingxi.ai.LingXiApp
import com.lingxi.ai.R
import com.lingxi.ai.data.AgreementSection
import com.lingxi.ai.data.Agreements
import com.lingxi.ai.data.DefaultApiUrlPresets
import com.lingxi.ai.permission.DhizukuBridge
import com.lingxi.ai.permission.ShizukuBridge
import com.lingxi.ai.ui.glass.GlassButton
import com.lingxi.ai.ui.glass.GlassPanel
import com.lingxi.ai.ui.glass.rememberGlassColors

/**
 * 2.1.1 首次启动引导：API 配置（真实持久化）→ Shizuku/Dhizuku 授权（可选）→ 协议确认。
 * 无强制步骤校验：除协议勾选外，所有步骤均可跳过。
 */
@Composable
fun OnboardingDialog(
    app: LingXiApp,
    onComplete: () -> Unit
) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var apiUrl by rememberSaveable { mutableStateOf(app.settings.settings.value.apiProviderUrl) }
    var apiKey by rememberSaveable { mutableStateOf(app.settings.settings.value.apiProviderKey) }
    var agreedOpenSource by rememberSaveable { mutableStateOf(false) }
    var agreedUserNotice by rememberSaveable { mutableStateOf(false) }

    val glass = rememberGlassColors()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.app_name),
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

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clipToBounds()
            ) {
                when (step) {
                    0 -> ApiConfigStepContent(
                        apiUrl = apiUrl, apiKey = apiKey,
                        onUrlChange = { apiUrl = it }, onKeyChange = { apiKey = it }
                    )
                    1 -> PrivilegeStepContent(shizuku = app.shizuku, dhizuku = app.dhizuku)
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
                    Text(if (step == 0) stringResource(R.string.onboarding_exit)
                        else stringResource(R.string.common_back))
                }
                GlassButton(
                    text = when (step) {
                        0 -> stringResource(R.string.onboarding_next)
                        1 -> stringResource(R.string.onboarding_next)
                        else -> stringResource(R.string.onboarding_agree_start)
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

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ApiConfigStepContent(
    apiUrl: String, apiKey: String,
    onUrlChange: (String) -> Unit, onKeyChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text(stringResource(R.string.onboarding_api_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(
            stringResource(R.string.onboarding_api_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = apiUrl, onValueChange = onUrlChange,
            label = { Text(stringResource(R.string.settings_api_url)) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("https://api.example.com/v1") }
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = apiKey, onValueChange = onKeyChange,
            label = { Text(stringResource(R.string.settings_api_key)) }, singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.chat_quick_preset), style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(6.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
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
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun PrivilegeStepContent(shizuku: ShizukuBridge, dhizuku: DhizukuBridge) {
    val shizukuState by shizuku.state.collectAsState()
    val dhizukuState by dhizuku.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text(
            stringResource(R.string.onboarding_privilege_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            stringResource(R.string.onboarding_privilege_desc_long),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(14.dp))

        PrivilegeRow(
            name = "Shizuku",
            subtitle = stringResource(R.string.privilege_shizuku_desc),
            granted = shizukuState == ShizukuBridge.State.GRANTED,
            unavailable = shizukuState == ShizukuBridge.State.UNAVAILABLE,
            onAction = {
                if (shizukuState == ShizukuBridge.State.UNAVAILABLE) shizuku.refresh()
                else shizuku.requestPermission()
            }
        )
        Spacer(Modifier.height(10.dp))

        PrivilegeRow(
            name = "Dhizuku",
            subtitle = stringResource(R.string.privilege_dhizuku_desc),
            granted = dhizukuState == DhizukuBridge.State.GRANTED,
            unavailable = dhizukuState == DhizukuBridge.State.UNAVAILABLE,
            onAction = {
                if (dhizukuState == DhizukuBridge.State.UNAVAILABLE) dhizuku.refresh()
                else dhizuku.requestPermission()
            }
        )
    }
}

@Composable
private fun PrivilegeRow(
    name: String,
    subtitle: String,
    granted: Boolean,
    unavailable: Boolean,
    onAction: () -> Unit
) {
    GlassPanel(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (granted) Icons.Default.CheckCircle else Icons.Default.Security,
                    contentDescription = null,
                    tint = if (granted) Color(0xFF3DBE7B)
                    else if (unavailable) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        granted -> stringResource(R.string.privilege_state_granted)
                        unavailable -> stringResource(R.string.privilege_state_unavailable)
                        else -> stringResource(R.string.privilege_state_need_permission)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (granted) Color(0xFF3DBE7B) else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(10.dp))
                GlassButton(
                    text = if (granted) stringResource(R.string.privilege_open_settings)
                    else if (unavailable) stringResource(R.string.privilege_redetect)
                    else stringResource(R.string.privilege_authorize),
                    onClick = onAction
                )
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
        Text(stringResource(R.string.onboarding_agreement_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(
            stringResource(R.string.onboarding_agreement_desc),
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
                    stringResource(R.string.agreement_accepted),
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
