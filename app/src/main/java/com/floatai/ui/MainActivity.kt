package com.floatai.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val initialTab = intent?.getIntExtra("tab", 0) ?: 0
        setContent {
            GlassTheme {
                MainScreen(
                    prefs = getSharedPreferences("float_ai_prefs", Context.MODE_PRIVATE),
                    initialTab = initialTab
                )
            }
        }
    }
}

@Composable
fun MainScreen(prefs: android.content.SharedPreferences, initialTab: Int = 0) {
    val context = LocalContext.current
    var currentTab by rememberSaveable { mutableStateOf(initialTab) }
    var showProtocol by remember {
        mutableStateOf(!prefs.getBoolean("protocol_agreed", false))
    }
    var updateNotice by remember { mutableStateOf<UpdateNotice?>(null) }
    var protocolStep by remember { mutableStateOf(1) }
    var autoChecked by remember { mutableStateOf(false) }

    // 协议同意后（或已同意直接进入）自动检查更新一次
    LaunchedEffect(showProtocol) {
        if (!showProtocol && !autoChecked) {
            autoChecked = true
            val info = UpdateChecker.checkLatest("v0.2")
            if (info.isNewer && info.latestTag.isNotEmpty()) {
                updateNotice = UpdateNotice(info.latestTag, info.changelog)
            }
        }
    }

    if (showProtocol) {
        if (protocolStep == 1) {
            // 第一步：用户须知协议
            AlertDialog(
                onDismissRequest = { },
                title = { Text("用户须知协议", color = Color.White) },
                text = {
                    Text(
                        context.getString(R.string.user_notice),
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    )
                },
                confirmButton = {
                    TextButton(onClick = { protocolStep = 2 }) { Text("下一步") }
                },
                dismissButton = {
                    TextButton(onClick = { exitApp() }) { Text("不同意退出") }
                }
            )
        } else {
            // 第二步：权限协议
            AlertDialog(
                onDismissRequest = { },
                title = { Text("权限协议", color = Color.White) },
                text = {
                    Text(
                        context.getString(R.string.permission_notice),
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        prefs.edit().putBoolean("protocol_agreed", true).apply()
                        showProtocol = false
                    }) { Text("同意并继续") }
                },
                dismissButton = {
                    TextButton(onClick = { exitApp() }) { Text("不同意退出") }
                }
            )
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF1A1330)) {
                val tabs = listOf(
                    "AI 聊天" to Icons.Filled.Chat,
                    "API 配置" to Icons.Filled.Tune,
                    "设置" to Icons.Filled.Settings
                )
                tabs.forEachIndexed { index, (title, icon) ->
                    NavigationBarItem(
                        selected = currentTab == index,
                        onClick = { currentTab = index },
                        icon = { Icon(icon, contentDescription = title, tint = Color.White) },
                        label = { Text(title, color = Color.White.copy(alpha = 0.8f)) }
                    )
                }
            }
        }
    ) { innerPadding ->
        val mod = Modifier.fillMaxSize().padding(innerPadding)
        when (currentTab) {
            0 -> ChatScreen(mod)
            1 -> ApiScreen(mod)
            2 -> SettingsScreen(mod)
        }
    }

    // 强制更新公告（内容来自 GitHub Release body）
    updateNotice?.let { notice ->
        AlertDialog(
            onDismissRequest = { updateNotice = null },
            title = { Text("发现新版本 ${notice.tag}", color = Color(0xFFFF6B6B)) },
            text = {
                Text(
                    notice.changelog.ifEmpty { "请前往 GitHub Releases 下载更新" },
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.repo_releases_url)))
                    )
                    updateNotice = null
                }) { Text("前往下载") }
            },
            dismissButton = {
                TextButton(onClick = { updateNotice = null }) { Text("稍后") }
            }
        )
    }
}

private fun exitApp() {
    android.os.Process.killProcess(android.os.Process.myPid())
}
