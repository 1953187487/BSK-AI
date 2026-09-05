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
import com.bskai.ui.legal.AgreementDecision
import com.bskai.ui.legal.AgreementDialog
import com.bskai.ui.theme.AuraTheme
import com.bskai.data.Agreements

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val app = AuraApp.of(this)
        setContent {
            val settings by app.settings.settings.collectAsState()
            AuraTheme(
                darkTheme = settings.darkTheme,
                themeStyle = settings.themeStyle
            ) {
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
    val activity = androidx.compose.ui.platform.LocalContext.current as? MainActivity
    var initialAgreed by rememberSaveable { mutableStateOf(app.settings.hasAgreed()) }
    var versionSigned by rememberSaveable { mutableStateOf(app.settings.agreementVersion()) }

    val currentVersion = com.bskai.BuildConfig.APP_VERSION
    val needsResign = initialAgreed && versionSigned != currentVersion

    var showAgreement by rememberSaveable { mutableStateOf(!initialAgreed || needsResign) }
    var showMain by rememberSaveable { mutableStateOf(initialAgreed && !needsResign) }

    if (showAgreement) {
        AgreementDialog(
            version = currentVersion,
            openSource = Agreements.openSource,
            privacy = Agreements.privacy.copy(body = Agreements.renderPrivacy(currentVersion)),
            requireBoth = true,
            onCancel = if (initialAgreed && needsResign) {
                { activity?.finish() }
            } else null,
            onConfirm = { decision: AgreementDecision ->
                app.settings.setAgreed()
                app.settings.setAgreementVersion(currentVersion)
                app.settings.markSessionAgreement(currentVersion)
                showAgreement = false
                showMain = true
            }
        )
    }
    if (showMain) {
        AuraScaffold(app = app)
    }
}
