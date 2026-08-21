package com.bskai.agent

import org.json.JSONArray
import org.json.JSONObject

data class AgentMessage(
    val role: String,
    val content: String = "",
    val toolCallId: String? = null,
    val toolName: String? = null,
    val toolArgs: JSONObject? = null
) {
    fun toWire(): JSONObject {
        val obj = JSONObject().put("role", role)
        when (role) {
            "tool" -> {
                obj.put("tool_call_id", toolCallId ?: "")
                obj.put("content", content)
            }
            else -> obj.put("content", content)
        }
        if (role == "assistant" && toolName != null) {
            val call = JSONObject()
                .put("id", toolCallId ?: "")
                .put("type", "function")
                .put("function", JSONObject()
                    .put("name", toolName)
                    .put("arguments", toolArgs?.toString() ?: "{}"))
            obj.put("tool_calls", JSONArray().put(call))
        }
        return obj
    }
}

data class AgentTurn(
    val request: String,
    val messages: List<AgentMessage>,
    val finished: Boolean = false
)

sealed class AgentEvent {
    data class Delta(val text: String) : AgentEvent()
    data class ToolCallStarted(val id: String, val name: String, val args: JSONObject) : AgentEvent()
    data class ToolCallFinished(val id: String, val name: String, val output: String) : AgentEvent()
    data class PermissionRequested(
        val requestId: String,
        val toolName: String,
        val args: JSONObject
    ) : AgentEvent()

    data class PermissionResolved(val requestId: String, val allowed: Boolean) : AgentEvent()
    data class Done(val finishReason: String) : AgentEvent()
    data class Error(val message: String) : AgentEvent()
}
