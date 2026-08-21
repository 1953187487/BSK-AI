package com.bskai.agent.tools

import com.bskai.toolkit.ApkInspector
import org.json.JSONObject
import java.io.File

object AnalyzeApkTool : Tool {
    override val name = "analyze_apk"
    override val description = "分析 APK 文件的包名、版本、权限、签名摘要等信息。"
    override val requiresPermission = true
    override val parameters: JSONObject = JSONObject()
        .put("type", "object")
        .put("properties", JSONObject()
            .put("path", JSONObject().put("type", "string").put("description", "APK 文件的完整路径")))
        .put("required", JSONArrayOf("path"))

    override suspend fun execute(args: JSONObject, ctx: ToolContext): ToolResult {
        val path = args.optString("path")
        val file = File(path)
        if (!file.exists()) return ToolResult(false, "APK 文件不存在: $path")
        return runCatching {
            val info = ApkInspector.inspect(ctx.app, file).getOrNull()
                ?: return ToolResult(false, "无法解析 APK")
            val sb = buildString {
                appendLine("应用: ${info.applicationLabel}")
                appendLine("包名: ${info.packageName}")
                appendLine("版本: ${info.versionName} (${info.versionCode})")
                appendLine("Min SDK: ${info.minSdk}")
                appendLine("Target SDK: ${info.targetSdk}")
                if (info.permissions.isNotEmpty()) {
                    appendLine("权限 (${info.permissions.size}):")
                    info.permissions.forEach { appendLine("  - $it") }
                }
                if (info.signatures.isNotEmpty()) {
                    appendLine("签名 SHA-256 (${info.signatures.firstOrNull()?.take(30)}...)")
                }
            }
            ToolResult(true, sb.trimEnd())
        }.getOrElse { ToolResult(false, "分析失败: ${it.message}") }
    }
}
