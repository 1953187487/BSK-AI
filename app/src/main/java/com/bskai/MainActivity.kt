package com.bskai

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.*
import com.bskai.ui.screens.AppRoot
import com.bskai.ui.agreements.AgreementOverlay
import com.bskai.ui.theme.AuraTheme
import com.bskai.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    @Composable
    fun AppContent() {
        val prefs = remember { getSharedPreferences("aura_prefs", MODE_PRIVATE) }
        var agreementsAccepted by remember { mutableStateOf(prefs.getBoolean("agreements_accepted", false)) }

        val app = application as AuraApp

        AuraTheme(darkTheme = true) {
            if (!agreementsAccepted) {
                AgreementOverlay(
                    onAccept = {
                        prefs.edit().putBoolean("agreements_accepted", true).apply()
                        agreementsAccepted = true
                    },
                    onDecline = {
                        finish()
                    }
                )
            } else {
                val viewModel: MainViewModel = viewModel(factory = MainViewModel.Companion.Factory)
                AppRoot(app = app, viewModel = viewModel)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { AppContent() }
    }
}
