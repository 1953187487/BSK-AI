package com.lingxi.ai.ui.update

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lingxi.ai.BuildConfig
import com.lingxi.ai.R
import com.lingxi.ai.update.DownloadStatus
import com.lingxi.ai.update.GitHubApi
import com.lingxi.ai.update.RemoteRelease
import com.lingxi.ai.update.UpdateCheckResult
import com.lingxi.ai.update.UpdateInstaller
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

    DisposableEffect(Unit) { onDispose { downloadJob?.cancel() } }

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
        onDismissRequest = { if (status !is DownloadStatus.Downloading) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Update, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(stringResource(R.string.update_center), fontWeight = FontWeight.SemiBold)
                    Text(
                        text = stringResource(R.string.update_current_line, BuildConfig.APP_VERSION, currentVersionSigned),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column {
                TabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.surface) {
                    Tab(
                        selected = tab == 0, onClick = { tab = 0 },
                        text = {
                            val label = if (result.hasUpdate) stringResource(R.string.update_latest_available)
                            else stringResource(R.string.update_latest)
                            Text(label, fontWeight = if (result.hasUpdate) FontWeight.SemiBold else FontWeight.Normal)
                        },
                        icon = { Icon(Icons.Default.Update, contentDescription = null) }
                    )
                    Tab(
                        selected = tab == 1, onClick = { tab = 1 },
                        text = { Text(stringResource(R.string.update_history, result.releases.size)) },
                        icon = { Icon(Icons.Default.History, contentDescription = null) }
                    )
                }
                Spacer(Modifier.height(10.dp))
                if (tab == 0) LatestTab(
                    target = target, result = result, status = status,
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
                    Text(stringResource(R.string.update_install_now))
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text(if (status is DownloadStatus.Downloading) stringResource(R.string.update_hide)
                    else stringResource(R.string.common_close))
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
            Text(stringResource(R.string.update_no_info), style = MaterialTheme.typography.bodyMedium)
            return
        }
        Text("${target.name} · ${target.versionName}", fontWeight = FontWeight.Medium)
        Text(
            text = stringResource(R.string.update_release_line, target.publishedAtLabel(), formatSize(target.sizeBytes)) +
                if (target.isPrerelease) stringResource(R.string.update_prerelease_suffix) else "",
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
                    text = stringResource(R.string.update_new_version),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(8.dp))
        }
        when (status) {
            is DownloadStatus.Idle -> {
                Button(onClick = onDownload, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.update_download))
                }
            }
            is DownloadStatus.Downloading -> {
                LinearProgressIndicator(
                    progress = { status.percent.coerceIn(0, 100) / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                Text("${status.percent}%", style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.common_cancel)) }
            }
            is DownloadStatus.Done -> {
                Text(stringResource(R.string.update_downloaded), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
            }
            is DownloadStatus.Failed -> {
                Text(stringResource(R.string.update_failed, status.message), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(4.dp))
                Button(onClick = onDownload, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.common_retry)) }
            }
        }
    }
}

@Composable
private fun HistoryTab(releases: List<RemoteRelease>) {
    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
        items(releases) { release ->
            Surface(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(release.tagName, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        if (release.isPrerelease) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                            ) {
                                Text(stringResource(R.string.update_prerelease), fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                    Text(release.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "${release.publishedAtLabel()} · ${formatSize(release.sizeBytes)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0L) return "?"
    val mb = bytes.toDouble() / (1024 * 1024)
    return "%.2f MB".format(mb)
}
