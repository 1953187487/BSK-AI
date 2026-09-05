package com.bskai.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.bskai.data.AppSettings
import com.bskai.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * VoiceEngine
 *
 * 封装 Android 系统 ASR + TTS。两端都基于 Android Framework（开源）：
 * - ASR: android.speech.SpeechRecognizer
 * - TTS: android.speech.tts.TextToSpeech
 *
 * 关键设计：
 * 1. 持久化一个 SpeechRecognizer 实例，start/stop 复用，不每次 create/destroy，
 *    避免国产 ROM 上 ERROR_RECOGNIZER_BUSY / 双实例抢麦克风。
 * 2. ensureReady() 在 startListening 前调用，等 recognizer 真正可用再 start。
 * 3. 启动失败或 busy 时 destroy 重试一次，给国产 ROM 多一次机会。
 * 4. EXTRA_PREFER_OFFLINE + EXTRA_PARTIAL_RESULTS + EXTRA_LANGUAGE 一起设置。
 * 5. 听不到声音 5 秒（onBeginningOfSpeech 未触发）自动 stop，避免长亮麦。
 */
class VoiceEngine(
    private val context: Context,
    private val settings: SettingsRepository
) : TextToSpeech.OnInitListener {

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _rmsDb = MutableStateFlow(0f)
    val rmsDb: StateFlow<Float> = _rmsDb.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private val _elapsedMs = MutableStateFlow(0L)
    val elapsedMs: StateFlow<Long> = _elapsedMs.asStateFlow()

    private val _status = MutableStateFlow(VoiceStatus.IDLE)
    val status: StateFlow<VoiceStatus> = _status.asStateFlow()

    var onFinalResult: ((String) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext, this)
    private var ttsReady = false
    private var pendingSpeech: String? = null

    private var recognizer: SpeechRecognizer? = null
    private var recognizerActive = false
    private var listenStartMs = 0L
    private var firstAudioAtMs = 0L
    private val silenceTimeoutMs = 5_000L

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) { _isSpeaking.value = true }
            override fun onDone(utteranceId: String?) { _isSpeaking.value = false }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) { _isSpeaking.value = false }
        })
        ensureRecognizer()
    }

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            Log.w(TAG, "TTS init failed: $status")
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
        try { tts.stop() } catch (_: Exception) {}
        _isSpeaking.value = false
    }

    /**
     * 启动一次识别。已运行则直接返回 true。
     */
    fun startListening(): Boolean {
        if (recognizerActive) {
            Log.d(TAG, "startListening skipped: already active")
            return true
        }
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w(TAG, "SpeechRecognizer not available on device")
            onError?.invoke("当前设备不支持语音识别，请使用文字输入")
            return false
        }
        val sr = ensureRecognizer()
        recognizerActive = true
        listenStartMs = System.currentTimeMillis()
        firstAudioAtMs = 0L
        _elapsedMs.value = 0L
        _partialText.value = ""
        _rmsDb.value = 0f
        _status.value = VoiceStatus.STARTING
        _isListening.value = true
        val intent = buildRecognizerIntent()
        try {
            sr.startListening(intent)
            Log.d(TAG, "startListening OK (locale=${intent.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE)})")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "startListening failed", e)
            recognizerActive = false
            _isListening.value = false
            _status.value = VoiceStatus.ERROR
            // 二次重试：destroy 后重建一次
            destroyRecognizer()
            val sr2 = ensureRecognizer()
            try {
                sr2.startListening(intent)
                recognizerActive = true
                _isListening.value = true
                _status.value = VoiceStatus.STARTING
                Log.d(TAG, "startListening retry OK")
                return true
            } catch (e2: Exception) {
                Log.e(TAG, "startListening retry failed", e2)
                onError?.invoke("语音识别启动失败：${e2.message ?: "未知错误"}")
                return false
            }
        }
    }

    fun stopListening() {
        val sr = recognizer
        if (sr != null && recognizerActive) {
            try { sr.stopListening() } catch (e: Exception) {
                Log.w(TAG, "stopListening exception", e)
            }
        }
    }

    private fun buildRecognizerIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
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

    private fun ensureRecognizer(): SpeechRecognizer {
        val existing = recognizer
        if (existing != null) return existing
        val created = SpeechRecognizer.createSpeechRecognizer(context.applicationContext)
        created.setRecognitionListener(recognitionListener)
        recognizer = created
        return created
    }

    private fun destroyRecognizer() {
        try { recognizer?.destroy() } catch (_: Exception) {}
        recognizer = null
        recognizerActive = false
    }

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _status.value = VoiceStatus.LISTENING
            firstAudioAtMs = 0L
            Log.d(TAG, "onReadyForSpeech")
        }

        override fun onBeginningOfSpeech() {
            firstAudioAtMs = System.currentTimeMillis()
            _elapsedMs.value = firstAudioAtMs - listenStartMs
            Log.d(TAG, "onBeginningOfSpeech")
        }

        override fun onRmsChanged(rmsdB: Float) {
            val normalized = ((rmsdB + 2f) / 18f).coerceIn(0f, 1f)
            _rmsDb.value = normalized
            if (recognizerActive) {
                val now = System.currentTimeMillis()
                _elapsedMs.value = now - listenStartMs
                // 5 秒没听到声音自动停止
                if (firstAudioAtMs == 0L && now - listenStartMs > silenceTimeoutMs) {
                    Log.d(TAG, "silence timeout, auto-stop")
                    stopListening()
                }
            }
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            _status.value = VoiceStatus.PROCESSING
            Log.d(TAG, "onEndOfSpeech")
        }

        override fun onError(error: Int) {
            Log.w(TAG, "onError code=$error")
            val message = when (error) {
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "缺少录音权限，请在系统设置中开启"
                SpeechRecognizer.ERROR_NETWORK,
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "网络不可用，识别失败"
                SpeechRecognizer.ERROR_NO_MATCH -> "没有听清，请再说一遍"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "识别器忙，请稍后再试"
                SpeechRecognizer.ERROR_AUDIO -> "录音异常，请检查麦克风"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "没有检测到语音，请再试一次"
                SpeechRecognizer.ERROR_CLIENT -> "客户端错误"
                SpeechRecognizer.ERROR_SERVER -> "服务端错误"
                SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> "请求过于频繁"
                SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> "服务端断开"
                else -> "识别失败（code=$error）"
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

        override fun onPartialResults(partialResults: Bundle?) {
            val partial = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?.trim()
            if (!partial.isNullOrEmpty()) {
                _partialText.value = partial
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun finishListening() {
        recognizerActive = false
        _isListening.value = false
        _status.value = VoiceStatus.IDLE
        _rmsDb.value = 0f
        _partialText.value = ""
    }

    fun shutdown() {
        stopSpeaking()
        destroyRecognizer()
        try { tts.shutdown() } catch (_: Exception) {}
    }

    enum class VoiceStatus { IDLE, STARTING, LISTENING, PROCESSING, ERROR }

    companion object {
        private const val TAG = "VoiceEngine"
    }
}
