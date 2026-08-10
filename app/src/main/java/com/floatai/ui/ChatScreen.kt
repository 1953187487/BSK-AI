package com.floatai.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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

data class ChatMessage(val role: String, val content: String)

@Composable
fun ChatScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("float_ai_prefs", Context.MODE_PRIVATE) }

    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var input by remember { mutableStateOf("") }
    var selectedModel by remember {
        mutableStateOf(prefs.getString("selected_model", prefs.getString("api_model", "gpt-4o") ?: "gpt-4o") ?: "gpt-4o")
    }
    var loading by remember { mutableStateOf(false) }
    var showModelPicker by remember { mutableStateOf(false) }

    val savedUrl = prefs.getString("api_url", "") ?: ""
    val savedKey = prefs.getString("api_key", "") ?: ""

    Column(
        modifier = modifier
            .background(Brush.verticalGradient(listOf(Color(0xFF2B144D), Color(0xFF0F0A1E))))
            .fillMaxSize()
    ) {
        // 顶部标题栏 + 模型切换
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("AI 聊天", style = MaterialTheme.typography.headlineMedium, color = Color.White)
            TextButton(onClick = { showModelPicker = true }) {
                Text("模型: $selectedModel", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
            }
        }

        // 消息列表
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Text(
                        "开始和 AI 对话吧",
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 40.dp).align(Alignment.CenterHorizontally)
                    )
                }
            }
            items(messages) { msg ->
                ChatBubble(msg)
            }
        }

        // 输入框
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("输入消息...", color = Color.White.copy(alpha = 0.4f)) },
                modifier = Modifier.weight(1f),
                maxLines = 4
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
                    Icon(Icons.Filled.Send, contentDescription = "发送", tint = Color.White)
                }
            }
        }
    }

    if (showModelPicker) {
        ModelPickerDialog(
            onDismiss = { showModelPicker = false },
            onSelect = { model ->
                selectedModel = model
                prefs.edit().putString("selected_model", model).apply()
                showModelPicker = false
            }
        )
    }
}

@Composable
fun ChatBubble(msg: ChatMessage) {
    val isUser = msg.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .background(
                    if (isUser) Color(0xFF6C3BA8)
                    else Color.White.copy(alpha = 0.1f),
                    RoundedCornerShape(16.dp)
                )
                .padding(12.dp)
                .fillMaxWidth(0.75f)
        ) {
            Text(msg.content, color = Color.White, fontSize = 14.sp)
        }
    }
}

@Composable
fun ModelPickerDialog(onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    val commonModels = listOf(
        "gpt-4o", "gpt-4o-mini", "gpt-3.5-turbo",
        "deepseek-chat", "deepseek-reasoner",
        "claude-3-5-sonnet", "gemini-pro", "qwen-max", "glm-4"
    )
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择模型") },
        text = {
            Column {
                commonModels.forEach { model ->
                    TextButton(onClick = { onSelect(model) }, modifier = Modifier.fillMaxWidth()) {
                        Text(model)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("自定义模型") }
        }
    )
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
        body.put("model", model)
        val messages = JSONArray()
        history.takeLast(20).forEach { m ->
            val msg = JSONObject()
            msg.put("role", if (m.role == "assistant") "assistant" else "user")
            msg.put("content", m.content)
            messages.put(msg)
        }
        messages.put(JSONObject().put("role", "user").put("content", history.lastOrNull()?.content ?: ""))
        body.put("messages", messages)
        body.put("max_tokens", 2048)

        val os: OutputStream = conn.outputStream
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
