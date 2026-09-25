package com.bskai.ui.welcome

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bskai.BuildConfig
import com.bskai.R
import com.bskai.ui.glass.GlassButton
import com.bskai.ui.glass.GlassPanel
import com.bskai.ui.glass.rememberGlassColors

/**
 * 已进入用户的版本更新引导：展示当前版本重点变化，一键继续，无任何强制校验。
 */
@Composable
fun WhatsNewScreen(onDone: () -> Unit) {
    val glass = rememberGlassColors()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(24.dp))
            Text(
                text = "AURA",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.whatsnew_updated, BuildConfig.APP_VERSION),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(18.dp))

            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Text(
                    stringResource(R.string.whatsnew_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(12.dp))

                WhatsNewItem(R.string.whatsnew_item_nav, R.string.whatsnew_item_nav_desc)
                WhatsNewItem(R.string.whatsnew_item_glass, R.string.whatsnew_item_glass_desc)
                WhatsNewItem(R.string.whatsnew_item_privilege, R.string.whatsnew_item_privilege_desc)
                WhatsNewItem(R.string.whatsnew_item_i18n, R.string.whatsnew_item_i18n_desc)
                WhatsNewItem(R.string.whatsnew_item_models, R.string.whatsnew_item_models_desc)
                WhatsNewItem(R.string.whatsnew_item_deps, R.string.whatsnew_item_deps_desc)
                WhatsNewItem(R.string.whatsnew_item_video, R.string.whatsnew_item_video_desc)
            }

            GlassButton(
                text = stringResource(R.string.whatsnew_start),
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
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
            Column {
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
