package com.floatai.ui.shell

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Adb
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.floatai.BuildConfig
import com.floatai.ui.i18n.localStrings
import com.floatai.ui.screens.build.BuildScreen
import com.floatai.ui.screens.chat.ChatScreen
import com.floatai.ui.screens.packagehub.PackageHubScreen
import com.floatai.ui.screens.settings.SettingsScreen
import kotlinx.coroutines.launch

/** 主导航目的地 v1.0.6-rc.2：新增 ATK 反编译。 */
/**
 * 控制底部导航栏显示的全局开关（放大模式时隐藏）。
 * v1.0.6-rc.2 新增：AI 聊天界面"放大模式"按钮用此切换。
 */
object UiState {
    val bottomBarVisible = androidx.compose.runtime.mutableStateOf(true)
}
val LocalBottomBarVisible = androidx.compose.runtime.compositionLocalOf { true }

enum class ShellDestination(
    val route: String,
    val labelKey: String,
    val icon: ImageVector
) {
    CHAT("chat", "nav_ai_chat", Icons.AutoMirrored.Filled.Chat),
    BUILD("build", "nav_build", Icons.Filled.Code),
    PACKAGE_HUB("package_hub", "nav_package_hub", Icons.Filled.Extension),
    ATK("atk", "nav_atk", Icons.Filled.Adb),
    SETTINGS("settings", "nav_settings", Icons.Filled.Settings);

    companion object {
        val bottomBar = listOf(CHAT, BUILD)
        val drawer = listOf(CHAT, BUILD, PACKAGE_HUB, ATK, SETTINGS)
    }
}

@Composable
fun AppShell() {
    val strings = localStrings()
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val labels = remember(strings) {
        mapOf(
            ShellDestination.CHAT to strings.nav_ai_chat,
            ShellDestination.BUILD to strings.nav_build,
            ShellDestination.PACKAGE_HUB to strings.nav_package_hub,
            ShellDestination.SETTINGS to strings.nav_settings
        )
    }
    val currentTitle = labels[ShellDestination.entries.firstOrNull { it.route == currentRoute }]
        ?: strings.app_name

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxWidth(0.82f).fillMaxHeight(),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                DrawerContent(
                    currentRoute = currentRoute,
                    labels = labels,
                    onSelect = { dest ->
                        scope.launch { drawerState.close() }
                        if (dest.route != currentRoute) navigateTo(dest.route)
                    }
                )
            }
        }
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopBar(
                    title = "$currentTitle · FloatAI v${BuildConfig.VERSION_NAME}",
                    onMenu = { scope.launch { drawerState.open() } }
                )
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    AnimatedContent(
                        targetState = currentRoute,
                        transitionSpec = {
                            val direction = if (initialState != targetState)
                                AnimatedContentTransitionScope.SlideDirection.Start
                            else
                                AnimatedContentTransitionScope.SlideDirection.End
                            (slideIntoContainer(direction, tween(220)) + fadeIn(tween(220)))
                                .togetherWith(slideOutOfContainer(direction, tween(180)) + fadeOut(tween(160)))
                        },
                        label = "page-switch"
                    ) { _ ->
                        NavHost(
                            navController = navController,
                            startDestination = ShellDestination.CHAT.route,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            composable(ShellDestination.CHAT.route) {
                                com.floatai.ui.screens.chat.ChatScreenWithBottomBarToggle()
                            }
                            composable(ShellDestination.BUILD.route) { BuildScreen() }
                            composable(ShellDestination.PACKAGE_HUB.route) { PackageHubScreen() }
                            composable(ShellDestination.ATK.route) { com.floatai.ui.screens.atk.AtkScreen(
                                onBack = { navController.popBackStack() },
                                onSendToChat = { text ->
                                    // 切换到 chat tab 并把文本作为初始输入
                                    navController.navigate(ShellDestination.CHAT.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            ) }
                            composable(ShellDestination.SETTINGS.route) {
                                SettingsScreen(
                                    onOpenAbout = {
                                        navController.navigate("about") { launchSingleTop = true }
                                    },
                                    onOpenPackageHub = { navigateTo(ShellDestination.PACKAGE_HUB.route) }
                                )
                            }
                            composable("about") { com.floatai.ui.screens.about.AboutScreen() }
                        }
                    }
                }
                if (UiState.bottomBarVisible.value) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 3.dp
                    ) {
                        ShellDestination.bottomBar.forEach { dest ->
                            val selected = dest.route == currentRoute
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (dest.route != currentRoute) navigateTo(dest.route)
                                },
                                icon = {
                                    Icon(dest.icon, contentDescription = labels[dest])
                                },
                                label = { Text(labels[dest] ?: dest.route) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar(title: String, onMenu: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onMenu) {
                Icon(
                    Icons.Filled.Menu,
                    contentDescription = "打开菜单",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DrawerContent(
    currentRoute: String?,
    labels: Map<ShellDestination, String>,
    onSelect: (ShellDestination) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Apps,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    "FloatAI",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "v${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(8.dp))
        ShellDestination.drawer.forEach { dest ->
            val selected = dest.route == currentRoute
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp)
                    .clickable { onSelect(dest) }
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surface,
                        MaterialTheme.shapes.medium
                    )
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    dest.icon,
                    contentDescription = null,
                    tint = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.width(14.dp))
                Text(
                    text = labels[dest] ?: dest.route,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
