package com.floatai.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("float_ai_prefs", Context.MODE_PRIVATE) }

    var themeColor by remember { mutableStateOf(prefs.getString("theme_color", "#FF6B6B") ?: "#FF6B6B") }
    var shizuku by remember { mutableStateOf(prefs.getBoolean("shizuku", false)) }
    var dhizuku by remember { mutableStateOf(prefs.getBoolean("dhizuku", false)) }
    var message by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .background(Brush.verticalGradient(listOf(Color(0xFF2B144D), Color(0xFF0F0A1E))))
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text("设置", style = MaterialTheme.typography.headlineMedium, color = Color.White)

        GlassCard(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Column {
                Text("主题颜色", color = Color.White)
                OutlinedTextField(
                    value = themeColor,
                    onValueChange = { themeColor = it },
                    placeholder = { Text("如 #FF6B6B") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Column {
                Text("权限授权", color = Color.White, modifier = Modifier.padding(bottom = 8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = shizuku, onCheckedChange = { shizuku = it })
                    Text("授权 Shizuku（进程查看）", color = Color.White)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = dhizuku, onCheckedChange = { dhizuku = it })
                    Text("授权 Dhizuku", color = Color.White)
                }
            }
        }

        Button(
            onClick = {
                prefs.edit()
                    .putString("theme_color", themeColor.trim())
                    .putBoolean("shizuku", shizuku)
                    .putBoolean("dhizuku", dhizuku)
                    .apply()
                message = "设置已保存"
            },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) { Text("保存设置") }

        if (message.isNotEmpty()) {
            Text(
                message,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        GlassCard(modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
            Column {
                Text("关于", color = Color.White)
                Button(
                    onClick = {
                        context.startActivity(Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(context.getString(R.string.repo_releases_url))
                        ))
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                ) { Text("检查更新") }
                Button(
                    onClick = {
                        context.startActivity(Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(context.getString(R.string.repo_url))
                        ))
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) { Text("打开开源仓库") }
                Text(
                    "FloatAI v0.1 · MIT License · Android 8~14",
                    color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
