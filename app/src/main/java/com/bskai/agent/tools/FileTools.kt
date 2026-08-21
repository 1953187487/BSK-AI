package com.bskai.agent.tools

import org.json.JSONObject
import java.io.File

object ReadFileTool : Tool {
    override val name = "read_file"
    override val description = "读取工作区内的文本文件内容，用于理解项目代码与配置。"
    override val requiresPermission = false
    override val parameters: JSONObject = JSONObject()
        .put("type", "object")
        .put("properties", JSONObject()
            .put("path", JSONObject().put("type", "string").put("description", "相对工作区的文件路径，如 app/build.gradle"))
            .put("maxLines", JSONObject().put("type", "integer").put("description", "最多读取行数，默认 200"))
            .put("offset", JSONObject().put("type", "integer").put("description", "起始行号，默认 1")))
        .put("required", JSONArrayOf("path"))

    override suspend fun execute(args: JSONObject, ctx: ToolContext): ToolResult {
        val path = ctx.resolveWorkspace(args.optString("path"))
        val file = File(path)
        if (!file.exists()) return ToolResult(false, "文件不存在: $path")
        if (file.isDirectory) return ToolResult(false, "这是目录: $path")
        val lines = file.readLines()
        val offset = (args.optInt("offset", 1)).coerceAtLeast(1)
        val maxLines = args.optInt("maxLines", 200)
        val slice = lines.drop(offset - 1).take(maxLines)
        val total = lines.size
        return ToolResult(
            true,
            buildString {
                appendLine("文件: $path (共 $total 行)")
                slice.forEachIndexed { i, l ->
                    appendLine("${offset + i}: $l")
                }
                if (offset - 1 + maxLines < total) appendLine("... (已截断，共 $total 行)")
            }.trimEnd()
        )
    }
}

object WriteFileTool : Tool {
    override val name = "write_file"
    override val description = "写入或覆盖工作区内的文件内容，目录不存在时自动创建。"
    override val requiresPermission = true
    override val parameters: JSONObject = JSONObject()
        .put("type", "object")
        .put("properties", JSONObject()
            .put("path", JSONObject().put("type", "string").put("description", "相对工作区的文件路径"))
            .put("content", JSONObject().put("type", "string").put("description", "完整文件内容")))
        .put("required", JSONArrayOf("path", "content"))

    override suspend fun execute(args: JSONObject, ctx: ToolContext): ToolResult {
        val path = ctx.resolveWorkspace(args.optString("path"))
        val content = args.optString("content")
        val file = File(path)
        file.parentFile?.mkdirs()
        file.writeText(content)
        return ToolResult(true, "已写入 ${file.length()} 字节 -> $path")
    }
}

object EditFileTool : Tool {
    override val name = "edit_file"
    override val description = "对工作区内的文件做精确文本替换（Claude Code 风格）。"
    override val requiresPermission = true
    override val parameters: JSONObject = JSONObject()
        .put("type", "object")
        .put("properties", JSONObject()
            .put("path", JSONObject().put("type", "string").put("description", "相对工作区的文件路径"))
            .put("old", JSONObject().put("type", "string").put("description", "需要被替换的原文"))
            .put("new", JSONObject().put("type", "string").put("description", "替换后的新文本")))
        .put("required", JSONArrayOf("path", "old", "new"))

    override suspend fun execute(args: JSONObject, ctx: ToolContext): ToolResult {
        val path = ctx.resolveWorkspace(args.optString("path"))
        val old = args.optString("old")
        val new = args.optString("new")
        val file = File(path)
        if (!file.exists()) return ToolResult(false, "文件不存在: $path")
        val content = file.readText()
        if (!content.contains(old)) {
            return ToolResult(false, "未找到匹配的原文片段，请确认内容与缩进完全一致")
        }
        val updated = content.replace(old, new)
        file.writeText(updated)
        return ToolResult(true, "替换成功 -> $path")
    }
}

object ListDirTool : Tool {
    override val name = "list_dir"
    override val description = "列出工作区内目录内容，用于探索项目结构。"
    override val requiresPermission = false
    override val parameters: JSONObject = JSONObject()
        .put("type", "object")
        .put("properties", JSONObject()
            .put("path", JSONObject().put("type", "string").put("description", "相对工作区的目录路径，默认根目录")))
        .put("required", JSONArrayOf())

    override suspend fun execute(args: JSONObject, ctx: ToolContext): ToolResult {
        val path = ctx.resolveWorkspace(args.optString("path"))
        val dir = File(path)
        if (!dir.exists()) return ToolResult(false, "目录不存在: $path")
        if (!dir.isDirectory) return ToolResult(false, "不是目录: $path")
        val items = dir.listFiles()?.sortedBy { !it.isDirectory } ?: emptyList()
        val sb = StringBuilder()
        sb.appendLine("目录: $path")
        items.forEach { f ->
            val suffix = if (f.isDirectory) "/" else "  (${f.length()})"
            sb.appendLine("  ${f.name}$suffix")
        }
        if (items.isEmpty()) sb.appendLine("  (空)")
        return ToolResult(true, sb.toString().trimEnd())
    }
}

internal fun JSONArrayOf(vararg items: String): org.json.JSONArray =
    org.json.JSONArray().apply { items.forEach { put(it) } }
