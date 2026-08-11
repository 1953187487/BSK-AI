package com.floatai.ui.flow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.floatai.data.model.AppLanguage
import com.floatai.ui.i18n.localStrings
import com.floatai.ui.theme.liquidBackdrop
import com.floatai.ui.theme.relativeLuminance

@Composable
fun LanguageFlow(current: AppLanguage, onSelect: (AppLanguage) -> Unit) {
    val strings = localStrings()
    val dark = MaterialTheme.colorScheme.background.relativeLuminance() < 0.5f
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(liquidBackdrop(dark)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Filled.Language,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = strings.language_choose_title,
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = strings.language_choose_body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = { onSelect(AppLanguage.ZH) },
                modifier = Modifier.fillMaxWidth()
            ) { Text(strings.language_zh) }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { onSelect(AppLanguage.EN) },
                modifier = Modifier.fillMaxWidth()
            ) { Text(strings.language_en) }
        }
    }
}
