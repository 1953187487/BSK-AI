package com.bskai.ui.welcome

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bskai.AuraApp
import com.bskai.data.Agreements
import com.bskai.data.Language
import com.bskai.data.loadLanguages

@Composable
fun WelcomeScreen(app: AuraApp, onDone: () -> Unit) {
    var step by rememberSaveable { mutableStateOf(0) }
    var languageCode by remember { mutableStateOf(app.settings.selectedLanguage()) }
    val languages = remember { loadLanguages(app) }
    var search by rememberSaveable { mutableStateOf("") }
    var agreedOpenSource by rememberSaveable { mutableStateOf(false) }
    var agreedPrivacy by rememberSaveable { mutableStateOf(false) }

    val filtered = remember(search, languages) {
        if (search.isBlank()) languages
        else languages.filter { it.name.contains(search, true) || it.nativeName.contains(search, true) }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
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
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (i == step) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            Box(modifier = Modifier.weight(1f)) {
                when (step) {
                    0 -> LanguageStep(
                        languages = filtered, selected = languageCode,
                        onSelect = { languageCode = it }, search = search, onSearch = { search = it }
                    )
                    1 -> AgreementStep(
                        agreedOpenSource = agreedOpenSource, agreedPrivacy = agreedPrivacy,
                        onToggleOpenSource = { agreedOpenSource = it }, onTogglePrivacy = { agreedPrivacy = it }
                    )
                    else -> WelcomeDoneStep()
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { if (step == 0) onDone() else step -= 1 }) {
                    Text(if (step == 0) "退出" else "上一步")
                }
                Button(
                    enabled = when (step) {
                        0 -> languageCode.isNotBlank()
                        1 -> agreedOpenSource && agreedPrivacy
                        else -> true
                    },
                    onClick = {
                        when (step) {
                            0 -> {
                                app.settings.setSelectedLanguage(languageCode)
                                step = 1
                            }
                            1 -> step = 2
                            else -> {
                                app.settings.setAgreed()
                                app.settings.setLastSeenVersion(com.bskai.BuildConfig.APP_VERSION)
                                onDone()
                            }
                        }
                    }
                ) {
                    Text(if (step == 2) "开始使用" else "下一步")
                }
            }
        }
    }
}

@Composable
private fun LanguageStep(
    languages: List<Language>,
    selected: String,
    onSelect: (String) -> Unit,
    search: String,
    onSearch: (String) -> Unit
) {
    Column {
        Text("选择界面语言", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(
            "你可以随时在设置中更改",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = search, onValueChange = onSearch,
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
                FilterChip(selected = selected == code, onClick = { onSelect(code) }, label = { Text(code.uppercase()) })
            }
        }
        Spacer(Modifier.height(10.dp))
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(languages, key = { "${it.code}-${it.name}" }) { lang ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (selected == lang.code) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else Color.Transparent
                        )
                        .clickable { onSelect(lang.code) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(
                                if (selected == lang.code) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
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
private fun AgreementStep(
    agreedOpenSource: Boolean,
    agreedPrivacy: Boolean,
    onToggleOpenSource: (Boolean) -> Unit,
    onTogglePrivacy: (Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text("用户协议", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(
            "请阅读并同意以下条款后继续使用 AURA。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(14.dp))

        AgreementCard(section = Agreements.openSource, checked = agreedOpenSource, onCheckedChange = onToggleOpenSource)
        Spacer(Modifier.height(12.dp))
        AgreementCard(
            section = Agreements.privacy.copy(body = Agreements.renderPrivacy(com.bskai.BuildConfig.APP_VERSION)),
            checked = agreedPrivacy,
            onCheckedChange = onTogglePrivacy
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun AgreementCard(
    section: com.bskai.data.AgreementSection,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(section.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(
                section.body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onCheckedChange(!checked) }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = checked, onCheckedChange = onCheckedChange)
                Text(
                    "我已阅读并同意",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun WelcomeDoneStep() {
    Column(
        modifier = Modifier.fillMaxSize().padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🎉", fontSize = 64.sp)
        Spacer(Modifier.height(16.dp))
        Text("一切就绪", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "AURA 已准备就绪，点击开始使用即可体验。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
