package com.bskai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bskai.ui.AppRoot
import com.bskai.ui.screens.agent.AgentViewModel
import com.bskai.ui.theme.BskTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val app = application as BskApp
            val settings = app.settingsStore.settings.value
            BskTheme(
                darkTheme = settings.darkTheme
            ) {
                val agentViewModel: AgentViewModel = viewModel(factory = AgentViewModel.Factory(app))
                AppRoot(
                    app = app,
                    agentViewModel = agentViewModel
                )
            }
        }
    }
}
