package com.floatai

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.floatai.data.model.AppSettings
import com.floatai.ui.navigation.AppDestination
import com.floatai.ui.screens.api.ApiScreen
import com.floatai.ui.screens.chat.ChatScreen
import com.floatai.ui.screens.settings.SettingsScreen
import com.floatai.ui.theme.FloatAITheme
import com.floatai.ui.theme.accentColorByName

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as App
        setContent {
            val settings by app.settingsRepository.settings.collectAsStateWithLifecycle()
            FloatAITheme(
                darkTheme = settings.darkTheme,
                dynamicColor = settings.dynamicColor,
                accentColor = accentColorByName(settings.accentColor)
            ) {
                MainScreen(app)
            }
        }
    }
}

@Composable
fun MainScreen(app: App) {
    val navController = rememberNavController()
    val settings by app.settingsRepository.settings.collectAsStateWithLifecycle()

    if (!settings.protocolAgreed) {
        StepOneProtocol(
            onAgree = {
                app.settingsRepository.updateSettings { it.copy(protocolAgreed = true) }
            },
            onExit = {
                android.os.Process.killProcess(android.os.Process.myPid())
            }
        )
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                AppDestination.bottomBar.forEach { destination ->
                    val backStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = backStackEntry?.destination?.route
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                destination.icon,
                                contentDescription = destination.label
                            )
                        },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Chat.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(AppDestination.Chat.route) { ChatScreen() }
            composable(AppDestination.Api.route) { ApiScreen() }
            composable(AppDestination.Settings.route) { SettingsScreen() }
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
                title = { Text("用户须知") },
                text = {
                    Text(
                        context.getString(R.string.user_notice),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                },
                confirmButton = {
                    TextButton(onClick = { step = 2 }) { Text("下一步") }
                },
                dismissButton = {
                    TextButton(onClick = onExit) { Text("不同意退出") }
                }
            )
        }
        2 -> {
            AlertDialog(
                onDismissRequest = { },
                title = { Text("权限协议") },
                text = {
                    Text(
                        context.getString(R.string.permission_notice),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                },
                confirmButton = {
                    TextButton(onClick = onAgree) { Text("同意并继续") }
                },
                dismissButton = {
                    TextButton(onClick = onExit) { Text("不同意退出") }
                }
            )
        }
    }
}
