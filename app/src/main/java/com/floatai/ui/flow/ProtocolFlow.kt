package com.floatai.ui.flow

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.floatai.data.model.AppLanguage
import com.floatai.ui.i18n.localStrings
import com.floatai.ui.theme.liquidBackdrop
import com.floatai.ui.theme.relativeLuminance

/**
 * 双步协议流：
 *  1. 用户须知
 *  2. 权限协议
 *
 * 每一步均允许切换语言；切换后所有文案立即跟随。
 */
@Composable
fun ProtocolFlow(
    language: AppLanguage,
    onAgree: () -> Unit,
    onLanguage: (AppLanguage) -> Unit
) {
    val strings = localStrings()
    val dark = MaterialTheme.colorScheme.background.relativeLuminance() < 0.5f
    var step by remember { mutableStateOf(1) }
    var showLanguage by remember { mutableStateOf(false) }

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
            verticalArrangement = ArrangementTop
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = { showLanguage = true },
                    label = { Text(if (language == AppLanguage.ZH) strings.language_zh else strings.language_en) },
                    leadingIcon = { Icon(Icons.Filled.Language, contentDescription = null) }
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = if (step == 1) strings.user_notice_title else strings.permission_notice_title,
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        RoundedCornerShape(18.dp)
                    )
                    .padding(18.dp)
            ) {
                Text(
                    text = if (step == 1) strings.user_notice_body else strings.permission_notice_body,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    if (step == 1) step = 2 else onAgree()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (step == 1) strings.permission_notice_title else strings.elevated_continue)
            }
            if (step == 2) {
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = { step = 1 },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("← ${strings.user_notice_title}") }
            }
        }
    }

    if (showLanguage) {
        AlertDialog(
            onDismissRequest = { showLanguage = false },
            title = { Text(strings.language_choose_title) },
            text = {
                Column {
                    TextButton(onClick = {
                        onLanguage(AppLanguage.ZH); showLanguage = false
                    }) { Text(strings.language_zh) }
                    TextButton(onClick = {
                        onLanguage(AppLanguage.EN); showLanguage = false
                    }) { Text(strings.language_en) }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguage = false }) { Text("✕") }
            }
        )
    }
}

private val ArrangementTop = androidx.compose.foundation.layout.Arrangement.Top
