package com.floatai.ui.screens.about

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.floatai.BuildConfig
import com.floatai.data.remote.ReleaseNote
import com.floatai.data.remote.UpdateRepository
import com.floatai.ui.components.GlassCard
import com.floatai.ui.components.SectionTitle
import com.floatai.ui.i18n.localStrings
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 关于页：版本信息 + 检查更新 + 历史 release 列表 + 开源协议链接。
 */
@Composable
fun AboutScreen() {
    val strings = localStrings()
    val context = LocalContext.current
    val app = context.applicationContext as com.floatai.App
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var checkMessage by remember { mutableStateOf<String?>(null) }
    var isNewer by remember { mutableStateOf(false) }
    var releases by remember { mutableStateOf<List<ReleaseNote>>(emptyList()) }
    var filterChannel by remember { mutableStateOf("all") }

    val settings by app.settingsRepository.settings.collectAsStateWithLifecycle()
    val currentChannel = filterChannel
    val currentTag = "v${BuildConfig.VERSION_NAME}"

    fun openUrl(url: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    LaunchedEffect(currentChannel) {
        loading = true
        releases = UpdateRepository.loadRecentByChannel(20, currentTag, currentChannel)
        loading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        SectionTitle(strings.about_app)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                InfoRow(strings.about_version, "v${BuildConfig.VERSION_NAME} (code ${BuildConfig.VERSION_CODE})")
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                InfoRow(strings.about_protocol, "v${BuildConfig.PROTOCOL_VERSION}")
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                InfoRow(strings.about_build_type, if (BuildConfig.DEBUG) "Debug" else "Release")
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                InfoRow(
                    strings.about_repo,
                    "github.com/1953187487/FloatAI",
                    onClick = { openUrl("https://github.com/1953187487/FloatAI") }
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionTitle(strings.about_open_source)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                InfoRow(
                    strings.about_license,
                    "Apache License 2.0",
                    onClick = { openUrl("https://www.apache.org/licenses/LICENSE-2.0") }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                InfoRow(
                    strings.about_github,
                    "View source on GitHub",
                    onClick = { openUrl("https://github.com/1953187487/FloatAI") }
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionTitle(strings.about_updates)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(22.dp)
                                .clickable {
                                    scope.launch {
                                        loading = true
                                        val info = UpdateRepository.checkLatest("v${BuildConfig.VERSION_NAME}")
                                        checkMessage = if (info.isNewer) {
                                            "发现新版本：${info.latestTag}"
                                        } else if (info.latestTag.isNotBlank()) {
                                            "当前已是最新版本 (${info.latestTag})"
                                        } else {
                                            info.changelog
                                        }
                                        isNewer = info.isNewer
                                        releases = UpdateRepository.loadRecentByChannel(20, currentTag, currentChannel)
                                        loading = false
                                    }
                                }
                        )
                    }
                    Text(
                        text = checkMessage ?: strings.about_check_now,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.clickable {
                            scope.launch {
                                loading = true
                                val info = UpdateRepository.checkLatest("v${BuildConfig.VERSION_NAME}")
                                checkMessage = if (info.isNewer) {
                                    "发现新版本：${info.latestTag}"
                                } else if (info.latestTag.isNotBlank()) {
                                    "当前已是最新版本 (${info.latestTag})"
                                } else {
                                    info.changelog
                                }
                                isNewer = info.isNewer
                                releases = UpdateRepository.loadRecentByChannel(20, currentTag, currentChannel)
                                loading = false
                            }
                        }
                    )
                }
                if (isNewer) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            strings.about_new_version_available,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
                if (!loading && releases.isEmpty()) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            strings.about_no_network,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionTitle(strings.about_history)
        Text(
            "每个版本号、发布时间、变更内容均来自 GitHub Releases。",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        )
        // 通道过滤
        Row(
            modifier = Modifier.padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                "all" to "全部",
                "major" to "大版本",
                "patch" to "补丁版"
            ).forEach { (key, label) ->
                FilterChip(
                    selected = currentChannel == key,
                    onClick = { filterChannel = key },
                    label = { Text(label, fontSize = 11.sp) },
                    leadingIcon = if (currentChannel == key) {
                        { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(12.dp)) }
                    } else null
                )
            }
        }
        LazyColumn(
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(releases) { note ->
                ReleaseDetailCard(note = note, onOpen = {
                    note.downloadUrl?.takeIf { it.isNotBlank() }?.let(::openUrl)
                })
            }
        }
    }
}

@Composable
private fun ReleaseDetailCard(note: ReleaseNote, onOpen: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Code,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    note.tag,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.size(8.dp))
                Box(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        formatDate(note.publishedAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    if (expanded) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { expanded = !expanded }
                )
            }

            if (note.apkSize > 0) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "APK 大小：${formatSize(note.apkSize)}" +
                        (if (note.author.isNotBlank()) "  ·  作者：${note.author}" else ""),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (note.fullBody.isNotBlank() || note.summary.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                val displayText = if (expanded) (note.fullBody.ifBlank { note.summary })
                else (note.summary.ifBlank { note.fullBody.take(200) })
                Text(
                    displayText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = if (expanded) Int.MAX_VALUE else 4,
                    modifier = Modifier.clickable { expanded = !expanded }
                )
                if (!expanded && (note.fullBody.length > 200 || note.fullBody != note.summary)) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "点击展开查看完整内容",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { expanded = true }
                    )
                }
            }

            if (note.downloadUrl?.isNotBlank() == true) {
                Spacer(Modifier.height(6.dp))
                TextButton(onClick = onOpen) {
                    Icon(Icons.Filled.OpenInNew, contentDescription = null,
                        modifier = Modifier.size(14.dp))
                    Text(" 在浏览器查看", fontSize = 11.sp)
                }
            }
        }
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "?"
    val units = arrayOf("B", "KB", "MB", "GB")
    var size = bytes.toDouble()
    var i = 0
    while (size >= 1024 && i < units.size - 1) { size /= 1024; i++ }
    return if (i == 0) "$bytes B" else "%.1f %s".format(size, units[i])
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    onClick: (() -> Unit)? = null
) {
    val modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp)
        .let { if (onClick != null) it.clickable { onClick() } else it }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

private fun formatDate(timestamp: Long): String {
    if (timestamp <= 0) return ""
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(timestamp))
}
