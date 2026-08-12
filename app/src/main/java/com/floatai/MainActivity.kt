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
        // 反调试：检测到调试器时延迟启动（增加破解成本）
        runCatching {
            if (com.floatai.security.AntiDebug.isDebugging()) {
                Thread.sleep(800)
            }
        }
        setContent {
            val settings by app.settingsRepository.settings.collectAsStateWithLifecycle()
            FloatAITheme(
                darkTheme = settings.darkTheme,
                dynamicColor = settings.dynamicColor,
                accentColor = accentColorByName(settings.accentColor)
            ) {
                // 启动流：
                //   1. 用户须知 + 开源协议 + 权限声明（每版本必须重签）
                //   2. 主界面（底部 NavigationBar + 抽屉式菜单）
                // 语言选择在「设置」中
                val context = this@MainActivity
                when {
                    !settings.protocolAgreed || VersionGate.needsReSign(context) -> ProtocolFlow(
                        language = settings.language,
                        onAgree = {
                            VersionGate.markAgreed(context)
                            app.settingsRepository.updateSettings { it.copy(protocolAgreed = true) }
                            android.widget.Toast.makeText(
                                context, "协议已签署", android.widget.Toast.LENGTH_SHORT
                            ).show()
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
