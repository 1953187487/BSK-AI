package com.bskai.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * 模型服务连接客户端：拉取模型列表、测试连通性。
 */
object ModelsClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun fetchModels(baseUrl: String, apiKey: String): List<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = baseUrl.trimEnd('/') + "/models"
                val request = Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer $apiKey")
                    .build()
                client.newCall(request).execute().use { resp ->
                    if (!resp.isSuccessful) return@use emptyList()
                    val body = resp.body?.string() ?: return@use emptyList()
                    val data = JSONObject(body).optJSONArray("data") ?: return@use emptyList()
                    buildList {
                        for (i in 0 until data.length()) {
                            val id = data.getJSONObject(i).optString("id")
                            if (id.isNotEmpty()) add(id)
                        }
                    }
                }
            }.getOrDefault(emptyList())
        }

    suspend fun testConnection(baseUrl: String, apiKey: String, model: String): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = baseUrl.trimEnd('/') + "/chat/completions"
                val body = JSONObject()
                    .put("model", model.ifBlank { "auto" })
                    .put("messages", org.json.JSONArray().put(JSONObject().put("role", "user").put("content", "ping")))
                    .put("max_tokens", 4)
                val request = Request.Builder()
                    .url(url)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer $apiKey")
                    .post(body.toString().toRequestBody())
                    .build()
                client.newCall(request).execute().use { resp -> resp.isSuccessful }
            }.getOrDefault(false)
        }

    private fun String.toRequestBody(): okhttp3.RequestBody =
        okhttp3.RequestBody.create(
            "application/json; charset=utf-8".toMediaType(),
            this
        )
}
