package com.bskai

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.bskai.ui.AuraScaffold
import com.bskai.ui.welcome.WelcomeScreen
import com.bskai.ui.theme.AuraTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val app = AuraApp.of(this)
        setContent {
            val settings by app.settings.settings.collectAsState()
            AuraTheme(darkTheme = settings.darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRoot(app = app)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        navRequests.tryEmit(intent.getStringExtra("nav_target"))
    }

    companion object {
        val navRequests = kotlinx.coroutines.flow.MutableSharedFlow<String?>(
            extraBufferCapacity = 8
        )
    }
}

@androidx.compose.runtime.Composable
private fun AppRoot(app: AuraApp) {
    var agreed by rememberSaveable { mutableStateOf(app.settings.hasAgreed()) }
    if (!agreed) {
        WelcomeScreen(app = app) {
            agreed = true
        }
    } else {
        AuraScaffold(app = app)
    }
}
