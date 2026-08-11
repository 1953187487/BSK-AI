package com.floatai.data.remote

import com.floatai.data.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

sealed class ChatResult {
    data class Success(val content: String) : ChatResult()
    data class Error(val message: String) : ChatResult()
}

/**
 * OpenAI 兼容 API 客户端。
 */
object OpenAiClient {

    suspend fun chatCompletions(
        baseUrl: String,
        apiKey: String,
        model: String,
        messages: List<ChatMessage>
    ): ChatResult = withContext(Dispatchers.IO) {
        try {
            val chatUrl = if (baseUrl.endsWith("/")) baseUrl + "chat/completions" else baseUrl + "/chat/completions"
            val conn = URL(chatUrl).openConnection() as HttpURLConnection
            conn.connectTimeout = 20_000
            conn.readTimeout = 60_000
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer $apiKey")
            conn.doOutput = true

            val body = JSONObject()
            body.put("model", if (model.isBlank() || model == "auto") "gpt-4o" else model)
            body.put("max_tokens", 2048)

            val msgs = JSONArray()
            messages.takeLast(20).forEach { m ->
                msgs.put(JSONObject().put("role", m.role).put("content", m.content))
            }
            body.put("messages", msgs)

            conn.outputStream.use { os ->
                os.write(body.toString().toByteArray(Charsets.UTF_8))
                os.flush()
            }

            val code = conn.responseCode
            val stream = if (code >= 400) conn.errorStream else conn.inputStream
            val sb = StringBuilder()
            stream?.let { input ->
                BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { br ->
                    var line = br.readLine()
                    while (line != null) {
                        sb.append(line)
                        line = br.readLine()
                    }
                }
            }
            conn.disconnect()

            if (code == 200) {
                val resp = JSONObject(sb.toString())
                val content = resp.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                ChatResult.Success(content.trim())
            } else {
                ChatResult.Error("请求失败 (HTTP $code)：${sb}")
            }
        } catch (e: Exception) {
            ChatResult.Error("网络错误：${e.message}")
        }
    }

    suspend fun fetchModels(baseUrl: String, apiKey: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val modelsUrl = if (baseUrl.endsWith("/")) baseUrl + "models" else baseUrl + "/models"
            val conn = URL(modelsUrl).openConnection() as HttpURLConnection
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            conn.setRequestProperty("Authorization", "Bearer $apiKey")
            if (conn.responseCode != 200) return@withContext emptyList()

            val sb = StringBuilder()
            BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { br ->
                var line = br.readLine()
                while (line != null) {
                    sb.append(line)
                    line = br.readLine()
                }
            }
            conn.disconnect()

            val data = JSONObject(sb.toString()).optJSONArray("data") ?: return@withContext emptyList()
            buildList {
                for (i in 0 until data.length()) {
                    val id = data.getJSONObject(i).optString("id")
                    if (id.isNotEmpty()) add(id)
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
