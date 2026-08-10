package com.floatai.ui

import android.content.Context
import android.content.Intent
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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GlassTheme {
                MainScreen(getSharedPreferences("float_ai_prefs", Context.MODE_PRIVATE))
            }
        }
    }
}

@Composable
fun MainScreen(prefs: android.content.SharedPreferences) {
    val context = LocalContext.current
    var currentTab by rememberSaveable { mutableStateOf(0) }
    var showProtocol by remember {
        mutableStateOf(!prefs.getBoolean("protocol_agreed", false))
    }
    val tabs = listOf("AI \u5BF9\u8BDD", "API \u914D\u7F6E", "Settings")
    val icons = listOf(Icons.Filled.Home, Icons.Filled.Chat, Icons.Filled.Settings)

    if (showProtocol) {
        // Two-step protocol on first install
        StepOneProtocol(
            onAgree = {
                prefs.edit().putBoolean("protocol_agreed", true).apply()
                showProtocol = false
            },
            onExit = {
                android.os.Process.killProcess(android.os.Process.myPid())
            }
        )
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF1A1330)
            ) {
                tabs.forEachIndexed { index, title ->
                    NavigationBarItem(
                        selected = currentTab == index,
                        onClick = { currentTab = index },
                        icon = {
                            Icon(
                                icons[index],
                                contentDescription = title,
                                tint = Color.White
                            )
                        },
                        label = {
                            Text(title, color = Color.White.copy(0.8f))
                        }
                    )
                }
            }
        }
    ) { padding ->
        val modifier = Modifier.fillMaxSize().padding(padding)
        when (currentTab) {
            0 -> ChatScreen(modifier)
            1 -> ApiScreen(modifier)
            2 -> SettingsScreen(modifier)
        }
    }
}

@Composable
private fun StepOneProtocol(
    onAgree: () -> Unit,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    var step by remember { mutableStateOf(1) }

    when (step) {
        1 -> {
            AlertDialog(
                onDismissRequest = { },
                title = { Text("\u7528\u6237\u987B\u77E5\u534F\u8BAE", color = Color.White) },
                text = {
                    Text(
                        context.getString(R.string.user_notice),
                        color = Color.White.copy(0.85f),
                        fontSize = 13.sp,
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    )
                },
                confirmButton = {
                    TextButton(onClick = { step = 2 }) { Text("\u4E0B\u4E00\u6B65") }
                },
                dismissButton = {
                    TextButton(onClick = onExit) { Text("\u4E0D\u540C\u610F\u9000\u51FA") }
                }
            )
        }
        2 -> {
            AlertDialog(
                onDismissRequest = { },
                title = { Text("\u6743\u9650\u534F\u8BAE", color = Color.White) },
                text = {
                    Text(
                        context.getString(R.string.permission_notice),
                        color = Color.White.copy(0.85f),
                        fontSize = 13.sp,
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    )
                },
                confirmButton = {
                    TextButton(onClick = onAgree) { Text("\u540C\u610F\u5E76\u7EE7\u7EED") }
                },
                dismissButton = {
                    TextButton(onClick = onExit) { Text("\u4E0D\u540C\u610F\u9000\u51FA") }
                }
            )
        }
    }
}
