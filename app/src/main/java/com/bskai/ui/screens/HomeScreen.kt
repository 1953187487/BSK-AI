package com.bskai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bskai.ui.utils.FlowingText
import com.bskai.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: MainViewModel) {
    var expanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AURA 2.0", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { expanded = true }) { 
                        Icon(Icons.Default.MoreVert, "更多") 
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(text = { Text("设置") }, onClick = { /* Navigate */ })
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { DeviceInfoCard() }
            item { PermissionStatusCard(viewModel) }
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = { viewModel.startListening() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C5CE7))
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Mic, contentDescription = null, tint = Color.White)
                            Text("按住说话", color = Color.White)
                        }
                    }
                }
            }
            item {
                val response by viewModel.currentResponse.collectAsState()
                if (response.isNotEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        Box(modifier = Modifier.padding(16.dp)) { FlowingText(response) }
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceInfoCard() {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("设备信息", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("设备型号: ${android.os.Build.MODEL}", style = MaterialTheme.typography.bodyMedium)
            Text("系统版本: Android ${android.os.Build.VERSION.RELEASE}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun PermissionStatusCard(viewModel: MainViewModel) {
    val required = viewModel.permissionManager.requiredPermissions
    
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("权限状态", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            viewModel.permissionManager.requiredPermissions.forEach { perm ->
                val granted = viewModel.permissionManager.hasPermission(perm.permission)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(perm.permission, style = MaterialTheme.typography.bodyMedium)
                    Icon(
                        imageVector = if (granted) Icons.Default.Check else Icons.Default.Block,
                        contentDescription = null,
                        tint = if (granted) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                }
            }
        }
    }
}
