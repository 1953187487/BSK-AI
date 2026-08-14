package com.floatai.ide

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.material3.ExperimentalMaterial3Api
import com.floatai.ui.components.GlassCard
import com.floatai.BuildConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IDE2Home() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selected by remember { mutableStateOf("项目") }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(Modifier.padding(16.dp)) {
                    Text("Android Dev Toolkit", style = MaterialTheme.typography.titleLarge)
                    Text("v${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.height(16.dp))
                    listOf("项目", "设置").forEach { item ->
                        Text(
                            item,
                            Modifier.padding(vertical = 12.dp),
                            color = if (selected == item) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Android Dev Toolkit 2.0") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = null)
                        }
                    }
                )
            }
        ) { padding ->
            Column(Modifier.padding(padding).padding(16.dp)) {
                GlassCard {
                    Text("C++ 编辑器主页", style = MaterialTheme.typography.headlineMedium)
                }
                Spacer(Modifier.height(24.dp))
                ThreeBalls()
            }
        }
    }
}

@Composable
fun ThreeBalls() {
    Column {
        Text("创建项目", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Ball("选择文件夹")
            Ball("配置服务商")
            Ball("检查依赖")
        }
    }
}

@Composable
fun Ball(label: String) {
    GlassCard(modifier = Modifier.size(140.dp)) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), androidx.compose.foundation.shape.CircleShape)
            )
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}
