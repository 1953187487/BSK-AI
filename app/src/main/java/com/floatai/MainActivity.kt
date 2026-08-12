package com.floatai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.floatai.core.versioning.VersionGate
import com.floatai.ui.flow.LanguageFlow
import com.floatai.ui.flow.ProtocolFlow
import com.floatai.ui.shell.AppShell
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
                // 启动流：
                //   1. 首次选语言
                //   2. 用户须知 + 权限协议（每版本必须重签）
                //   3. 主界面（底部 NavigationBar + 抽屉式菜单）
                val context = this@MainActivity
                when {
                    !settings.protocolAgreed && !settings.languageChosen -> LanguageFlow(
                        current = settings.language,
                        onSelect = { lang ->
                            app.settingsRepository.updateSettings {
                                it.copy(language = lang, languageChosen = true)
                            }
                        }
                    )
                    !settings.protocolAgreed || VersionGate.needsReSign(context) -> ProtocolFlow(
                        language = settings.language,
                        onAgree = {
                            VersionGate.markAgreed(context)
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
