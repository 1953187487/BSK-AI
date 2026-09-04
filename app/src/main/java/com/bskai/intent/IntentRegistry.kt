package com.bskai.intent

import android.content.Context
import com.bskai.voice.VoiceEngine

sealed class VoiceIntent(val type: String, val rawText: String) {
    data class PlayMusic(val query: String, val text: String) : VoiceIntent("play_music", text)
    data class StopMusic(val text: String) : VoiceIntent("stop_music", text)
    data class NextSong(val text: String) : VoiceIntent("next_song", text)
    data class PrevSong(val text: String) : VoiceIntent("prev_song", text)
    data class SetVolume(val level: Int, val text: String) : VoiceIntent("set_volume", text)
    data class MoveFile(val from: String, val to: String, val text: String) : VoiceIntent("move_file", text)
    data class CopyFile(val from: String, val to: String, val text: String) : VoiceIntent("copy_file", text)
    data class DeleteFile(val path: String, val text: String) : VoiceIntent("delete_file", text)
    data class OpenApp(val appName: String, val text: String) : VoiceIntent("open_app", text)
    data class ToggleSetting(val setting: String, val text: String) : VoiceIntent("toggle_setting", text)
    data class GetInfo(val query: String, val text: String) : VoiceIntent("get_info", text)
    data class MakeCall(val contact: String, val text: String) : VoiceIntent("make_call", text)
    data class SendMsg(val contact: String, val msg: String, val text: String) : VoiceIntent("send_msg", text)
    data class TakeScreenshot(val text: String) : VoiceIntent("screenshot", text)
    data class Unknown(val text: String) : VoiceIntent("unknown", text)
}

class IntentRegistry(private val context: Context, private val voiceEngine: VoiceEngine) {

    private val rules = listOf(
        Regex("""(?:播放|听|放)(?:一)?[下]??(.+?)[曲歌音乐]?[\s]?(?:的|吧|吗)?[\s]*$""") to { text: String, match: MatchResult? ->
            val query = match?.groupValues?.get(1)?.trim() ?: text
            VoiceIntent.PlayMusic(query, text)
        },
        Regex("""(?:停|停止|暂停|别播)(?:播放|音乐|歌)?[\s]*$""") to { text: String, _: MatchResult? -> VoiceIntent.StopMusic(text) },
        Regex("""(?:下一|切到下一|换)(?:首|首歌|音乐)?[\s]*$""") to { text: String, _: MatchResult? -> VoiceIntent.NextSong(text) },
        Regex("""(?:上一|切到上一|换上一)(?:首|首歌|音乐)?[\s]*$""") to { text: String, _: MatchResult? -> VoiceIntent.PrevSong(text) },
        Regex("""音量(?:调到|设为|调至|开到)?(\d+)[%百分]?[\s]*$""") to { text: String, match: MatchResult? ->
            val level = match?.groupValues?.get(1)?.toIntOrNull() ?: 50
            VoiceIntent.SetVolume(level.coerceIn(0, 100), text)
        },
        Regex("""(?:移动|转移|搬到|移到)(?:从)?(.+?)(?:到|至)(.+?)[\s]*$""") to { text: String, match: MatchResult? ->
            val g1 = match?.groupValues?.get(1)?.trim() ?: ""
            val g2 = match?.groupValues?.get(2)?.trim() ?: ""
            VoiceIntent.MoveFile(g1, g2, text)
        },
        Regex("""(?:复制|拷贝)(?:从)?(.+?)(?:到|至)(.+?)[\s]*$""") to { text: String, match: MatchResult? ->
            val g1 = match?.groupValues?.get(1)?.trim() ?: ""
            val g2 = match?.groupValues?.get(2)?.trim() ?: ""
            VoiceIntent.CopyFile(g1, g2, text)
        },
        Regex("""(?:删除|删掉|移除)(.+?)[\s]*$""") to { text: String, match: MatchResult? ->
            VoiceIntent.DeleteFile(match?.groupValues?.get(1)?.trim() ?: "", text)
        },
        Regex("""(?:打开|启动|运行)(.+?)(?:应用|APP|app)?[\s]*$""") to { text: String, match: MatchResult? ->
            VoiceIntent.OpenApp(match?.groupValues?.get(1)?.trim() ?: text, text)
        },
        Regex("""(?:开启|打开|关闭|关掉|切换)(蓝牙|wifi|飞行模式|定位|手电筒|夜览|勿扰|旋转锁定|热点)[\s]*$""") to { text: String, match: MatchResult? ->
            VoiceIntent.ToggleSetting(match?.groupValues?.get(1) ?: "", text)
        },
        Regex("""(?:截屏|截图|截取屏幕)[\s]*$""") to { text: String, _: MatchResult? -> VoiceIntent.TakeScreenshot(text) },
        Regex("""(?:打电话|拨打电话|致电|打给)(.+?)[\s]*$""") to { text: String, match: MatchResult? ->
            VoiceIntent.MakeCall(match?.groupValues?.get(1)?.trim() ?: "", text)
        },
        Regex("""(?:发消息|发送消息|发短信|发信息)(?:给)?(.+?)(?:说|道|：)?(.+)[\s]*$""") to { text: String, match: MatchResult? ->
            val g1 = match?.groupValues?.get(1)?.trim() ?: ""
            val g2 = match?.groupValues?.get(2)?.trim() ?: ""
            VoiceIntent.SendMsg(g1, g2, text)
        }
    )

