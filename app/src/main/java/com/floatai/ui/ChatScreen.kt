package com.floatai.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Doorbell
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

data class ChatMessage(val role: String, val content: String, val timestamp: Long = System.currentTimeMillis())
data class ChatHistory(val id: String, val title: String, val messages: List<ChatMessage>, val time: Long)

@Composable
fun ChatScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("float_ai_prefs", Context.MODE_PRIVATE) }

    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var input by remember { mutableStateOf("") }
    var selectedModel by remember {
        mutableStateOf(prefs.getString("selected_model", prefs.getString("api_model", "gpt-4o") ?: "gpt-4o") ?: "auto")
    }
    var loading by remember { mutableStateOf(false) }
    var showDropdown by remember { mutableStateOf(false) }
    var expandedDropdown by remember { mutableStateOf(false) }

    val savedUrl = prefs.getString("api_url", "") ?: ""
    val savedKey = prefs.getString("api_key", "") ?: ""

    Column(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1B0E3A), Color(0xFF0F0A1E))
                )
            )
            .fillMaxSize()
    ) {
        // 顶部标题栏
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "FloatAI 助手",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )
            Column(horizontalAlignment = Alignment.End) {
                Box {
                    TextButton(onClick = { expandedDropdown = !expandedDropdown }) {
                        Text("模型: $selectedModel", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    }
                    DropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false }
                    ) {
                        val common = listOf(
                            "gpt-4o", "gpt-4o-mini", "gpt-3.5-turbo",
                            "deepseek-chat", "deepseek-reasoner",
                            "claude-3-5-sonnet", "gemini-pro", "qwen-max", "glm-4"
                        )
                        common.forEach { model ->
                            DropdownMenuItem(
                                text = { Text(model) },
                                onClick = {
                                    selectedModel = model
                                    prefs.edit().putString("selected_model", model).apply()
                                    expandedDropdown = false
                                }
                            )
                        }
                    }
                }
                if (selectedModel.isEmpty()) {
                    Text("未选择", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
                }
            }
        }

        // 消息列表
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "开始和 AI 对话吧",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            items(messages) { msg ->
                ChatBubble(msg)
            }
        }

        // 底部输入框
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = {
                    Text(
                        "输入消息，按回车发送...",
                        color = Color.White.copy(alpha = 0.35f)
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                maxLines = 4,
                enabled = !loading
            )
            IconButton(
                onClick = {
                    if (input.isBlank() || loading) return@IconButton
                    if (savedUrl.isEmpty() || savedKey.isEmpty()) {
                        messages = messages + ChatMessage("assistant", "请先在 API 配置页填写服务商地址和密钥")
                        return@IconButton
                    }
                    val userMsg = input.trim()
                    input = ""
                    messages = messages + ChatMessage("user", userMsg)
                    loading = true
                    scope.launch {
                        val reply = withContext(Dispatchers.IO) {
                            chatCompletions(savedUrl, savedKey, selectedModel, messages)
                        }
                        messages = messages + ChatMessage("assistant", reply)
                        loading = false
                    }
                },
                enabled = !loading
            ) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubble(msg: ChatMessage) {
    val isUser = msg.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .background(
                    if (isUser) Color(0xFF6C3BA8) else Color.White.copy(alpha = 0.1f),
                    RoundedCornerShape(16.dp)
                )
                .padding(12.dp)
                .fillMaxWidth(0.8f)
        ) {
            Text(msg.content, color = Color.White, fontSize = 14.sp)
        }
    }
}

private fun chatCompletions(url: String, key: String, model: String, history: List<ChatMessage>): String {
    return try {
        val chatUrl = if (url.endsWith("/")) url + "chat/completions" else url + "/chat/completions"
        val conn = URL(chatUrl).openConnection() as HttpURLConnection
        conn.connectTimeout = 20000
        conn.readTimeout = 60000
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Authorization", "Bearer $key")
        conn.doOutput = true

        val body = JSONObject()
        body.put("model", if (model == "auto") "gpt-4o" else model)
        val msgs = JSONArray()
        history.takeLast(20).forEach { m ->
            val obj = JSONObject()
            obj.put("role", if (m.role == "assistant") "assistant" else "user")
            obj.put("content", m.content)
            msgs.put(obj)
        }
        val last = history.lastOrNull() ?: ChatMessage("", "Hello")
        msgs.put(JSONObject().put("role", "user").put("content", last.content))
        body.put("messages", msgs)
        body.put("max_tokens", 2048)

        val os = conn.outputStream
        os.write(body.toString().toByteArray(StandardCharsets.UTF_8))
        os.flush()
        os.close()

        val code = conn.responseCode
        val input = if (code >= 400) conn.errorStream else conn.inputStream
        val sb = StringBuilder()
        BufferedReader(InputStreamReader(input)).use { br ->
            var line = br.readLine()
            while (line != null) { sb.append(line); line = br.readLine() }
        }
        conn.disconnect()

        if (code == 200) {
            val resp = JSONObject(sb.toString())
            resp.getJSONArray("choices").getJSONObject(0)
                .getJSONObject("message").getString("content").trim()
        } else {
            "请求失败 (HTTP $code)：$sb"
        }
    } catch (e: Exception) {
        "错误：${e.message}"
    }
}
