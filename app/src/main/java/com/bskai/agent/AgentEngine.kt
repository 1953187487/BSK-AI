package com.bskai.agent

import android.content.Context
import com.bskai.files.FileController
import com.bskai.intent.IntentRegistry
import com.bskai.media.AudioController
import com.bskai.settings.AppSettings
import com.bskai.voice.VoiceEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class AgentEngine(
    private val context: Context,
    private val voiceEngine: VoiceEngine,
    private val intentRegistry: IntentRegistry,
    private val fileController: FileController,
    private val audioController: AudioController,
    private val settingsStore: com.bskai.settings.SettingsStore
) {
    var onResult: ((String) -> Unit)? = null

    fun processVoiceInput(text: String): String {
        return try {
            val settings = settingsStore.load()
            if (settings.apiProviderKey.isNotEmpty() && settings.apiProviderUrl.isNotEmpty()) {
                callCustomAPI(settings, text)
            } else {
                localProcessing(text)
            }
        } catch (e: Exception) {
            "处理出错: ${e.message}"
        }
    }

    private fun callCustomAPI(settings: AppSettings, text: String): String {
        return try {
            val client = OkHttpClient.Builder().build()
            val requestBody = JSONObject().apply {
                put("model", settings.apiModel)
                put("messages", JSONObject().put("content", text).put("role", "user"))
            }.toString()

            val request = Request.Builder()
                .url(settings.apiProviderUrl)
                .addHeader("Authorization", "Bearer ${settings.apiProviderKey}")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: return "API无响应"

            val json = JSONObject(responseBody)
            json.optString("answer", "无回复")
        } catch (e: IOException) {
            "网络错误: ${e.message}"
        }
    }

    private fun localProcessing(text: String): String {
        val normalizedText = text.lowercase()
        if (normalizedText.contains("时间")) {
            return "现在时间是 ${java.time.LocalTime.now().toString()}"
        }
        if (normalizedText.contains("天气")) {
            return "抱歉，我无法访问实时天气数据。"
        }
        if (normalizedText.contains("你好") || normalizedText.contains("早上好")) {
            return "您好，我是AURA 2.0系统助手，有什么可以帮您的？"
        }
        if (normalizedText.contains("关闭")) {
            audioController.toggleMute()
            voiceEngine.speak("正在关闭设备...")
            return "收到，正在执行关机流程。"
        }
        return "我收到了您的指令: $text"
    }
}
