package com.bskai.ui.agreements

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import org.json.JSONArray

data class Language(
    val code: String,
    val name: String,
    val nativeName: String,
    val region: String
)

data class UpdateAnnouncement(
    val version: String,
    val title: String,
    val content: String,
    val changelog: List<String>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgreementOverlay(
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val context = LocalContext.current
    var currentStep by remember { mutableStateOf(0) }
    var selectedLanguage by remember { mutableStateOf<Language?>(null) }
    var agreedToOpenSource by remember { mutableStateOf(false) }
    var agreedToUsage by remember { mutableStateOf(false) }

    val languages = remember { loadLanguages(context) }
    val announcements = remember { loadUpdateAnnouncements(context) }
    
    // Check if this is first launch or new version
    val prefs = remember { context.getSharedPreferences("aura_prefs", Context.MODE_PRIVATE) }
    val lastVersion = remember { prefs.getString("last_version", null) }
    val currentVersion = "2.0.0-beta.3"
    val isNewVersion = lastVersion != currentVersion
    
    var showAnnouncement by remember { mutableStateOf(false) }
    
    LaunchedEffect(isNewVersion) {
        if (isNewVersion && announcements.isNotEmpty()) {
            delay(800)
            showAnnouncement = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A14))
    ) {
        // Animated background orbs
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .offset(x = (-50).dp, y = 100.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0x306C5CE7), Color.Transparent),
                                center = androidx.compose.ui.geometry.Offset(150f, 150f),
                                radius = 150f
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .size(250.dp)
                        .offset(x = 150.dp, y = (-50).dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0x2000CED1), Color.Transparent),
                                center = androidx.compose.ui.geometry.Offset(125f, 125f),
                                radius = 125f
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .offset(x = 200.dp, y = 400.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0x20FF6B9D), Color.Transparent),
                                center = androidx.compose.ui.geometry.Offset(100f, 100f),
                                radius = 100f
                            )
                        )
                )
            }
        }

        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))

            // Logo section
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF6C5CE7), Color(0xFF00CED1))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "AURA",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 8.sp
                ),
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = getWelcomeText(selectedLanguage?.code ?: "en"),
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFB8B6D0),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            // Version badge
            if (isNewVersion) {
                Surface(
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF6C5CE7)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.NewReleases,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "新版本 $currentVersion 可用",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Step indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(0, 1, 2).forEachIndexed { idx, _ ->
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                if (idx <= currentStep) Color(0xFF6C5CE7)
                                else Color(0xFF2A2A4A)
                            )
                    )
                    if (idx < 2) {
                        Spacer(Modifier.width(8.dp))
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Step content
            when (currentStep) {
                0 -> LanguageSelectionStep(
                    languages = languages,
                    selected = selectedLanguage,
                    onSelect = { selectedLanguage = it }
                )
                1 -> AgreementAndAnnouncementStep(
                    announcements = announcements,
                    agreedToOpenSource = agreedToOpenSource,
                    agreedToUsage = agreedToUsage,
                    onToggleOpenSource = { agreedToOpenSource = it },
                    onToggleUsage = { agreedToUsage = it }
                )
                2 -> WelcomeStep(language = selectedLanguage)
            }

            Spacer(Modifier.height(20.dp))

            // Navigation buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (currentStep > 0) {
                    OutlinedButton(
                        onClick = { currentStep-- },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF6C5CE7))
                    ) {
                        Text(
                            text = getButtonText("back", selectedLanguage?.code),
                            color = Color(0xFFB8B6D0)
                        )
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }

                if (currentStep < 2) {
                    Button(
                        onClick = {
                            if (currentStep == 0 && selectedLanguage != null) {
                                currentStep++
                            } else if (currentStep == 1 && agreedToOpenSource && agreedToUsage) {
                                currentStep++
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        enabled = when (currentStep) {
                            0 -> selectedLanguage != null
                            1 -> agreedToOpenSource && agreedToUsage
                            else -> true
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF6C5CE7), Color(0xFF00CED1))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = getButtonText("next", selectedLanguage?.code, currentStep, agreedToOpenSource, agreedToUsage),
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            if (isNewVersion) {
                                prefs.edit().putString("last_version", currentVersion).apply()
                            }
                            onAccept()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        )
                    ) {
                        Text(
                            text = getButtonText("start", selectedLanguage?.code),
                            color = Color(0xFFB8B6D0)
                        )
                    }
                    Button(
                        onClick = onDecline,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        )
                    ) {
                        Text(
                            text = getButtonText("exit", selectedLanguage?.code),
                            color = Color(0xFFB8B6D0)
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }

        // Announcement dialog
        if (showAnnouncement && announcements.isNotEmpty()) {
            UpdateAnnouncementDialog(
                announcement = announcements.first(),
                onDismiss = { showAnnouncement = false }
            )
        }
    }
}

