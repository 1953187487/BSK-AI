package com.floatai.ui.flow

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.floatai.data.model.AppLanguage
import com.floatai.perm.ElevatedGrant
import com.floatai.perm.ElevatedGrantDetector
import com.floatai.ui.components.GlassCard
import com.floatai.ui.i18n.localStrings
import com.floatai.ui.theme.liquidBackdrop

/**
 * 高权限协议流：必须授予 Shizuku / Dhizuku / Root 至少一个才能进入主界面。
 */
@Composable
fun ElevatedGrantFlow(
    language: AppLanguage,
    onGranted: () -> Unit
) {
    val strings = localStrings()
    val context = LocalContext.current
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    var detected by remember { mutableStateOf(ElevatedGrantDetector.detect()) }
    var shizukuInfo by remember { mutableStateOf(false) }
    var dhizukuInfo by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(liquidBackdrop(dark))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = strings.elevated_title,
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = strings.elevated_intro,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                AssistChip(
                    onClick = { detected = ElevatedGrantDetector.detect() },
                    label = { Text(strings.elevated_recheck) },
                    leadingIcon = { Icon(Icons.Filled.CheckCircle, contentDescription = null) }
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = strings.elevated_status(ElevatedGrantDetector.describe(detected)),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (detected != ElevatedGrant.NONE) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            ElevatedOptionCard(
                title = strings.elevated_shizuku,
                desc = strings.elevated_shizuku_desc,
                granted = detected == ElevatedGrant.SHIZUKU,
                onOpen = {
                    try {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/"))
                        )
                    } catch (_: Exception) {}
                },
                onInfo = { shizukuInfo = true }
            )
            ElevatedOptionCard(
                title = strings.elevated_dhizuku,
                desc = strings.elevated_dhizuku_desc,
                granted = detected == ElevatedGrant.DHIZUKU,
                onOpen = {
                    try {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/iamr0s/Dhizuku"))
                        )
                    } catch (_: Exception) {}
                },
                onInfo = { dhizukuInfo = true }
            )
            ElevatedOptionCard(
                title = strings.elevated_root,
                desc = strings.elevated_root_desc,
                granted = detected == ElevatedGrant.ROOT,
                onOpen = { /* 用户需自行刷入 su；此处提供 Magisk 链接 */ },
                onInfo = {
                    try {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/topjohnwu/Magisk"))
                        )
                    } catch (_: Exception) {}
                }
            )

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    detected = ElevatedGrantDetector.detect()
                    if (detected != ElevatedGrant.NONE) onGranted()
                },
                enabled = detected != ElevatedGrant.NONE,
                modifier = Modifier.fillMaxWidth()
            ) { Text(strings.elevated_continue) }

            if (detected == ElevatedGrant.NONE) {
                Text(
                    text = strings.elevated_required,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    if (shizukuInfo) {
        AlertDialog(
            onDismissRequest = { shizukuInfo = false },
            title = { Text(strings.shizuku_intro_title) },
            text = { Text(strings.shizuku_intro_body, fontSize = 13.sp) },
            confirmButton = {
                TextButton(onClick = { shizukuInfo = false }) { Text("✕") }
            }
        )
    }
    if (dhizukuInfo) {
        AlertDialog(
            onDismissRequest = { dhizukuInfo = false },
            title = { Text(strings.dhizuku_intro_title) },
            text = { Text(strings.dhizuku_intro_body, fontSize = 13.sp) },
            confirmButton = {
                TextButton(onClick = { dhizukuInfo = false }) { Text("✕") }
            }
        )
    }
}

@Composable
private fun ElevatedOptionCard(
    title: String,
    desc: String,
    granted: Boolean,
    onOpen: () -> Unit,
    onInfo: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (granted) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onInfo) { Text("?") }
            Button(onClick = onOpen) { Text("→") }
        }
    }
}

private fun androidx.compose.ui.graphics.Color.luminance(): Float {
    return 0.2126f * red + 0.7152f * green + 0.0722f * blue
}
