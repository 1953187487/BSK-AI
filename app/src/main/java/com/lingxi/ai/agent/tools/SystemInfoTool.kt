package com.lingxi.ai.agent.tools

import android.content.Context
import android.os.Build
import org.json.JSONObject
import java.io.File

/**
 * system_info: reports device and app information (Android version, SDK, model,
 * available memory, CPU cores, package name and version).
 *
 * Registered in LingXiApp.kt toolRegistry.
 * NOTE: user-facing strings are in Chinese and are i18n candidates.
 */
class SystemInfoTool(private val context: Context) : Tool {
    override val name = "system_info"
    override val description =
        "查询当前设备与应用信息：Android 版本、SDK 级别、设备型号、可用内存、CPU 核数、包名与版本号。"
    override val requiresWorkspace = false
    override val parametersSchema =
        """{"type":"object","properties":{}}"""

    /**
     * Collects the device and app facts. Every field is guarded individually,
     * so a single failing read never breaks the whole tool.
     */
    override suspend fun execute(argumentsJson: String): ToolResult {
        val o = JSONObject()
        put(o, "android_version", safe { "Android ${Build.VERSION.RELEASE}" })
        put(o, "sdk_int", safe { Build.VERSION.SDK_INT.toString() })
        put(o, "device_model", safe { "${Build.MANUFACTURER} ${Build.MODEL}".trim() })
        put(o, "available_memory_mb", safe { availableMemoryMb() })
        put(o, "cpu_cores", safe { cpuCoreCount().toString() })
        put(o, "package_name", safe { context.packageName })
        put(o, "app_version", safe { appVersion() })
        return ToolResult(name, o.toString())
    }

    private fun put(obj: JSONObject, key: String, value: String) {
        try {
            obj.put(key, value)
        } catch (_: Exception) {
        }
    }

    private fun safe(block: () -> String): String = try {
        block()
    } catch (_: Exception) {
        "未知"
    }

    private fun availableMemoryMb(): String {
        val info = android.app.ActivityManager.MemoryInfo()
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
            ?: return "未知"
        am.getMemoryInfo(info)
        val total = info.totalMem
        return if (total > 0) {
            val used = total - info.availMem
            "${used / (1024 * 1024)}MB used / ${total / (1024 * 1024)}MB total"
        } else {
            "${info.availMem / (1024 * 1024)}MB available"
        }
    }

    private fun cpuCoreCount(): Int {
        val files = File("/sys/devices/system/cpu/").listFiles { f -> f.name.matches(Regex("cpu[0-9]+")) }
        return files?.size ?: 0
    }

    private fun appVersion(): String {
        val pm = context.packageManager
        val info = pm.getPackageInfo(context.packageName, 0)
        val code = info.longVersionCode.toString()
        @Suppress("DEPRECATION")
        val name = info.versionName?.takeIf { it.isNotBlank() } ?: code
        return "$name ($code)"
    }
}
