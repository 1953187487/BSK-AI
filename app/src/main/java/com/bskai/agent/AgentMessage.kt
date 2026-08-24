package com.bskai.agent

import org.json.JSONArray
import org.json.JSONObject

data class ToolCallData(
    val id: String,
    val name: String,
    val args: JSONObject
)

data class AgentMessage(
    val role: String,
    val content: String = "",
    val toolCallId: String? = null,
    val toolName: String? = null,
    val toolArgs: JSONObject? = null,
    val toolCalls: List<ToolCallData> = emptyList()
) {

    private fun effectiveToolCalls(): List<ToolCallData> =
        if (toolCalls.isNotEmpty()) toolCalls
        else if (toolName != null) listOf(ToolCallData(toolCallId ?: "", toolName, toolArgs ?: JSONObject()))
        else emptyList()

    fun toWire(): JSONObject {
        val obj = JSONObject().put("role", role)
        when (role) {
            "tool" -> {
                obj.put("tool_call_id", toolCallId ?: "")
                obj.put("content", content)
            }
            else -> obj.put("content", content)
        }
        if (role == "assistant") {
            val calls = effectiveToolCalls()
            if (calls.isNotEmpty()) {
                val arr = JSONArray()
                calls.forEach { tc ->
                    arr.put(
                        JSONObject()
                            .put("id", tc.id)
                            .put("type", "function")
                            .put("function", JSONObject()
                                .put("name", tc.name)
                                .put("arguments", tc.args.toString()))
                    )
                }
                obj.put("tool_calls", arr)
            }
        }
        return obj
    }

    fun toJson(): JSONObject {
        val obj = JSONObject()
            .put("role", role)
            .put("content", content)
        if (role == "tool") {
            obj.put("toolCallId", toolCallId ?: "")
        }
        if (role == "assistant") {
            val calls = effectiveToolCalls()
            if (calls.isNotEmpty()) {
                val arr = JSONArray()
                calls.forEach { tc ->
                    arr.put(
                        JSONObject()
                            .put("id", tc.id)
                            .put("name", tc.name)
                            .put("args", tc.args)
                    )
                }
                obj.put("toolCalls", arr)
            }
        }
        return obj
    }

    companion object {
        fun fromJson(obj: JSONObject): AgentMessage {
            val role = obj.optString("role")
            val content = obj.optString("content")
            if (role == "tool") {
                return AgentMessage(role, content, toolCallId = obj.optString("toolCallId").ifEmpty { null })
            }
            val calls = mutableListOf<ToolCallData>()
            val arr = obj.optJSONArray("toolCalls")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val c = arr.getJSONObject(i)
                    calls.add(
                        ToolCallData(
                            c.optString("id"),
                            c.optString("name"),
                            c.optJSONObject("args") ?: JSONObject()
                        )
                    )
                }
            }
            if (calls.isEmpty() && obj.has("toolName")) {
                calls.add(
                    ToolCallData(
                        obj.optString("toolCallId"),
                        obj.optString("toolName"),
                        obj.optJSONObject("toolArgs") ?: JSONObject()
                    )
                )
            }
            return AgentMessage(role, content, toolCalls = calls)
        }
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
