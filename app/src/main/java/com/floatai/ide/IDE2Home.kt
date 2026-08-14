package com.floatai.ide

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IDE2Home() {
    var drawerOpen by remember { mutableStateOf(false) }
    ModalNavigationDrawer(
        drawerContent = {
            ModalDrawerSheet {
                Text("项目", Modifier.padding(16.dp))
                Text("设置", Modifier.padding(16.dp))
            }
        },
        drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Android Dev Toolkit 2.0") })
            }
        ) { padding ->
            Column(Modifier.padding(padding).padding(16.dp)) {
                Text("C++ 编辑器主页", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(24.dp))
                ThreeBalls()
            }
        }
    }
}

@Composable
fun ThreeBalls() {
    Column {
        Text("创建项目")
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
    Card(modifier = Modifier.size(120.dp)) {
        Box(contentAlignment = androidx.compose.ui.Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(label)
        }
    }
}
