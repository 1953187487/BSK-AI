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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectTapGestures
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun ApiScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("float_ai_prefs", Context.MODE_PRIVATE) }

    var apiUrl by remember { mutableStateOf(prefs.getString("api_url", "") ?: "") }
    var apiKey by remember { mutableStateOf(prefs.getString("api_key", "") ?: "") }
    var apiModel by remember { mutableStateOf(prefs.getString("api_model", "") ?: "") }
    var models by remember { mutableStateOf<List<String>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf("点击\"获取模型\"从服务商拉取可用模型列表") }
    var showCustomDialog by remember { mutableStateOf(false) }
    var customModel by remember { mutableStateOf("") }

    val commonModels = listOf(
        "gpt-4o", "gpt-4o-mini", "gpt-3.5-turbo",
        "deepseek-chat", "deepseek-reasoner",
        "claude-3-5-sonnet", "gemini-pro", "qwen-max", "glm-4"
    )

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
                    value = apiUrl, onValueChange = { apiUrl = it },
                    placeholder = { Text("如 https://api.openai.com/v1") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Text("API Key", color = Color.White, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
                OutlinedTextField(
                    value = apiKey, onValueChange = { apiKey = it },
                    placeholder = { Text("sk-...") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Button(
                onClick = {
                    if (apiUrl.isEmpty() || apiKey.isEmpty()) {
                        result = "请先填写 Base URL 和 API Key"
                        return@Button
                    }
                    loading = true
                    result = "正在获取模型列表..."
                    scope.launch {
                        val list = withContext(Dispatchers.IO) { fetchModels(apiUrl, apiKey) }
                        loading = false
                        models = list
                        result = if (list.isEmpty()) "未能获取到模型，请检查地址和密钥" else "获取到 ${list.size} 个模型"
                    }
                },
                enabled = !loading,
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            ) {
                if (loading) CircularProgressIndicator(modifier = Modifier.height(20.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("获取模型")
            }
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

        // 当前模型
        GlassCard(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Column {
                Text("当前模型", color = Color.White)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {},
                                onLongPress = { showCustomDialog = true }
                            )
                        }
                        .padding(12.dp)
                ) {
                    Text(
                        if (apiModel.isEmpty()) "未选择 (长按输入自定义模型)" else apiModel,
                        color = Color.White, fontSize = 16.sp
                    )
                }
                Text("长按输入自定义模型", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }

        // 已获取的模型列表
        if (models.isNotEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Column {
                    Text("可用模型 (${models.size})", color = Color.White)
                    models.forEach { m ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(
                                    if (m == apiModel) Color(0xFF6C3BA8).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.06f),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    apiModel = m
                                    prefs.edit().putString("api_model", m).apply()
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(m, color = Color.White, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // 常用模型（长按选择）
        GlassCard(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Column {
                Text("常用模型（点击选择）", color = Color.White)
                commonModels.chunked(2).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        row.forEach { m ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp)
                                    .background(
                                        if (m == apiModel) Color(0xFF4ECDC4).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.08f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        apiModel = m
                                        prefs.edit().putString("api_model", m).apply()
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(m, color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // 结果提示
        GlassCard(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Text(result, color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
        }
    }

    if (showCustomDialog) {
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = { Text("自定义模型") },
            text = {
                OutlinedTextField(
                    value = customModel, onValueChange = { customModel = it },
                    placeholder = { Text("输入模型名称") }, singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (customModel.isNotBlank()) {
                        apiModel = customModel.trim()
                        prefs.edit().putString("api_model", apiModel).apply()
                        customModel = ""
                    }
                    showCustomDialog = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false }) { Text("取消") }
            }
        )
    }
}

private fun fetchModels(url: String, key: String): List<String> {
    return try {
        val modelsUrl = if (url.endsWith("/")) url + "models" else url + "/models"
        val conn = URL(modelsUrl).openConnection() as HttpURLConnection
        conn.connectTimeout = 15000
        conn.readTimeout = 30000
        conn.setRequestProperty("Authorization", "Bearer $key")
        val code = conn.responseCode
        if (code != 200) return emptyList()
        val sb = StringBuilder()
        BufferedReader(InputStreamReader(conn.inputStream)).use { br ->
            var line = br.readLine()
            while (line != null) { sb.append(line); line = br.readLine() }
        }
        conn.disconnect()
        val json = JSONObject(sb.toString())
        val data = json.optJSONArray("data") ?: return emptyList()
        val list = mutableListOf<String>()
        for (i in 0 until data.length()) {
            val id = data.getJSONObject(i).optString("id")
            if (id.isNotEmpty()) list.add(id)
        }
        list
    } catch (e: Exception) {
        emptyList()
    }
}