    fun recognize(text: String): VoiceIntent {
        for ((regex, factory) in rules) {
            val match = regex.find(text)
            if (match != null) {
                return factory(text, match)
            }
        }
        return VoiceIntent.Unknown(text)
    }

    fun execute(intent: VoiceIntent): String = when (intent) {
        is VoiceIntent.PlayMusic -> handlePlayMusic(intent.query)
        is VoiceIntent.StopMusic -> handleStopMusic()
        is VoiceIntent.NextSong -> handleNextSong()
        is VoiceIntent.PrevSong -> handlePrevSong()
        is VoiceIntent.SetVolume -> handleSetVolume(intent.level)
        is VoiceIntent.MoveFile -> handleMoveFile(intent.from, intent.to)
        is VoiceIntent.CopyFile -> handleCopyFile(intent.from, intent.to)
        is VoiceIntent.DeleteFile -> handleDeleteFile(intent.path)
        is VoiceIntent.OpenApp -> handleOpenApp(intent.appName)
        is VoiceIntent.ToggleSetting -> handleToggleSetting(intent.setting)
        is VoiceIntent.GetInfo -> handleGetInfo(intent.query)
        is VoiceIntent.MakeCall -> handleMakeCall(intent.contact)
        is VoiceIntent.SendMsg -> handleSendMsg(intent.contact, intent.msg)
        is VoiceIntent.TakeScreenshot -> handleScreenshot()
        is VoiceIntent.Unknown -> "抱歉，没听明白：${intent.rawText}"
    }

    private fun handlePlayMusic(query: String): String {
        voiceEngine.speak("正在播放：$query")
        return "已播放 $query"
    }

    private fun handleStopMusic(): String {
        voiceEngine.speak("已停止播放")
        return "已停止音乐播放"
    }

    private fun handleNextSong(): String {
        voiceEngine.speak("下一首")
        return "已切换到下一首"
    }

    private fun handlePrevSong(): String {
        voiceEngine.speak("上一首")
        return "已切换到上一首"
    }

    private fun handleSetVolume(level: Int): String {
        voiceEngine.speak("音量已调整为 $level 百分比")
        return "音量已设置为 $level%"
    }

    private fun handleMoveFile(from: String, to: String): String {
        return "已将 $from 移动到 $to"
    }

    private fun handleCopyFile(from: String, to: String): String {
        return "已将 $from 复制到 $to"
    }

    private fun handleDeleteFile(path: String): String {
        voiceEngine.speak("已删除 $path")
        return "已删除 $path"
    }

    private fun handleOpenApp(appName: String): String {
        voiceEngine.speak("正在打开 $appName")
        return "已打开 $appName"
    }

    private fun handleToggleSetting(setting: String): String {
        voiceEngine.speak("已处理：$setting")
        return "已切换 $setting"
    }

    private fun handleGetInfo(query: String): String {
        voiceEngine.speak("正在查询：$query")
        return "正在为您查询：$query"
    }

    private fun handleMakeCall(contact: String): String {
        voiceEngine.speak("正在拨打 $contact")
        return "正在拨打 $contact"
    }

    private fun handleSendMsg(contact: String, msg: String): String {
        voiceEngine.speak("已发送消息给 $contact：$msg")
        return "已发送消息给 $contact"
    }

    private fun handleScreenshot(): String {
        voiceEngine.speak("正在截图")
        return "已截图"
    }
}
