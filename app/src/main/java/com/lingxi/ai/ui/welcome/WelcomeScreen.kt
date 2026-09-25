package com.lingxi.ai.ui.welcome

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lingxi.ai.BuildConfig
import com.lingxi.ai.R
import com.lingxi.ai.ui.glass.GlassButton
import com.lingxi.ai.ui.glass.GlassPanel
import com.lingxi.ai.ui.glass.rememberGlassColors

/**
 * Version-update intro for returning users: shows the highlights of the current
 * version and a single continue button, with no mandatory checks.
 *
 * Layout contract: fixed header + weighted scrollable body + fixed footer, so the
 * button can never be covered by the feature list.
 */
@Composable
fun WhatsNewScreen(onDone: () -> Unit) {
    val glass = rememberGlassColors()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.whatsnew_updated, BuildConfig.APP_VERSION),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clipToBounds()
            ) {
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    Text(
                        stringResource(R.string.whatsnew_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(12.dp))

                    WhatsNewItem(R.string.whatsnew_item_brand, R.string.whatsnew_item_brand_desc)
                    WhatsNewItem(R.string.whatsnew_item_privilege, R.string.whatsnew_item_privilege_desc)
                    WhatsNewItem(R.string.whatsnew_item_ai, R.string.whatsnew_item_ai_desc)
                    WhatsNewItem(R.string.whatsnew_item_slash, R.string.whatsnew_item_slash_desc)
                    WhatsNewItem(R.string.whatsnew_item_stability, R.string.whatsnew_item_stability_desc)
                    WhatsNewItem(R.string.whatsnew_item_i18n, R.string.whatsnew_item_i18n_desc)
                }
            }

            GlassButton(
                text = stringResource(R.string.whatsnew_start),
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                onClick = onDone
            )
        }
    }
}

@Composable
private fun WhatsNewItem(@StringRes titleRes: Int, @StringRes descriptionRes: Int) {
    val glass = rememberGlassColors()
    GlassPanel(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(14.dp)) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = glass.accent,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(titleRes),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = glass.content
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    stringResource(descriptionRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = glass.contentMuted
                )
            }
        }
    }
    Spacer(Modifier.height(10.dp))
}
