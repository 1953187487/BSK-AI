package com.bskai.agent

import android.content.Context
import com.bskai.data.AppSettings
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class ChatMsg(
    val role: String,
    val content: String
)

class LlmClient(context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun chat(s: AppSettings, messages: List<ChatMsg>): String =
        suspendCancellableCoroutine { cont ->
            val base = s.apiProviderUrl.trim().trimEnd('/')
            if (base.isEmpty()) {
                cont.resumeWithException(IllegalStateException("未配置 API 地址"))
                return@suspendCancellableCoroutine
            }
            val url = if (base.endsWith("/chat/completions")) base else "$base/chat/completions"
            val payload = JSONObject().apply {
                put("model", s.apiModel)
                val arr = JSONArray()
                messages.forEach { m ->
                    arr.put(JSONObject().put("role", m.role).put("content", m.content))
                }
                put("messages", arr)
                put("temperature", 0.6)
                put("stream", false)
            }
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer ${s.apiProviderKey}")
                .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            val call = client.newCall(request)
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: java.io.IOException) {
                    if (cont.isActive) {
                        cont.resumeWithException(e)
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use { resp ->
                        val body = resp.body?.string().orEmpty()
                        if (!resp.isSuccessful) {
                            if (cont.isActive) {
                                cont.resumeWithException(
                                    RuntimeException("服务返回异常 HTTP ${resp.code}")
                                )
                            }
                            return
                        }
                        try {
                            val json = JSONObject(body)
                            val content = json.getJSONArray("choices")
                                .getJSONObject(0)
                                .getJSONObject("message")
                                .optString("content")
                                .trim()
                            if (content.isEmpty()) {
                                cont.resumeWithException(RuntimeException("服务返回内容为空"))
                            } else if (cont.isActive) {
                                cont.resume(content)
                            }
                        } catch (e: Exception) {
                            if (cont.isActive) {
                                cont.resumeWithException(RuntimeException("解析服务响应失败"))
                            }
                        }
                    }
                }
            })
            cont.invokeOnCancellation { call.cancel() }
        }
}
