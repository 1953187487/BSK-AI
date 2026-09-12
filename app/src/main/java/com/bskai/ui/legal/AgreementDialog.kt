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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bskai.data.AgreementSection
import com.bskai.data.Agreements
import com.bskai.data.DefaultApiUrlPresets
import com.bskai.data.Language
import com.bskai.data.loadLanguages
import com.bskai.ui.glass.GlassButton
import com.bskai.ui.glass.GlassPanel
import com.bskai.ui.glass.rememberGlassColors

@Composable
fun FourStepAgreementDialog(onComplete: () -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var languageCode by rememberSaveable { mutableStateOf("zh") }
    var apiUrl by rememberSaveable { mutableStateOf("") }
    var apiKey by rememberSaveable { mutableStateOf("") }
    var agreedOpenSource by rememberSaveable { mutableStateOf(false) }
    var agreedUserNotice by rememberSaveable { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val languages = remember { loadLanguages(context) }
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
                    repeat(4) { i ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 2.dp)
                                .size(8.dp)
                                .androidClip(RoundedCornerShape(4.dp))
                                .then(
                                    if (i <= step) Modifier.background(glass.accent, RoundedCornerShape(4.dp))
                                    else Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                                )
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Box(modifier = Modifier.weight(1f)) {
                when (step) {
                    0 -> LanguageStepContent(
                        languages = languages, selected = languageCode, onSelect = { languageCode = it }
                    )
                    1 -> ApiConfigStepContent(
                        apiUrl = apiUrl, apiKey = apiKey,
                        onUrlChange = { apiUrl = it }, onKeyChange = { apiKey = it }
                    )
                    2 -> PermissionAndToolsStepContent()
                    3 -> AgreementStepContent(
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
                        2 -> "跳过"
                        3 -> "同意并开始使用"
                        else -> "下一步"
                    },
                    enabled = when (step) {
                        0 -> languageCode.isNotBlank()
                        3 -> agreedOpenSource && agreedUserNotice
                        else -> true
                    },
                    onClick = {
                        when (step) {
                            0 -> step = 1
                            1 -> step = 2
                            2 -> step = 3
                            3 -> onComplete()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun LanguageStepContent(
    languages: List<Language>,
    selected: String,
    onSelect: (String) -> Unit
) {
    var search by rememberSaveable { mutableStateOf("") }
    val filtered = remember(search, languages) {
        if (search.isBlank()) languages
        else languages.filter { it.name.contains(search, true) || it.nativeName.contains(search, true) }
    }
    val glass = rememberGlassColors()

    Column(modifier = Modifier.fillMaxSize()) {
        Text("选择界面语言", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(
            "选择您偏好的语言，稍后可在设置中更改",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = search, onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = { Text("搜索语言") },
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
        )
        Spacer(Modifier.height(10.dp))
        val popular = listOf("zh", "en", "ja", "ko", "es", "fr", "de")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            popular.forEach { code ->
                FilterChip(
                    selected = selected == code, onClick = { onSelect(code) },
                    label = { Text(code.uppercase()) }
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(languages, key = { "${it.code}-${it.name}" }) { lang ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .androidClip(RoundedCornerShape(12.dp))
                        .then(
                            if (selected == lang.code) Modifier.background(
                                glass.accent.copy(alpha = 0.18f),
                                RoundedCornerShape(12.dp)
                            ) else Modifier
                        )
                        .androidClickable { onSelect(lang.code) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .androidClip(RoundedCornerShape(9.dp))
                            .then(
                                if (selected == lang.code) androidx.compose.ui.Modifier.background(
                                    glass.accent,
                                    RoundedCornerShape(9.dp)
                                ) else androidx.compose.ui.Modifier.background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(9.dp)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected == lang.code) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(lang.nativeName.ifBlank { lang.name }, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.weight(1f))
                    Text(lang.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
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
            "配置 AI 服务地址和密钥，稍后可在设置中更改。您可以跳过此步骤。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = apiUrl, onValueChange = onUrlChange,
            label = { Text("API 地址") }, singleLine = true, modifier = Modifier.fillMaxWidth()
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
private fun PermissionAndToolsStepContent() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    var downloading by remember { mutableStateOf(false) }
    var downloaded by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text("权限与开发工具", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(
            "授权基础权限并准备构建工具，均为可选步骤，稍后可在设置中完成。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(14.dp))

        GlassPanel(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("通知权限", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    Text("用于显示后台通知", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) { Text("授权") }
            }
        }
        Spacer(Modifier.height(10.dp))
        GlassPanel(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("存储权限", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    Text("用于读写工作区文件", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = { permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE) }) { Text("授权") }
            }
        }
        Spacer(Modifier.height(10.dp))
        GlassPanel(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("APK 构建工具", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                        Text("包含 aapt2、d8、apksigner 等构建工具链", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (downloading || downloaded) {
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (downloaded) "下载完成" else "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!downloading && !downloaded) {
                    Spacer(Modifier.height(10.dp))
                    GlassButton(
                        text = "下载构建工具",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            downloading = true
                            progress = 0.3f
                            downloaded = true
                            downloading = false
                            progress = 1f
                        }
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
        AgreementCard(section = Agreements.userNotice.copy(body = Agreements.renderUserNotice()), checked = agreedUserNotice, onCheckedChange = onToggleUserNotice)
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

private fun Modifier.androidClip(shape: RoundedCornerShape): Modifier =
    this.clip(shape)
