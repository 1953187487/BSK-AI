package com.floatai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.floatai.data.model.AppLanguage
import com.floatai.ui.flow.ElevatedGrantFlow
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
                // 是否走语言选择页：协议未同意时强制先选语言
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
                    !settings.elevatedGranted -> ElevatedGrantFlow(
                        language = settings.language,
                        onGranted = {
                            app.settingsRepository.updateSettings { it.copy(elevatedGranted = true) }
                        }
                    )
                    else -> AppShell()
                }
            }
        }
    }
}
