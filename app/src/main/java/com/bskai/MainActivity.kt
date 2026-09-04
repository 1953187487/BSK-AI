package com.bskai

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bskai.ui.screens.AppRoot
import com.bskai.ui.agreements.AgreementOverlay
import com.bskai.ui.theme.AuraTheme
import com.bskai.ui.viewmodel.MainViewModel
import androidx.compose.runtime.*

class MainActivity : ComponentActivity() {
    private var showAgreements by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Check if agreements were already accepted
        val prefs = getSharedPreferences("aura_prefs", MODE_PRIVATE)
        showAgreements = !prefs.getBoolean("agreements_accepted", false)

        setContent {
            val app = application as AuraApp
            AuraTheme(darkTheme = true) {
                if (showAgreements) {
                    AgreementOverlay(
                        onAccept = {
                            prefs.edit().putBoolean("agreements_accepted", true).apply()
                            showAgreements = false
                        },
                        onDecline = {
                            finish()
                        }
                    )
                } else {
                    val viewModel: MainViewModel = viewModel(factory = MainViewModel.Companion.Factory)
                    AppRoot(
                        app = app,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}
