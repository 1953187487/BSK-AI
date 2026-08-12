package com.floatai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.floatai.data.model.AppLanguage
import com.floatai.ui.flow.LanguageFlow
import com.floatai.ui.flow.ProtocolFlow
import com.floatai.ui.navigation.AppShell
import com.floatai.ui.theme.FloatAITheme
import com.floatai.ui.theme.accentColorByName

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as App
        setContent {
            val settings by app.settingsRepository.settings.collectAsStateWithLifecycle()
            FloatAITheme(
                darkTheme = settings.darkTheme,
                dynamicColor = settings.dynamicColor,
                accentColor = accentColorByName(settings.accentColor)
            ) {
                // v1.0.1：移除 ElevatedGrantFlow 高权限协议，直接进入主界面。
                when {
                    !settings.protocolAgreed && !settings.languageChosen -> LanguageFlow(
                        current = settings.language,
                        onSelect = { lang ->
                            app.settingsRepository.updateSettings {
                                it.copy(language = lang, languageChosen = true)
                            }
                        }
                    )
                    !settings.protocolAgreed -> ProtocolFlow(
                        language = settings.language,
                        onAgree = {
                            app.settingsRepository.updateSettings { it.copy(protocolAgreed = true) }
                        },
                        onLanguage = { lang ->
                            app.settingsRepository.updateSettings { it.copy(language = lang) }
                        }
                    )
                    else -> AppShell()
                }
            }
        }
    }
}
