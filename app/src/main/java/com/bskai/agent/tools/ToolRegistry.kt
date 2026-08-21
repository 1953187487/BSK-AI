package com.bskai.agent.tools

import android.content.Context
import kotlinx.coroutines.runBlocking

/**
 * 工具注册表：Agent 可见的所有工具。
 */
object ToolRegistry {

    private val registry = listOf(
        ReadFileTool,
        WriteFileTool,
        EditFileTool,
        ListDirTool,
        ShellTool,
        SystemInfoTool,
        NewProjectTool,
        ListModelsTool,
        DownloadModelTool,
        BuildProjectTool
    )

    fun all(): List<Tool> = registry

    fun find(name: String): Tool? = registry.firstOrNull { it.name == name }

    fun definitions(): org.json.JSONArray {
        val arr = org.json.JSONArray()
        registry.forEach { t ->
            arr.put(
                org.json.JSONObject()
                    .put("type", "function")
                    .put("function", org.json.JSONObject()
                        .put("name", t.name)
                        .put("description", t.description)
                        .put("parameters", t.parameters))
            )
        }
        return arr
    }

    fun runTool(name: String, args: org.json.JSONObject, ctx: ToolContext): ToolResult {
        val tool = find(name) ?: return ToolResult(false, "未知工具: $name")
        return runCatching { runBlocking { tool.execute(args, ctx) } }
            .getOrElse { ToolResult(false, "工具异常: ${it.message}") }
    }
}