@Composable
fun LanguageSelectionStep(
    languages: List<Language>,
    selected: Language?,
    onSelect: (Language) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showAll by remember { mutableStateOf(false) }

    val filtered = remember(languages, searchQuery) {
        if (searchQuery.isEmpty()) languages
        else languages.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.nativeName.contains(searchQuery, ignoreCase = true) ||
            it.code.contains(searchQuery, ignoreCase = true) ||
            it.region.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            placeholder = {
                Text(
                    text = if (selected?.code == "zh") "搜索语言..." else "Search languages...",
                    color = Color(0xFF6B6B8D)
                )
            },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF6B6B8D))
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF6C5CE7),
                unfocusedBorderColor = Color(0xFF2A2A4A),
                cursorColor = Color(0xFF6C5CE7)
            ),
            shape = RoundedCornerShape(12.dp)
        )

        // Popular languages quick select
        if (!showAll) {
            Text(
                text = if (selected?.code == "zh") "常用语言" else "Popular",
                style = MaterialTheme.typography.titleSmall,
                color = Color(0xFF6C5CE7),
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(Modifier.height(8.dp))

            val popular = languages.take(12)
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(popular) { lang ->
                    LanguageChip(
                        language = lang,
                        isSelected = selected?.code == lang.code,
                        onClick = { onSelect(lang) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Show more button
            TextButton(
                onClick = { showAll = true },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = if (selected?.code == "zh") "显示全部 ${languages.size} 种语言"
                           else "Show all ${languages.size} languages",
                    color = Color(0xFF00CED1)
                )
            }
        } else {
            // All languages grid
            Text(
                text = if (searchQuery.isNotEmpty()) "$filtered.size results"
                       else if (selected?.code == "zh") "全部语言"
                       else "All Languages",
                style = MaterialTheme.typography.titleSmall,
                color = Color(0xFF6C5CE7),
                modifier = Modifier.align(Alignment.Start)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .height(320.dp)
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                gridItems(filtered) { lang ->
                    LanguageChip(
                        language = lang,
                        isSelected = selected?.code == lang.code,
                        onClick = { onSelect(lang) }
                    )
                }
            }

            // Back button
            TextButton(
                onClick = { showAll = false; searchQuery = "" },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = if (selected?.code == "zh") "返回常用语言"
                           else "Back to Popular",
                    color = Color(0xFFB8B6D0)
                )
            }
        }
    }
}

@Composable
fun LanguageChip(
    language: Language,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val brush = if (isSelected) {
        Brush.linearGradient(listOf(Color(0xFF6C5CE7), Color(0xFF00CED1)))
    } else {
        Brush.linearGradient(listOf(Color(0xFF1E1E3A), Color(0xFF2A2A4A)))
    }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(brush)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = language.nativeName,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) Color.White else Color(0xFFF0EEFF),
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
        if (isSelected) {
            Text(
                text = language.name,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFB8B6D0)
            )
        }
    }
}

