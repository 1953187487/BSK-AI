package com.floatai.ui

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GlassTheme {
                MainScreen(
                    prefs = getSharedPreferences("float_ai_prefs", Context.MODE_PRIVATE)
                )
            }
        }
    }
}

@Composable
fun MainScreen(prefs: SharedPreferences) {
    var currentTab by rememberSaveable { mutableStateOf(0) }
    var showProtocol by rememberSaveable {
        mutableStateOf(!prefs.getBoolean("protocol_agreed", false))
    }

    if (showProtocol) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("用户须知与开源协议") },
            text = {
                Text(
                    "【用户须知】\n" +
                    "1. FloatAI 支持 Android 8~14 (API 26~34)。\n" +
                    "2. 悬浮窗功能需要授权\"显示在其他应用上层\"。\n" +
                    "3. 进程查看需要 Shizuku / Dhizuku 授权。\n" +
                    "4. AI 功能需自行配置服务商 API 地址与密钥。\n" +
                    "5. 请遵守当地法律法规，合法使用。\n\n" +
                    "【开源协议】\n" +
                    "本项目基于 MIT License 开源。\n" +
                    "仓库: https://github.com/1953187487/FloatAI"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    prefs.edit().putBoolean("protocol_agreed", true).apply()
                    showProtocol = false
                }) { Text("同意并继续") }
            },
            dismissButton = {
                TextButton(onClick = {
                    prefs.edit().putBoolean("protocol_agreed", false).apply()
                    android.os.Process.killProcess(android.os.Process.myPid())
                }) { Text("不同意退出") }
            }
        )
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                listOf(
                    "AI 聊天" to Icons.Filled.Chat,
                    "API 配置" to Icons.Filled.Tune,
                    "设置" to Icons.Filled.Settings
                ).forEachIndexed { index, (title, icon) ->
                    NavigationBarItem(
                        selected = currentTab == index,
                        onClick = { currentTab = index },
                        icon = { Icon(icon, contentDescription = title) },
                        label = { Text(title) }
                    )
                }
            }
        }
    ) { innerPadding ->
        val mod = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        when (currentTab) {
            0 -> ChatScreen(mod)
            1 -> ApiScreen(mod)
            2 -> SettingsScreen(mod)
        }
    }
}
