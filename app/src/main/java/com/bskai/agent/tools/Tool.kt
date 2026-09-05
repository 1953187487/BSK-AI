package com.bskai.agent.tools

/**
 * AI 可调用的工具接口。工具在 chat 循环里被 LLM 选择调用，结果回填到下一次请求。
 * 工具描述以 JSON schema 形式发给 LLM（OpenAI tools 协议）。
 */
interface Tool {
    val name: String
    val description: String
    val parametersSchema: String
    suspend fun execute(argumentsJson: String): ToolResult
}

data class ToolResult(
    val name: String,
    val content: String,
    val isError: Boolean = false
)

class ToolRegistry {
    private val tools = mutableMapOf<String, Tool>()

    fun register(tool: Tool) {
        tools[tool.name] = tool
    }

    fun get(name: String): Tool? = tools[name]

    fun all(): List<Tool> = tools.values.toList()

    fun toolsJsonForLlm(): String {
        if (tools.isEmpty()) return "[]"
        val arr = org.json.JSONArray()
        tools.values.forEach { tool ->
            val obj = org.json.JSONObject().apply {
                put("type", "function")
                put("function", org.json.JSONObject().apply {
                    put("name", tool.name)
                    put("description", tool.description)
                    put("parameters", org.json.JSONObject(tool.parametersSchema))
                })
            }
            arr.put(obj)
        }
        return arr.toString()
    }
}
