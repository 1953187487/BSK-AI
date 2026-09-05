package com.bskai.ui.update

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bskai.BuildConfig
import com.bskai.update.DownloadStatus
import com.bskai.update.GitHubApi
import com.bskai.update.RemoteRelease
import com.bskai.update.UpdateCheckResult
import com.bskai.update.UpdateInstaller
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun CombinedUpdateDialog(
    result: UpdateCheckResult,
    currentVersionSigned: String,
    onAgreementNeeded: (String) -> Boolean,
    onAgreementRequested: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val target = result.latestRelease

    var tab by remember { mutableIntStateOf(0) }

    var status by remember { mutableStateOf<DownloadStatus>(DownloadStatus.Idle) }
    var downloadJob by remember { mutableStateOf<Job?>(null) }

    DisposableEffect(Unit) {
        onDispose { downloadJob?.cancel() }
    }

    fun startDownload(release: RemoteRelease) {
        if (release.apkUrl.isBlank()) return
        if (onAgreementNeeded(release.versionName)) {
            onAgreementRequested(release.versionName)
            return
        }
        downloadJob?.cancel()
        val cacheFile = File(context.cacheDir, "update/${release.versionName}.apk")
        downloadJob = scope.launch {
            status = DownloadStatus.Downloading(0, release.sizeBytes)
            GitHubApi.downloadApk(release.apkUrl, cacheFile).collectLatest { st ->
                status = st
                if (st is DownloadStatus.Failed) downloadJob = null
                if (st is DownloadStatus.Done) downloadJob = null
            }
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (status !is DownloadStatus.Downloading) onDismiss()
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Update, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("更新中心", fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "当前：${BuildConfig.APP_VERSION} · 协议：$currentVersionSigned",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column {
                TabRow(
                    selectedTabIndex = tab,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Tab(
                        selected = tab == 0,
                        onClick = { tab = 0 },
                        text = {
                            val label = if (result.hasUpdate) "最新 (有更新)" else "最新"
                            Text(label, fontWeight = if (result.hasUpdate) FontWeight.SemiBold else FontWeight.Normal)
                        },
                        icon = { Icon(Icons.Default.Update, contentDescription = null) }
                    )
                    Tab(
                        selected = tab == 1,
                        onClick = { tab = 1 },
                        text = { Text("历史版本 (${result.releases.size})") },
                        icon = { Icon(Icons.Default.History, contentDescription = null) }
                    )
                }
                Spacer(Modifier.height(10.dp))
                if (tab == 0) LatestTab(
                    target = target,
                    result = result,
                    status = status,
                    onDownload = { target?.let { startDownload(it) } },
                    onCancel = {
                        downloadJob?.cancel()
                        status = DownloadStatus.Idle
                    }
                ) else HistoryTab(releases = result.releases)
            }
        },
        confirmButton = {
            if (tab == 0 && status is DownloadStatus.Done) {
                Button(onClick = {
                    UpdateInstaller.install(context, File((status as DownloadStatus.Done).localPath))
                }) {
                    Icon(Icons.Default.InstallMobile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("立即安装")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text(if (status is DownloadStatus.Downloading) "隐藏" else "关闭")
                }
            }
        }
    )
}

@Composable
private fun LatestTab(
    target: RemoteRelease?,
    result: UpdateCheckResult,
    status: DownloadStatus,
    onDownload: () -> Unit,
    onCancel: () -> Unit
) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        if (target == null) {
            Text("暂未获取到版本信息", style = MaterialTheme.typography.bodyMedium)
            return
        }
        Text(
            text = "${target.name} · ${target.versionName}",
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "发布：${target.publishedAtLabel()} · ${formatSize(target.sizeBytes)}" +
                if (target.isPrerelease) " · 测试版" else "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        if (result.hasUpdate) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "检测到新版本，点击下方按钮下载并安装。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(10.dp)
                )
            }
        } else {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(10.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("已是最新版本", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (target.body.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(target.body, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(12.dp))
        when (val s = status) {
            is DownloadStatus.Downloading -> {
                LinearProgressIndicator(
                    progress = { s.percent.coerceIn(0, 100) / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                ) {
                    Text("${s.percent}%", style = MaterialTheme.typography.bodySmall)
                    Text(
                        "${formatSize(s.bytesRead)} / ${formatSize(s.total)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onCancel) { Text("取消下载") }
            }
            is DownloadStatus.Failed -> {
                Text(
                    text = "下载失败：${s.message}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            is DownloadStatus.Done -> {
                Text(
                    text = "下载完成：${s.localPath}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else -> {
                if (target.apkUrl.isNotBlank()) {
                    Button(
                        onClick = onDownload,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (result.hasUpdate) "下载并安装" else "重新下载")
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryTab(releases: List<RemoteRelease>) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    if (releases.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
            Text("暂无可用历史版本")
        }
        return
    }
    LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
        items(releases, key = { it.versionName }) { release ->
            HistoryRow(
                release = release,
                onDownload = {
                    if (release.apkUrl.isBlank()) return@HistoryRow
                    val f = File(context.cacheDir, "update/${release.versionName}.apk")
                    scope.launch {
                        GitHubApi.downloadApk(release.apkUrl, f).collectLatest { /* handled in row */ }
                    }
                }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        }
    }
}

@Composable
private fun HistoryRow(release: RemoteRelease, onDownload: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<DownloadStatus>(DownloadStatus.Idle) }
    var job by remember { mutableStateOf<Job?>(null) }

    DisposableEffect(release.versionName) {
        onDispose { job?.cancel() }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${release.name} · ${release.versionName}",
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${release.publishedAtLabel()} · ${formatSize(release.sizeBytes)}" +
                        if (release.isPrerelease) " · 测试版" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            when (val s = status) {
                is DownloadStatus.Downloading -> {
                    Text(
                        text = "${s.percent}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                is DownloadStatus.Done -> {
                    OutlinedButton(onClick = {
                        UpdateInstaller.install(context, File(s.localPath))
                    }) {
                        Icon(Icons.Default.InstallMobile, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("安装")
                    }
                }
                is DownloadStatus.Failed -> {
                    Text(
                        text = "失败",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                else -> {
                    if (release.apkUrl.isNotBlank()) {
                        OutlinedButton(onClick = {
                            job?.cancel()
                            val f = File(context.cacheDir, "update/${release.versionName}.apk")
                            job = scope.launch {
                                status = DownloadStatus.Downloading(0, release.sizeBytes)
                                GitHubApi.downloadApk(release.apkUrl, f).collectLatest { st ->
                                    status = st
                                    if (st is DownloadStatus.Done || st is DownloadStatus.Failed) job = null
                                }
                            }
                        }) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("下载")
                        }
                    }
                }
            }
        }
        if (status is DownloadStatus.Downloading) {
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { (status as DownloadStatus.Downloading).percent.coerceIn(0, 100) / 100f },
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (status is DownloadStatus.Failed) {
            Text(
                text = (status as DownloadStatus.Failed).message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0L) return "?"
    val mb = bytes.toDouble() / (1024 * 1024)
    return "%.2f MB".format(mb)
}
