package com.floatai.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
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

@Composable
fun ApiScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("float_ai_prefs", Context.MODE_PRIVATE) }

    var apiUrl by remember { mutableStateOf(prefs.getString("api_url", "") ?: "") }
    var apiKey by remember { mutableStateOf(prefs.getString("api_key", "") ?: "") }
    var apiModel by remember { mutableStateOf(prefs.getString("api_model", "") ?: "") }
    var result by remember { mutableStateOf("配置后点击测试模型验证连通性") }
    var testing by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .background(Brush.verticalGradient(listOf(Color(0xFF2B144D), Color(0xFF0F0A1E))))
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text("API 配置", style = MaterialTheme.typography.headlineMedium, color = Color.White)
        Text(
            "支持所有 OpenAI 兼容的 AI 服务商",
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 4.dp)
        )

        GlassCard(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Column {
                Text("服务商 Base URL", color = Color.White, modifier = Modifier.padding(bottom = 8.dp))
                OutlinedTextField(
                    value = apiUrl,
                    onValueChange = { apiUrl = it },
                    placeholder = { Text("如 https://api.openai.com/v1") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("API Key", color = Color.White, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    placeholder = { Text("sk-...") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                Text("模型名称", color = Color.White, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
                OutlinedTextField(
                    value = apiModel,
                    onValueChange = { apiModel = it },
                    placeholder = { Text("如 gpt-4o / deepseek-chat") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Button(
                onClick = {
                    if (apiUrl.isEmpty() || apiKey.isEmpty() || apiModel.isEmpty()) {
                        result = "请填写 Base URL、API Key 和模型名称"
                        return@Button
                    }
                    testing = true
                    result = "测试中，请稍候..."
                    scope.launch {
                        result = withContext(Dispatchers.IO) {
                            testModel(apiUrl, apiKey, apiModel)
                        }
                        testing = false
                    }
                },
                enabled = !testing,
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            ) { Text("测试模型") }
            Button(
                onClick = {
                    prefs.edit()
                        .putString("api_url", apiUrl.trim())
                        .putString("api_key", apiKey.trim())
                        .putString("api_model", apiModel.trim())
                        .apply()
                    result = "配置已保存"
                },
                modifier = Modifier.weight(1f).padding(start = 8.dp)
            ) { Text("保存配置") }
        }

        GlassCard(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Text(result, color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
        }
    }
}

private fun testModel(url: String, key: String, model: String): String {
    return try {
        val chatUrl = if (url.endsWith("/")) url + "chat/completions" else url + "/chat/completions"
        val conn = URL(chatUrl).openConnection() as HttpURLConnection
        conn.connectTimeout = 15000
        conn.readTimeout = 30000
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Authorization", "Bearer $key")
        conn.doOutput = true

        val body = JSONObject()
        body.put("model", model)
        val messages = JSONArray()
        val msg = JSONObject()
        msg.put("role", "user")
        msg.put("content", "你好，请回复：连接成功")
        messages.put(msg)
        body.put("messages", messages)

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
            val reply = resp.getJSONArray("choices").getJSONObject(0)
                .getJSONObject("message").getString("content")
            "连接成功！模型回复：${reply.trim()}"
        } else {
            "请求失败 (HTTP $code)：$sb"
        }
    } catch (e: Exception) {
        "测试失败：${e.message}"
    }
}