@Composable
fun AgreementAndAnnouncementStep(
    announcements: List<UpdateAnnouncement>,
    agreedToOpenSource: Boolean,
    agreedToUsage: Boolean,
    onToggleOpenSource: (Boolean) -> Unit,
    onToggleUsage: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Update Announcement Card
        if (announcements.isNotEmpty()) {
            AnnouncementCard(announcement = announcements.first())
        }

        // Two-column layout for agreements
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Open Source Agreement - Left side
            AgreementCard(
                title = "一、开源协议许可",
                accepted = agreedToOpenSource,
                onAccepted = { onToggleOpenSource(it) },
                modifier = Modifier.weight(1f)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LicenseSection(
                        title = "Apache License 2.0",
                        year = "2026",
                        holder = "BSK-AI Project"
                    ) {
                        """
                        Licensed under the Apache License, Version 2.0 (the "License");
                        you may not use this software except in compliance with the License.
                        You may obtain a copy of the License at

                        http://www.apache.org/licenses/LICENSE-2.0

                        Unless required by applicable law or agreed to in writing, software
                        distributed under the License is distributed on an "AS IS" BASIS,
                        WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
                        See the License for the specific language governing permissions and
                        limitations under the License.

                        This project uses the following third-party components:
                        - Android SDK (Apache 2.0)
                        - Jetpack Compose (Apache 2.0)
                        - Kotlin (Apache 2.0)
                        - OkHttp (Apache 2.0)
                        """.trimIndent()
                    }
                    LicenseSection(
                        title = "MIT License - API Integration",
                        year = "2026",
                        holder = "Custom API Client"
                    ) {
                        """
                        Permission is hereby granted, free of charge, to any person obtaining a copy
                        of this software and associated documentation files (the "Software"), to deal
                        in the Software without restriction, including without limitation the rights
                        to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
                        copies of the Software.
                        """.trimIndent()
                    }
                }
            }

            // Usage Agreement - Right side
            AgreementCard(
                title = "二、使用协议",
                accepted = agreedToUsage,
                onAccepted = { onToggleUsage(it) },
                modifier = Modifier.weight(1f)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader("服务说明")
                    BodyText("AURA是一款运行在Android设备上的智能语音助手应用，通过语音识别、自然语言处理等技术，为用户提供语音交互服务。支持多种语言识别和合成，适配全球用户。")

                    SectionHeader("用户权利")
                    BodyText("1. 您可以在自己的设备上自由安装和使用本应用。")
                    BodyText("2. 您可以查看源代码（如适用），并根据开源协议进行修改和分发。")
                    BodyText("3. 您有权通过设置页面配置自定义API服务商。")
                    BodyText("4. 您可以选择使用不同的语音引擎和语言包。")

                    SectionHeader("用户义务")
                    BodyText("1. 您应当合法使用本应用，不得利用本应用从事任何违法活动。")
                    BodyText("2. 您应当妥善保管您的API密钥，不得将其泄露给第三方。")
                    BodyText("3. 您在使用语音功能时，应确保所处环境符合录音的法律要求。")
                    BodyText("4. 不得对本应用进行反向工程、反编译或破解。")

                    SectionHeader("隐私保护")
                    BodyText("1. 本应用仅在本地收集和处理您的语音数据，不会将您的语音上传到第三方服务器（除非您配置了自定义API）。")
                    BodyText("2. 对话历史记录仅保存在本地设备上。")
                    BodyText("3. 如您配置了自定义API，数据将传输至您指定的服务商，请自行了解其隐私政策。")
                    BodyText("4. 我们不会收集您的个人身份信息，也不会与第三方共享您的数据。")

                    SectionHeader("免责声明")
                    BodyText("1. 本应用按原样提供，不提供任何明示或暗示的担保。")
                    BodyText("2. 开发者不对因使用本应用而产生的任何直接或间接损失承担责任。")
                    BodyText("3. 用户应自行承担使用本应用的风险。")
                    BodyText("4. 开发者保留随时更新本应用的权利，包括但不限于功能更新、安全补丁等。")
                }
            }
        }

        // Agreement to continue
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = agreedToOpenSource && agreedToUsage,
                onCheckedChange = { checked ->
                    onToggleOpenSource(checked)
                    onToggleUsage(checked)
                }
            )
            Text(
                text = "我已阅读并同意以上所有协议",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFB8B6D0)
            )
        }
    }
}

@Composable
fun AnnouncementCard(announcement: UpdateAnnouncement) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, Color(0xFF6C5CE7))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Color(0xFF6C5CE7), Color(0xFF00CED1)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NewReleases,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = announcement.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFF0EEFF),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = announcement.version,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF6C5CE7)
                        )
                    }
                }
                Text(
                    text = "更新",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF00CED1),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x2000CED1))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            
            Spacer(Modifier.height(12.dp))
            
            Text(
                text = announcement.content,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFB8B6D0),
                lineHeight = 18.sp
            )
            
            if (announcement.changelog.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "更新内容：",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFFF0EEFF),
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                announcement.changelog.forEach { item ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(text = "•", color = Color(0xFF6C5CE7))
                        Text(text = item, style = MaterialTheme.typography.bodySmall, color = Color(0xFFB8B6D0))
                    }
                }
            }
        }
    }
}

@Composable
fun AgreementCard(
    title: String,
    accepted: Boolean,
    onAccepted: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (accepted) Color(0xFF1A1A2E) else Color(0xFF12121E)
        ),
        shape = RoundedCornerShape(16.dp),
        border = if (accepted) BorderStroke(1.5.dp, Color(0xFF6C5CE7)) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (accepted) Color(0xFF6C5CE7) else Color(0xFFF0EEFF),
                    fontWeight = FontWeight.SemiBold
                )
                FilterChip(
                    selected = accepted,
                    onClick = { onAccepted(!accepted) },
                    label = {
                        Text(
                            text = if (accepted) "已同意" else "同意",
                            color = if (accepted) Color(0xFF6C5CE7) else Color(0xFFB8B6D0)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0x206C5CE7)
                    )
                )
            }
            Spacer(Modifier.height(12.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0D0D1A)),
                shape = RoundedCornerShape(12.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item { content() }
                }
            }
        }
    }
}

@Composable
private fun LicenseSection(title: String, year: String, holder: String, content: () -> String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFF6C5CE7)
            )
            Text(
                text = "$holder · $year",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF6B6B8D)
            )
        }
        Text(
            text = content(),
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFB8B6D0),
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = Color(0xFFF0EEFF)
    )
}

