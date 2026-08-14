package com.floatai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.floatai.core.versioning.VersionGate
import com.floatai.ui.flow.ProtocolFlow
import com.floatai.ui.shell.AppShell
import com.floatai.ui.theme.FloatAITheme
import com.floatai.ui.theme.accentColorByName

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as App
        // 反调试：检测到调试器时在后台线程延迟（不阻塞主线程，避免 setContent 卡死）
        com.floatai.security.AntiDebug.applyDefenseDelayAsync()
        setContent {
            val settings by app.settingsRepository.settings.collectAsStateWithLifecycle()
            FloatAITheme(
                darkTheme = settings.darkTheme,
                dynamicColor = settings.dynamicColor,
                accentColor = accentColorByName(settings.accentColor)
            ) {
                com.floatai.ide.IDE2Home()
            }
        }
    }
}
