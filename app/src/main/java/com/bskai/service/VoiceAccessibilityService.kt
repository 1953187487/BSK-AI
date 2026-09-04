package com.bskai.service
import com.bskai.AuraApp

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.bskai.voice.VoiceEngine

class VoiceAccessibilityService : AccessibilityService() {

    private val voiceEngine: VoiceEngine
        get() = (applicationContext as AuraApp).voiceEngine

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val text = event?.text?.joinToString(" ") ?: return
        val lower = text.lowercase()
        when {
            lower.contains("打开") && lower.contains("设置") -> voiceEngine.speak("正在打开设置")
            lower.contains("截屏") || lower.contains("截图") -> voiceEngine.speak("正在截屏")
            lower.contains("亮度") -> voiceEngine.speak("请滑动调节亮度")
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
    }
}
