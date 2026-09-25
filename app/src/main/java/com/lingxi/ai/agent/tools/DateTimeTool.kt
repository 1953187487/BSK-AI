package com.lingxi.ai.agent.tools

import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.json.JSONObject

/**
 * current_time: returns the current date and time of the device, including the
 * time zone and the day of the week in Chinese.
 *
 * Registered in LingXiApp.kt toolRegistry.
 * NOTE: user-facing strings are in Chinese and are i18n candidates.
 */
class DateTimeTool : Tool {
    override val name = "current_time"
    override val description =
        "获取当前设备的日期与时间，包含时区与中文星期。当用户询问现在几点、今天星期几时使用。"
    override val requiresWorkspace = false
    override val parametersSchema =
        """{"type":"object","properties":{}}"""

    /**
     * Formats the current instant in the device time zone.
     */
    override suspend fun execute(argumentsJson: String): ToolResult {
        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zone)

        val dayOfWeek = try {
            val formatter = DateTimeFormatter.ofPattern("EEEE", Locale.CHINESE)
            now.format(formatter)
        } catch (_: Exception) {
            "未知"
        }

        val time = try {
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(now)
        } catch (_: Exception) {
            now.toString()
        }

        val offset = now.offset.toString()

        val o = JSONObject()
        try {
            o.put("datetime", time)
            o.put("day_of_week", dayOfWeek)
            o.put("timezone", "$zone UTC$offset")
            o.put("epoch_millis", now.toInstant().toEpochMilli())
            o.put("date", time.substringBefore(" ").ifEmpty { "未知" })
            o.put("time", time.substringAfter(" ", ""))
        } catch (_: Exception) {
        }
        return ToolResult(name, o.toString())
    }
}
