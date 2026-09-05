package com.bskai.ui.update

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bskai.BuildConfig
import com.bskai.update.RemoteRelease
import com.bskai.update.UpdateCheckResult

@Composable
fun UpdateDialog(
    result: UpdateCheckResult,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (result.hasUpdate) Icons.Default.Update else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (result.hasUpdate) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (result.hasUpdate) "发现新版本" else "已是最新",
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
                val target = result.latestRelease
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
                }
            }
        },
        confirmButton = {
            val target = result.latestRelease
            if (target != null && target.apkUrl.isNotBlank()) {
                Button(onClick = {
                    openUrl(context, target.apkUrl)
                    onDismiss()
                }) {
                    Icon(
                        Icons.Default.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("下载更新")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

@Composable
fun HistoryDialog(
    releases: List<RemoteRelease>,
    loading: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
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
                            ReleaseRow(release = release) {
                                if (release.apkUrl.isNotBlank()) openUrl(context, release.apkUrl)
                            }
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
private fun ReleaseRow(release: RemoteRelease, onDownload: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
        if (release.apkUrl.isNotBlank()) {
            OutlinedButton(onClick = onDownload) {
                Icon(
                    Icons.Default.CloudDownload,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("下载")
            }
        }
    }
}

private fun openUrl(context: android.content.Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0L) return ""
    val mb = bytes.toDouble() / (1024 * 1024)
    return "%.2f MB".format(mb)
}
