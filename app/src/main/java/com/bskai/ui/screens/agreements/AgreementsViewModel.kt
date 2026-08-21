package com.bskai.ui.screens.agreements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bskai.BskApp

class AgreementsViewModel(private val app: BskApp) : ViewModel() {
    private val prefs = app.getSharedPreferences("bsk_ai_prefs", android.content.Context.MODE_PRIVATE)

    val agreedToTerms get() = prefs.getBoolean("terms_agreed", false)
    val agreedToOSS get() = prefs.getBoolean("oss_agreed", false)
    val allAgreed get() = agreedToTerms && agreedToOSS

    fun toggleTerms() {
        prefs.edit().putBoolean("terms_agreed", !agreedToTerms).apply()
    }

    fun toggleOSS() {
        prefs.edit().putBoolean("oss_agreed", !agreedToOSS).apply()
    }

    fun agreeAll() {
        prefs.edit()
            .putBoolean("terms_agreed", true)
            .putBoolean("oss_agreed", true)
            .putBoolean("protocol_agreed", true)
            .apply()
    }

    class Factory(private val app: BskApp) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AgreementsViewModel(app) as T
        }
    }
}
