package com.lingxi.ai.agent.tools

/**
 * AI 可调用的工具接口。工具在 chat 循环里被 LLM 选择调用，结果回填到下一次请求。
 * 工具描述以 JSON schema 形式发给 LLM（OpenAI tools 协议）。
 */
interface Tool {
    val name: String
    val description: String
    val parametersSchema: String

    /**
     * 是否依赖工作区权限（workspaceEnabled）。
     * 内核会据此过滤工具暴露面：工作区关闭时不把该工具发给模型。
     */
    val requiresWorkspace: Boolean get() = false

    /** 执行工具。参数为模型给出的 JSON 字符串；失败抛异常或返回 isError 结果。 */
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

    /**
     * 生成 OpenAI tools 协议的完整 JSON 数组字符串（调试/日志用）。
     * 运行时暴露面请使用 [definitions]。
     */
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

    /** 供 LlmClient 使用的工具定义列表（name/description/parameters）。 */
    fun definitions(): List<Map<String, Any>> = tools.values.map { tool ->
        mapOf(
            "name" to tool.name,
            "description" to tool.description,
            "parameters" to org.json.JSONObject(tool.parametersSchema)
        )
    }
}