@Composable
private fun BodyText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = Color(0xFFB8B6D0),
        lineHeight = 16.sp
    )
}

@Composable
fun WelcomeStep(language: Language?) {
    val code = language?.code ?: "en"
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Success animation circle
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF6C5CE7), Color(0xFF00CED1))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }

        Text(
            text = if (code == "zh") "准备就绪！" else "Ready to Go!",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = if (code == "zh") "欢迎使用AURA智能语音助手" 
                   else "Welcome to AURA Smart Voice Assistant",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFFB8B6D0),
            textAlign = TextAlign.Center
        )

        if (language != null) {
            Surface(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x206C5CE7)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${language.nativeName} (${language.code})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF6C5CE7),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(text = "·", color = Color(0xFF6B6B8D))
                    Text(
                        text = language.region,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFB8B6D0)
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        val features = if (code == "zh") 
            listOf("语音识别", "文字转语音", "自定义API", "多语言支持", "本地隐私保护")
        else 
            listOf("Speech Recognition", "Text-to-Speech", "Custom API", "Multi-language", "Local Privacy")

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(features) { feature ->
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x1A6C5CE7)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = feature,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFB8B6D0)
                    )
                }
            }
        }
    }
}

@Composable
fun UpdateAnnouncementDialog(
    announcement: UpdateAnnouncement,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.5.dp, Color(0xFF6C5CE7))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Color(0xFF6C5CE7), Color(0xFF00CED1)))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.NewReleases,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = announcement.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = announcement.version,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF6C5CE7),
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = announcement.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFB8B6D0),
                    textAlign = TextAlign.Center
                )

                if (announcement.changelog.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "更新内容：",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFF0EEFF),
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    announcement.changelog.forEach { item ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(text = "•", color = Color(0xFF6C5CE7))
                            Text(text = item, style = MaterialTheme.typography.bodySmall, color = Color(0xFFB8B6D0))
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C5CE7)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("知道了", color = Color.White)
                }
            }
        }
    }
}

// Helper functions
fun getWelcomeText(code: String?): String {
    return when (code) {
        "zh" -> "智能语音助手"
        "ja" -> "スマート音声アシスタント"
        "ko" -> "스마트 음성 도우미"
        else -> "Smart Voice Assistant"
    }
}

fun getButtonText(type: String, lang: String?, step: Int = 0, src: Boolean = false, usage: Boolean = false): String {
    val isZh = lang == "zh"
    return when (type) {
        "back" -> if (isZh) "上一步" else "Back"
        "next" -> when {
            step == 2 -> if (isZh) "开始使用" else "Get Started"
            src && usage -> if (isZh) "进入应用" else "Enter App"
            else -> if (isZh) "下一步" else "Next"
        }
        "start" -> if (isZh) "开始使用" else "Start"
        "exit" -> if (isZh) "退出" else "Exit"
        else -> ""
    }
}

fun loadLanguages(context: Context): List<Language> {
    return try {
        val json = context.resources.openRawResource(com.bskai.R.raw.languages).bufferedReader().use { it.readText() }
        val array = JSONArray(json)
        (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            Language(
                code = obj.getString("code"),
                name = obj.getString("name"),
                nativeName = obj.getString("nativeName"),
                region = obj.getString("region")
            )
        }
    } catch (e: Exception) {
        listOf(Language("en", "English", "English", "Global"))
    }
}

fun loadUpdateAnnouncements(context: Context): List<UpdateAnnouncement> {
    return listOf(
        UpdateAnnouncement(
            version = "v2.0.0-beta.3",
            title = "AURA 2.0 Beta 3 发布",
            content = "本次更新全面重写UI，新增多语言支持、自定义API配置、声音克隆功能。优化了交互体验，修复了已知问题。",
            changelog = listOf(
                "全面重写UI设计，采用现代化深色主题",
                "新增多语言支持，覆盖200+国家和地区语言",
                "新增自定义API配置，支持DeepSeek、ChatGLM等",
                "新增声音克隆功能对话框",
                "优化语音识别和文字转语音体验",
                "修复协议弹窗跳转循环问题",
                "新增版本更新公告系统",
                "优化权限管理界面"
            )
        ),
        UpdateAnnouncement(
            version = "v2.0.0-beta.2",
            title = "AURA 2.0 Beta 2 发布",
            content = "首次发布AURA 2.0版本，引入全新的用户界面和交互体验。",
            changelog = listOf(
                "全新设计的用户界面",
                "精简导航为双Tab布局",
                "新增开源协议和使用协议弹窗",
                "优化设备识别和权限展示",
                "集成自定义API调用功能"
            )
        )
    )
}
