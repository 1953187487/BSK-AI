package com.bskai.agent.tools

import android.content.Context
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
    val workspaceRoot: String
) {
    fun resolveWorkspace(path: String): String {
        val p = path.removePrefix("/").removePrefix("./")
        return if (p.isEmpty()) workspaceRoot else "$workspaceRoot/$p"
    }
}
