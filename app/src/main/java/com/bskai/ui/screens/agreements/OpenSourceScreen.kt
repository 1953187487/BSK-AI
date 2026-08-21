package com.bskai.ui.screens.agreements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bskai.ui.theme.BskGlassCardDefaults

data class OSSDependency(
    val name: String,
    val version: String,
    val license: String,
    val url: String
)

private val ossDependencies = listOf(
    OSSDependency("AndroidX Core KTX", "1.12.0", "Apache 2.0", "https://developer.android.com/jetpack/androidx"),
    OSSDependency("AndroidX Compose BOM", "2024.02.00", "Apache 2.0", "https://developer.android.com/jetpack/compose"),
    OSSDependency("Material Design 3", "1.11.0", "Apache 2.0", "https://material.io/components"),
    OSSDependency("OkHttp", "4.12.0", "Apache 2.0", "https://square.github.io/okhttp/"),
    OSSDependency("Kotlin Coroutines", "1.7.3", "Apache 2.0", "https://kotlinlang.org/docs/coroutines-overview.html"),
    OSSDependency("JSON", "20231013", "JSON.org", "https://json.org/"),
    OSSDependency("AndroidX DataStore", "1.0.0", "Apache 2.0", "https://developer.android.com/jetpack/androidx/data-store"),
    OSSDependency("AndroidX Navigation", "2.7.7", "Apache 2.0", "https://developer.android.com/jetpack/androidx/navigation"),
    OSSDependency("AndroidX Lifecycle", "2.7.0", "Apache 2.0", "https://developer.android.com/jetpack/androidx/lifecycle")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenSourceScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("开源授权", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            Text(
                "本应用使用了以下开源组件，感谢所有贡献者：",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            LazyColumn(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                items(ossDependencies) { dep ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = BskGlassCardDefaults.cardColors()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                dep.name,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "版本：${dep.version}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "授权：${dep.license}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(
                "所有开源组件均按照其各自授权协议使用，本应用在分发时均已遵守相关授权要求。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
