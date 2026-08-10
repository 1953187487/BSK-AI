package com.floatai.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

@Composable
fun ChatScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var processText by remember { mutableStateOf("进程查看：授权 Shizuku / Dhizuku 后可获取系统进程信息") }
    var version by remember { mutableStateOf("v0.1") }
    var floatEnabled by remember { mutableStateOf(false) }

    try {
        version = "v" + context.packageManager.getPackageInfo(context.packageName, 0).versionName
    } catch (_: PackageManager.NameNotFoundException) {}

    Column(
        modifier = modifier
            .background(
                Brush.verticalGradient(listOf(Color(0xFF2B144D), Color(0xFF0F0A1E)))
            )
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text("AI 聊天", style = MaterialTheme.typography.headlineMedium, color = Color.White)

        GlassCard(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("悬浮窗", color = Color.White, fontSize = 18.sp)
                        Text(
                            "可开关的 AI 悬浮窗，随时查看进程",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp
                        )
                    }
                    Switch(
                        checked = floatEnabled,
                        onCheckedChange = { checked ->
                            floatEnabled = checked
                            if (checked) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                                    !Settings.canDrawOverlays(context)
                                ) {
                                    floatEnabled = false
                                    context.startActivity(Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    ))
                                } else {
                                    context.startForegroundService(Intent(context, FloatService::class.java))
                                }
                            } else {
                                context.stopService(Intent(context, FloatService::class.java))
                            }
                        }
                    )
                }
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Column {
                Text("系统进程", color = Color.White, fontSize = 18.sp)
                Button(
                    onClick = {
                        scope.launch {
                            processText = withContext(Dispatchers.IO) { readProcesses() }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                ) { Text("查看进程") }
                Text(
                    processText,
                    color = Color.White.copy(alpha = 0.8f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 20.dp),
            color = Color.White.copy(alpha = 0.1f)
        )

        Text("当前版本 $version", color = Color.White.copy(alpha = 0.6f))
        Button(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.repo_releases_url)))
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) { Text("检查更新") }
    }
}

private fun readProcesses(): String {
    val sb = StringBuilder()
    try {
        val p = Runtime.getRuntime().exec("ps -A")
        val br = BufferedReader(InputStreamReader(p.inputStream))
        var line: String? = br.readLine()
        var count = 0
        while (line != null && count < 30) {
            sb.append(line).append("\n")
            line = br.readLine()
            count++
        }
        br.close()
    } catch (e: Exception) {
        return "无权限读取进程，请授予 Shizuku / Dhizuku 权限"
    }
    return if (sb.isNotEmpty()) sb.toString() else "无进程信息"
}
