package com.bskai.agent

import android.content.Context
import com.bskai.files.FileController
import com.bskai.intent.IntentRegistry
import com.bskai.intent.VoiceIntent
import com.bskai.media.AudioController
import com.bskai.voice.VoiceEngine

class AgentEngine(
    private val context: Context,
    private val voiceEngine: VoiceEngine,
    private val intentRegistry: IntentRegistry,
    private val fileController: FileController,
    private val audioController: AudioController
) {

    var onResult: ((String) -> Unit)? = null

    suspend fun processVoiceInput(text: String): String {
        val intent = intentRegistry.recognize(text)
        return when (intent) {
            is VoiceIntent.Unknown -> handleFallback(text)
            else -> intentRegistry.execute(intent)
        }.also { response ->
            voiceEngine.speak(response)
            onResult?.invoke(response)
        }
    }

    private fun handleFallback(text: String): String {
        return when {
            text.contains("时间") || text.contains("几点") -> {
                val time = java.text.SimpleDateFormat("HH:mm", java.util.Locale.CHINESE).format(java.util.Date())
                "现在时间是 $time"
            }
            text.contains("日期") || text.contains("今天") -> {
                val date = java.text.SimpleDateFormat("yyyy年MM月dd日 EEEE", java.util.Locale.CHINESE).format(java.util.Date())
                "今天是 $date"
            }
            text.contains("天气") -> "抱歉，当前版本暂不支持天气查询功能。请查看手机自带天气应用。"
            text.contains("电量") || text.contains("电池") -> "当前电量信息可在设置中查看。"
            text.contains("你好") || text.contains("嗨") || text.contains("您好") -> "你好！我是AURA，你的智能语音助手。有什么可以帮到你的？"
            text.contains("你是谁") || text.contains("你是什么") -> "我是AURA，一款运行在Android设备上的智能语音助手。我可以通过语音帮你控制媒体、管理文件、操作手机各项功能。"
            text.contains("帮助") || text.contains("你能做什么") -> "我可以帮你：播放音乐、调节音量、移动和复制文件、删除文件、打开应用、切换手机设置、拨打电话、发送消息、截图等。你可以直接对我说相应的指令。"
            text.contains("谢谢") || text.contains("感谢") -> "不客气，随时为你效劳！"
            else -> "抱歉，我不太理解：'$text'。你可以试试说：'播放音乐'、'打开设置'、'删除这个文件' 等指令。"
        }
    }

    fun getSystemStatus(): String {
        val sb = StringBuilder()
        sb.appendLine("系统状态：")
        sb.appendLine("  电量: ${getBatteryLevel()}%")
        sb.appendLine("  网络: ${getConnectionType()}")
        sb.appendLine("  存储可用: ${fileController.getStorageInfo().free / 1048576} MB")
        sb.appendLine("  媒体播放: ${if (audioController.isMediaPlaying) "播放中" else "已停止"}")
        sb.appendLine("  静音模式: ${if (audioController.isMuted) "是" else "否"}")
        return sb.toString()
    }

    private fun getBatteryLevel(): Int {
        val intent = context.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: 100
        return if (level >= 0) level * 100 / scale else -1
    }

    private fun getConnectionType(): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val network = cm.activeNetworkInfo
        return if (network != null && network.isConnected) {
            when (network.type) {
                android.net.ConnectivityManager.TYPE_WIFI -> "WiFi"
                android.net.ConnectivityManager.TYPE_CELLULAR -> "移动数据"
                else -> network.typeName
            }
        } else "无网络"
    }
}
