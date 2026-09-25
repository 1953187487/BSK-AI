package com.bskai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bskai.AuraApp
import com.bskai.BuildConfig
import com.bskai.R
import com.bskai.ui.chat.ChatScreen
import com.bskai.ui.glass.GlassBottomNav
import com.bskai.ui.glass.GlassSegmented
import com.bskai.ui.ide.IdeScreen
import com.bskai.ui.settings.SettingsScreen
import com.bskai.ui.terminal.TerminalScreen

@Composable
fun AuraScaffold(app: AuraApp) {
    val snackbarHostState = remember { SnackbarHostState() }
    var currentTab by rememberSaveable { mutableIntStateOf(0) }
    var devTab by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        com.bskai.MainActivity.navRequests.collect { target ->
            when (target) {
                "settings", "account" -> currentTab = 1
                "terminal" -> { currentTab = 0; devTab = 1 }
                "ide" -> { currentTab = 0; devTab = 2 }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(liquidBackdrop())
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 14.dp)
        ) {
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AURA",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "v${BuildConfig.APP_VERSION}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
                )
                Spacer(Modifier.weight(1f))
            }

            if (currentTab == 0) {
                Spacer(Modifier.height(10.dp))
                GlassSegmented(
                    options = listOf(
                        stringResource(R.string.tab_dialogs),
                        stringResource(R.string.tab_terminal),
                        stringResource(R.string.tab_ide)
                    ),
                    selected = devTab,
                    onSelect = { devTab = it },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            Spacer(Modifier.height(10.dp))

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (currentTab) {
                    0 -> when (devTab) {
                        0 -> ChatScreen(app = app, snackbarHostState = snackbarHostState)
                        1 -> TerminalScreen(engine = app.terminal, shizuku = app.shizuku, dhizuku = app.dhizuku)
                        else -> IdeScreen(app = app)
                    }
                    else -> SettingsScreen(app = app)
                }
            }

            Spacer(Modifier.height(10.dp))

            GlassBottomNav(
                selected = currentTab,
                onSelect = { currentTab = it },
                modifier = Modifier.navigationBarsPadding()
            )

            Spacer(Modifier.height(12.dp))
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp)
        ) {
            SnackbarHost(hostState = snackbarHostState)
        }
    }
}

@Composable
private fun liquidBackdrop(): Brush {
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    return if (dark) {
        Brush.verticalGradient(
            listOf(
                Color(0xFF0B1020),
                Color(0xFF141A33),
                Color(0xFF0D1226),
                Color(0xFF091022)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xFFEAF2FF),
                Color(0xFFE3ECFF),
                Color(0xFFF2EFFF),
                Color(0xFFEDF6FF)
            )
        )
    }
}
