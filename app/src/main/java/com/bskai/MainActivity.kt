package com.bskai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bskai.ui.AppRoot
import com.bskai.ui.theme.AuraTheme
import com.bskai.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val app = application as AuraApp
            AuraTheme(darkTheme = true) {
                val viewModel: MainViewModel = viewModel(factory = MainViewModel.Factory(app))
                AppRoot(
                    app = app,
                    viewModel = viewModel
                )
            }
        }
    }
}
