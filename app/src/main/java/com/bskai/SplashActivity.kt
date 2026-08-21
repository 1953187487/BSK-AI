package com.bskai

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.bskai.BskApp
import com.bskai.ui.screens.agreements.AgreementsScreen

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        val settingsStore = (application as BskApp).settingsStore
        val protocolAgreed = settingsStore.settings.value.protocolAgreed

        if (!protocolAgreed) {
            setContent {
                AgreementsScreen(
                    app = application as BskApp,
                    onAgreeAll = {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    },
                    onSkipToMain = {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                )
            }
        } else {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
