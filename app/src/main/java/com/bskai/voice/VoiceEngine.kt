package com.bskai.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.bskai.data.AppSettings
import com.bskai.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class VoiceEngine(
    private val context: Context,
    private val settings: SettingsRepository
) : TextToSpeech.OnInitListener {

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    var onFinalResult: ((String) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext, this)
    private var ttsReady = false
    private var pendingSpeech: String? = null

    private var recognizer: SpeechRecognizer? = null
    private var recognizerActive = false

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
            }

            override fun onDone(utteranceId: String?) {
                _isSpeaking.value = false
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
            }
        })
    }

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            ttsReady = false
            return
        }
        ttsReady = true
        applyTtsSettings(settings.settings.value)
        pendingSpeech?.let {
            pendingSpeech = null
            speakInternal(it)
        }
    }

    fun applyTtsSettings(s: AppSettings) {
        if (!ttsReady) return
        tts.language = languageLocale(s.ttsLanguage)
        tts.setPitch(s.ttsPitch)
        tts.setSpeechRate(s.ttsSpeed)
    }

    private fun languageLocale(code: String): Locale {
        return when {
            code.startsWith("zh") -> Locale.CHINESE
            code.startsWith("en") -> Locale.ENGLISH
            code.startsWith("ja") -> Locale.JAPANESE
            code.startsWith("ko") -> Locale.KOREAN
            else -> Locale.getDefault()
        }
    }

    fun speak(text: String) {
        if (!ttsReady) {
            pendingSpeech = text
            return
        }
        speakInternal(text)
    }

    private fun speakInternal(text: String) {
        if (!settings.settings.value.ttsEnabled) return
        _isSpeaking.value = true
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "aura")
    }

    fun stopSpeaking() {
        try {
            tts.stop()
        } catch (_: Exception) {
        }
        _isSpeaking.value = false
    }

    fun startListening() {
        if (recognizerActive) return
        val created = SpeechRecognizer.createSpeechRecognizer(context.applicationContext)
        created.setRecognitionListener(recognitionListener)
        recognizer = created
        recognizerActive = true
        _isListening.value = true
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }
        try {
            created.startListening(intent)
        } catch (_: Exception) {
            finishListening()
            onError?.invoke("语音识别启动失败，请检查录音权限")
        }
    }

    fun stopListening() {
        val sr = recognizer
        if (sr != null && recognizerActive) {
            try {
                sr.stopListening()
            } catch (_: Exception) {
            }
        }
    }

    private fun languageTag(): String {
        val code = settings.settings.value.ttsLanguage
        return when {
            code.startsWith("zh") -> "zh-CN"
            code.startsWith("en") -> "en-US"
            code.startsWith("ja") -> "ja-JP"
            code.startsWith("ko") -> "ko-KR"
            else -> Locale.getDefault().toLanguageTag()
        }
    }

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}

        override fun onBeginningOfSpeech() {}

        override fun onRmsChanged(rmsdB: Float) {}

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {}

        override fun onError(error: Int) {
            val message = when (error) {
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "缺少录音权限"
                SpeechRecognizer.ERROR_NETWORK,
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "网络不可用，识别失败"
                SpeechRecognizer.ERROR_NO_MATCH -> "没有听清，请再说一遍"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "识别器忙，请稍后再试"
                SpeechRecognizer.ERROR_AUDIO -> "录音异常"
                else -> "识别失败，请重试"
            }
            finishListening()
            onError?.invoke(message)
        }

        override fun onResults(results: Bundle?) {
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?.trim()
            finishListening()
            if (!text.isNullOrEmpty()) {
                onFinalResult?.invoke(text)
            } else {
                onError?.invoke("没有听清，请再说一遍")
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {}

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun finishListening() {
        cleanupRecognizer()
        _isListening.value = false
    }

    private fun cleanupRecognizer() {
        recognizerActive = false
        try {
            recognizer?.destroy()
        } catch (_: Exception) {
        }
        recognizer = null
    }

    fun shutdown() {
        stopSpeaking()
        cleanupRecognizer()
        try {
            tts.shutdown()
        } catch (_: Exception) {
        }
    }
}
