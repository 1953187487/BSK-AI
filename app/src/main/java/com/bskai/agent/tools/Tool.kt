package com.bskai.agent.tools

import android.content.Context
import com.bskai.agent.Workspace
import com.bskai.agent.WorkspaceEntry
import org.json.JSONObject

data class ToolResult(
    val success: Boolean,
    val output: String
)

interface Tool {
    val name: String
    val description: String
    val requiresPermission: Boolean
    val parameters: JSONObject
    suspend fun execute(args: JSONObject, ctx: ToolContext): ToolResult
}

class ToolContext(
    val app: Context,
    val workspace: Workspace
) {
    suspend fun exists(path: String): Boolean = workspace.exists(path)
    suspend fun isDirectory(path: String): Boolean = workspace.isDirectory(path)
    suspend fun readText(path: String): String = workspace.readText(path)
    suspend fun writeText(path: String, content: String) = workspace.writeText(path, content)
    suspend fun list(path: String): List<WorkspaceEntry> = workspace.list(path)
    fun realPathFor(path: String): String? = workspace.realPathFor(path)
}
