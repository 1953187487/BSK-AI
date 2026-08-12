package com.floatai.mcp

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * MCP 客户端（JSON-RPC over HTTP / SSE）。
 *
 * 协议：https://spec.modelcontextprotocol.io/
 *  - initialize：握手
 *  - tools/list：列出可用工具
 *  - tools/call：调用工具
 *  - resources/list：列出资源
 *  - resources/read：读取资源
 *
 * 支持 HTTP POST JSON-RPC 2.0。
 */
class McpClient(
    val name: String,
    val baseUrl: String,
    val apiKey: String? = null
) {
    @Volatile var connected: Boolean = false
        private set

    private var requestId = 0
    private val sessionId: String? = null

    private fun nextId(): Int = synchronized(this) { ++requestId }

    /** initialize 握手。 */
    fun initialize(): JSONObject = rpc(
        method = "initialize",
        params = JSONObject()
            .put("protocolVersion", "2024-11-05")
            .put("capabilities", JSONObject())
            .put("clientInfo", JSONObject()
                .put("name", "FloatAI")
                .put("version", "1.0.2"))
    ).also { connected = true }

    /** 列出可用工具。 */
    fun listTools(): List<JSONObject> {
        val resp = rpc("tools/list", JSONObject())
        val arr = resp.optJSONArray("tools") ?: JSONArray()
        return (0 until arr.length()).map { arr.getJSONObject(it) }
    }

    /** 调用工具。 */
    fun callTool(name: String, arguments: JSONObject): JSONObject =
        rpc("tools/call", JSONObject()
            .put("name", name)
            .put("arguments", arguments))

    /** 列出资源。 */
    fun listResources(): List<JSONObject> {
        val resp = rpc("resources/list", JSONObject())
        val arr = resp.optJSONArray("resources") ?: JSONArray()
        return (0 until arr.length()).map { arr.getJSONObject(it) }
    }

    /** 读取资源。 */
    fun readResource(uri: String): JSONObject =
        rpc("resources/read", JSONObject().put("uri", uri))

    /** 关闭连接。 */
    fun close() { connected = false }

    /** JSON-RPC 调用。 */
    fun rpc(method: String, params: JSONObject): JSONObject {
        val payload = JSONObject()
            .put("jsonrpc", "2.0")
            .put("id", nextId())
            .put("method", method)
            .put("params", params)

        val url = URL(baseUrl.trimEnd('/') + "/")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 15_000
            readTimeout = 60_000
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json, text/event-stream")
            apiKey?.let { setRequestProperty("Authorization", "Bearer $it") }
            sessionId?.let { setRequestProperty("Mcp-Session-Id", it) }
        }
        try {
            val body = payload.toString().toByteArray(Charsets.UTF_8)
            conn.setFixedLengthStreamingMode(body.size)
            conn.outputStream.use { os: OutputStream -> os.write(body) }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.let { BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use { it.readText() } } ?: "{}"

            // MCP 可能返回 SSE（event: data ...）或纯 JSON
            val jsonText = if (text.startsWith("event:") || text.startsWith("data:")) {
                text.lineSequence()
                    .firstOrNull { it.startsWith("data:") }
                    ?.removePrefix("data:")
                    ?.trim()
                    ?: "{}"
            } else text

            val obj = JSONObject(jsonText)
            if (obj.has("error")) {
                val err = obj.getJSONObject("error")
                throw McpException(
                    code = err.optInt("code", -1),
                    message = err.optString("message", "unknown")
                )
            }
            return obj.optJSONObject("result") ?: JSONObject()
        } finally {
            conn.disconnect()
        }
    }
}

class McpException(val code: Int, message: String) : RuntimeException("MCP error $code: $message")

/**
 * MCP 连接注册表（持久化到 SharedPreferences）。
 */
object McpRegistry {
    private const val PREFS = "float_ai_mcp_servers"

    fun loadAll(context: android.content.Context): MutableList<McpClient> {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
        val raw = prefs.getString("servers", "[]") ?: "[]"
        val arr = JSONArray(raw)
        val list = mutableListOf<McpClient>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(McpClient(
                name = o.optString("name"),
                baseUrl = o.optString("baseUrl"),
                apiKey = o.optString("apiKey").takeIf { it.isNotBlank() }
            ))
        }
        return list
    }

    fun saveAll(context: android.content.Context, list: List<McpClient>) {
        val arr = JSONArray()
        list.forEach { c ->
            arr.put(JSONObject()
                .put("name", c.name)
                .put("baseUrl", c.baseUrl)
                .put("apiKey", c.apiKey ?: ""))
        }
        context.applicationContext.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
            .edit().putString("servers", arr.toString()).apply()
    }
}
