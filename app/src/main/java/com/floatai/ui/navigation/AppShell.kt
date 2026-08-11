package com.floatai.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.floatai.App
import com.floatai.ui.i18n.localStrings
import com.floatai.ui.screens.atk.AtkScreen
import com.floatai.ui.screens.chat.ChatScreen
import com.floatai.ui.screens.settings.SettingsScreen
import kotlinx.coroutines.launch

@Composable
fun AppShell() {
    val strings = localStrings()
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val title = strings.app_name + " · " + AppDestination.bottomBar
        .firstOrNull { it.route == currentRoute }?.label.orEmpty()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxWidth(0.78f),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                DrawerContent(
                    currentRoute = currentRoute,
                    onSelect = { dest ->
                        scope.launch { drawerState.close() }
                        if (dest.route != currentRoute) {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            TopBar(title = title, onMenu = { scope.launch { drawerState.open() } })
            Box(modifier = Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = currentRoute,
                    transitionSpec = {
                        val direction = if (initialState != targetState)
                            AnimatedContentTransitionScope.SlideDirection.Start
                        else
                            AnimatedContentTransitionScope.SlideDirection.End
                        (slideIntoContainer(direction, tween(280)) + fadeIn(tween(280)))
                            .togetherWith(slideOutOfContainer(direction, tween(280)) + fadeOut(tween(220)))
                    },
                    label = "page-switch"
                ) { route ->
                    NavHost(
                        navController = navController,
                        startDestination = AppDestination.Chat.route,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable(AppDestination.Chat.route) { ChatScreen() }
                        composable(AppDestination.Atk.route) { AtkScreen() }
                        composable(AppDestination.Settings.route) { SettingsScreen() }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar(title: String, onMenu: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onMenu) {
            Icon(Icons.Filled.Menu, contentDescription = "菜单", tint = MaterialTheme.colorScheme.onBackground)
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
private fun DrawerContent(
    currentRoute: String?,
    onSelect: (AppDestination) -> Unit
) {
    val strings = localStrings()
    val labels = mapOf(
        AppDestination.Chat to strings.nav_ai_chat,
        AppDestination.Atk to strings.nav_atk,
        AppDestination.Settings to strings.nav_settings
    )
    Column(modifier = Modifier.fillMaxSize().padding(top = 28.dp)) {
        Text(
            text = strings.app_name,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp)
        )
        AppDestination.bottomBar.forEach { dest ->
            val selected = dest.route == currentRoute
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surface,
                        MaterialTheme.shapes.medium
                    )
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(dest.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = labels[dest] ?: dest.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.size(2.dp))
        }
    }
}
