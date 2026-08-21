package com.bskai.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bskai.BskApp
import com.bskai.ui.screens.agent.AgentScreen
import com.bskai.ui.screens.agent.AgentViewModel
import com.bskai.ui.screens.settings.SettingsScreen
import com.bskai.ui.screens.toolchain.ToolboxScreen
import com.bskai.ui.screens.terminal.TerminalScreen
import kotlinx.coroutines.launch

private data class NavItem(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val route: String
)

private val navItems = listOf(
    NavItem("AI", Icons.Outlined.Chat, "agent"),
    NavItem("Toolbox", Icons.Outlined.Build, "toolbox"),
    NavItem("Terminal", Icons.Outlined.Terminal, "terminal"),
    NavItem("Settings", Icons.Outlined.Settings, "settings")
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(
    app: BskApp,
    agentViewModel: AgentViewModel
) {
    var currentRoute by rememberSaveable { mutableStateOf("agent") }
    val pagerState = rememberPagerState(initialPage = 0) { navItems.size }
    val scope = rememberCoroutineScope()

    LaunchedEffect(currentRoute) {
        val idx = navItems.indexOfFirst { it.route == currentRoute }
        if (idx >= 0) pagerState.scrollToPage(idx)
    }
    LaunchedEffect(pagerState.currentPage) {
        val route = navItems[pagerState.currentPage].route
        if (route != currentRoute) currentRoute = route
    }

    Scaffold(
        bottomBar = {
            LiquidNavBar(
                items = navItems,
                selectedIndex = pagerState.currentPage,
                onTab = { scope.launch { pagerState.scrollToPage(it); currentRoute = navItems[it].route } }
            )
        }
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> AgentScreen(agentViewModel)
                    1 -> ToolboxScreen(app)
                    2 -> TerminalScreen(app)
                    3 -> SettingsScreen(app)
                }
            }
            if (pagerState.isScrollInProgress) {
                CurrentRouteBadge(route = navItems[pagerState.currentPage].label)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LiquidNavBar(
    items: List<NavItem>,
    selectedIndex: Int,
    onTab: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .padding(horizontal = 8.dp),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 24.dp, bottomEnd = 24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSystemInDarkTheme()) Color(0x9912101E) else Color(0xCCFFFFFF)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                items.forEachIndexed { index, item ->
                    LiquidNavItem(
                        item = item,
                        selected = index == selectedIndex,
                        onClick = { onTab(index) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LiquidNavItem(
    item: NavItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var pressed by remember { mutableStateOf(false) }
    val scale = remember { Animatable(1f) }

    LaunchedEffect(pressed) {
        if (pressed) scope.launch { scale.animateTo(0.85f, tween(100)) }
        else scope.launch { scale.animateTo(1f, tween(200)) }
    }

    Box(
        modifier = Modifier
            .size(52.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { pressed = true },
                    onDragCancel = { pressed = false },
                    onDragEnd = { pressed = false },
                    onHorizontalDrag = { _, _ -> }
                )
            }
            .then(if (selected) Modifier.background(
                Brush.radialGradient(
                    colors = listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), Color.Transparent),
                    center = Offset(0.5f, 0.5f),
                    radius = 160f
                ),
                shape = CircleShape
            ) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        if (selected) {
            Box(
                modifier = Modifier
                    .size(3.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun CurrentRouteBadge(route: String) {
    Box(
        modifier = Modifier
            .padding(end = 12.dp, top = 12.dp)
            .background(
                Brush.linearGradient(listOf(Color(0x60818CF8), Color(0x20818CF8))),
                RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = route,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            textAlign = TextAlign.Center
        )
    }
}
