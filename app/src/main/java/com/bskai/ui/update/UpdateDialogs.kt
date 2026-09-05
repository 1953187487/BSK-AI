package com.bskai.ui.update

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
fun UpdateDialog(
    result: UpdateCheckResult,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val target = result.latestRelease

    var status by remember { mutableStateOf<DownloadStatus>(DownloadStatus.Idle) }
    var downloadJob by remember { mutableStateOf<Job?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            downloadJob?.cancel()
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (status !is DownloadStatus.Downloading) onDismiss()
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val (icon, color) = when {
                    status is DownloadStatus.Done -> Icons.Default.InstallMobile to MaterialTheme.colorScheme.primary
                    status is DownloadStatus.Failed -> Icons.Default.Update to MaterialTheme.colorScheme.error
                    result.hasUpdate -> Icons.Default.Update to MaterialTheme.colorScheme.primary
                    else -> Icons.Default.CheckCircle to MaterialTheme.colorScheme.secondary
                }
                Icon(icon, contentDescription = null, tint = color)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = when (status) {
                        is DownloadStatus.Done -> "下载完成"
                        is DownloadStatus.Failed -> "下载失败"
                        else -> if (result.hasUpdate) "发现新版本" else "已是最新"
                    },
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "当前版本：${BuildConfig.APP_VERSION} (${BuildConfig.BUILD_NUMBER})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                if (target != null) {
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
                    if (target.body.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(target.body, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(10.dp))
                    when (val s = status) {
                        is DownloadStatus.Downloading -> {
                            LinearProgressIndicator(
                                progress = { (s.percent.coerceIn(0, 100)) / 100f },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "${s.percent}% · ${formatSize(s.bytesRead)} / ${formatSize(s.total)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        is DownloadStatus.Failed -> {
                            Text(
                                text = "错误：${s.message}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        is DownloadStatus.Done -> {
                            Text(
                                text = "已下载到本地：${s.localPath}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        else -> {}
                    }
                }
            }
        },
        confirmButton = {
            when (val s = status) {
                is DownloadStatus.Done -> {
                    Button(onClick = {
                        UpdateInstaller.install(context, File(s.localPath))
                    }) {
                        Icon(
                            Icons.Default.InstallMobile,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("立即安装")
                    }
                }
                is DownloadStatus.Downloading -> {
                    Button(onClick = {
                        downloadJob?.cancel()
                        status = DownloadStatus.Idle
                    }) {
                        Text("取消下载")
                    }
                }
                else -> {
                    if (target != null && target.apkUrl.isNotBlank()) {
                        Button(onClick = {
                            downloadJob?.cancel()
                            val cacheFile = File(context.cacheDir, "update/${target.versionName}.apk")
                            downloadJob = scope.launch {
                                status = DownloadStatus.Downloading(0, target.sizeBytes)
                                GitHubApi.downloadApk(target.apkUrl, cacheFile).collectLatest { st ->
                                    status = st
                                    if (st is DownloadStatus.Failed) downloadJob = null
                                    if (st is DownloadStatus.Done) downloadJob = null
                                }
                            }
                        }) {
                            Icon(
                                Icons.Default.CloudDownload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(if (result.hasUpdate) "下载并安装" else "重新下载")
                        }
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (status is DownloadStatus.Downloading) "隐藏" else "关闭")
            }
        }
    )
}

@Composable
fun HistoryDialog(
    releases: List<RemoteRelease>,
    loading: Boolean,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.History, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("历史版本", fontWeight = FontWeight.SemiBold)
            }
        },
        text = {
            Box(modifier = Modifier.height(360.dp)) {
                if (loading) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (releases.isEmpty()) {
                    Text("暂无可用历史版本")
                } else {
                    LazyColumn {
                        items(releases) { release ->
                            HistoryRow(release = release)
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

@Composable
private fun HistoryRow(release: RemoteRelease) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<DownloadStatus>(DownloadStatus.Idle) }
    var job by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(release.versionName) {
        // 行销毁时取消下载
    }
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
