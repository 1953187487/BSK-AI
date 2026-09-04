package com.bskai.intent

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.bskai.media.AudioController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Skill(
    val id: String,
    val label: String,
    val description: String,
    val patterns: List<Regex>,
    val execute: suspend SkillContext.(params: SkillParams) -> String?
)

data class SkillParams(
    val rawText: String,
    val groupValues: List<String>
) {
    fun group(index: Int): String? = groupValues.getOrNull(index)?.takeIf { it.isNotBlank() }
}

class SkillContext(
    val context: Context
) {
    val audio = AudioController(context)

    fun startActivity(activity: String) {
        try {
            val cls = Class.forName(activity)
            val intent = Intent(context, cls).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    fun openSystemPage(action: String) {
        try {
            val intent = Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }
}

class SkillEngine(private val appContext: Context) {

    private val skills: List<Skill> = listOf(
        Skill(
            id = "volume_percent",
            label = "音量调节",
            description = "把音量调到 50",
            patterns = listOf(Regex("音量.{0,8}?(\\d{1,3})\\s*%?"))
        ) { p ->
            val parsed = p.group(1)?.toIntOrNull()
            if (parsed == null) {
                null
            } else {
                audio.setMusicVolumePercent(parsed)
                "已把音量调到百分之${parsed}"
            }
        },

        Skill(
            id = "volume_up",
            label = "调大音量",
            description = "调大一点音量",
            patterns = listOf(
                Regex(".*(?:音量|声音).*(?:调大|加大|大一点|高一点|调高|大点)"),
                Regex("(?:调大|加大).*(?:音量|声音)")
            )
        ) {
            audio.volumeUp()
            "音量已调大"
        },

        Skill(
            id = "volume_down",
            label = "调小音量",
            description = "把声音调小",
            patterns = listOf(
                Regex(".*(?:音量|声音).*(?:调小|减小|小一点|低一点|调低|小点)"),
                Regex("(?:调小|减小).*(?:音量|声音)")
            )
        ) {
            audio.volumeDown()
            "音量已调小"
        },

        Skill(
            id = "mute",
            label = "静音切换",
            description = "开启或关闭静音",
            patterns = listOf(
                Regex(".*(?:静音|静音模式|把声音关掉|把声音打开).*")
            )
        ) {
            val muted = audio.toggleMute()
            if (muted) "已静音" else "已取消静音"
        },

        Skill(
            id = "media_play_pause",
            label = "播放暂停",
            description = "播放音乐或暂停",
            patterns = listOf(
                Regex(".*(?:播放|暂停|停止播放|继续播放).*(?:音乐|歌曲|媒体|音乐播放器).*"),
                Regex("^(?:播放音乐|暂停|继续播放|停止播放)$")
            )
        ) {
            audio.tryToggleMediaPlayPause(context)
            "好的"
        },

        Skill(
            id = "open_system_settings",
            label = "打开系统设置",
            description = "打开系统设置",
            patterns = listOf(
                Regex("(?:打开|进入|开启|帮我打开).{0,4}(?:系统)?设置"),
                Regex("(?:打开|进入).{0,4}(?:蓝牙|无线网络|wifi|WiFi|定位|存储|通知|电池).{0,2}设置")
            )
        ) { p ->
            val t = p.rawText.lowercase()
            val action = when {
                t.contains("蓝牙") -> Settings.ACTION_BLUETOOTH_SETTINGS
                t.contains("无线") || t.contains("wifi") || t.contains("wi-fi") ->
                    Settings.ACTION_WIFI_SETTINGS
                t.contains("定位") || t.contains("位置") -> Settings.ACTION_LOCATION_SOURCE_SETTINGS
                t.contains("存储") -> Settings.ACTION_INTERNAL_STORAGE_SETTINGS
                t.contains("通知") -> Settings.ACTION_APP_NOTIFICATION_SETTINGS
                t.contains("电池") -> Settings.ACTION_BATTERY_SAVER_SETTINGS
                else -> Settings.ACTION_SETTINGS
            }
            openSystemPage(action)
            "好的，已为你打开"
        },

        Skill(
            id = "open_app",
            label = "打开应用",
            description = "打开相机 / 浏览器 / 设置等",
            patterns = listOf(
                Regex("(?:打开|启动|运行|帮我打开)\\s*(\\S+)"),
                Regex("开一下\\s*(\\S+)")
            )
        ) { p ->
            val name = p.group(1) ?: return@Skill null
            val pkg = appPackages[name.trim()] ?: return@Skill null
            try {
                val launch = appContext.packageManager.getLaunchIntentForPackage(pkg)
                    ?: return@Skill null
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                appContext.startActivity(launch)
                "好的，打开${name.trim()}"
            } catch (_: Exception) {
                null
            }
        },

        Skill(
            id = "time",
            label = "报时间",
            description = "现在几点了",
            patterns = listOf(Regex(".*(?:现在|当前|目前)?(?:几点了|几点钟|几点|什么时间).*"))
        ) {
            val time = SimpleDateFormat("HH点mm分", Locale.CHINA).format(Date())
            "现在是$time"
        },

        Skill(
            id = "date",
            label = "报日期",
            description = "今天几号 / 星期几",
            patterns = listOf(
                Regex(".*(?:今天|现在).{0,3}(?:几号|日期|星期几|礼拜几).*"),
                Regex(".*什么日期.*")
            )
        ) {
            val date = SimpleDateFormat("M月d日 EEEE", Locale.CHINA).format(Date())
            "今天是$date"
        },

        Skill(
            id = "device_info",
            label = "设备信息",
            description = "查询手机型号与系统版本",
            patterns = listOf(
                Regex(".*(?:手机型号|什么手机|设备信息|啥手机|什么型号).*"),
                Regex(".*(?:系统版本|安卓版本|系统是多少|什么系统).*")
            )
        ) {
            "这是${Build.MANUFACTURER} ${Build.MODEL}，安卓 ${Build.VERSION.RELEASE}"
        },

        Skill(
            id = "open_aura_settings",
            label = "打开 AURA 设置",
            description = "打开 AURA 设置页",
            patterns = listOf(
                Regex("(?:打开|进入|去)\\s*(?:aura|助手)\\s*(?:应用)?设置"),
                Regex(".*(?:修改|设置)\\s*(?:aura|助手).*")
            )
        ) {
            startActivity("com.bskai.MainActivity")
            "好的，已打开设置页"
        }
    )

    suspend fun tryExecute(text: String): String? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        for (skill in skills) {
            for (pattern in skill.patterns) {
                val match = pattern.find(trimmed) ?: continue
                if (skill.id == "open_app") {
                    val name = match.groupValues.getOrNull(1)?.trim() ?: continue
                    if (appPackages[name] == null) continue
                }
                val params = SkillParams(
                    rawText = trimmed,
                    groupValues = match.groupValues
                )
                val result = skill.execute(SkillContext(appContext), params)
                if (result != null) return result
            }
        }
        return null
    }

    fun skillsList(): List<Skill> = skills

    companion object {
        private val appPackages = mapOf(
            "设置" to "com.android.settings",
            "相机" to "com.android.camera",
            "浏览器" to "com.android.browser",
            "日历" to "com.android.calendar",
            "计算器" to "com.android.calculator2",
            "时钟" to "com.android.deskclock",
            "联系人" to "com.android.contacts",
            "文件" to "com.android.documentsui",
            "信息" to "com.android.mms",
            "电话" to "com.android.dialer"
        )
    }
}
