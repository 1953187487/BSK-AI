package com.floatai.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("float_ai_prefs", Context.MODE_PRIVATE) }

    var themeColor by remember { mutableStateOf(prefs.getString("theme_color", "#FF6B6B") ?: "#FF6B6B") }
    var shizuku by remember { mutableStateOf(prefs.getBoolean("shizuku", false)) }
    var dhizuku by remember { mutableStateOf(prefs.getBoolean("dhizuku", false)) }
    var message by remember { mutableStateOf("") }
    var showUpdateNotice by remember { mutableStateOf<UpdateNotice?>(null) }
    var showPermissionNotice by remember { mutableStateOf(false) }

    // 设置页检查更新（不跳浏览器，直接在应用内显示）
    fun checkUpdate() {
        scope.launch {
            val info = UpdateChecker.checkLatest("v" + try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            } catch (_: Exception) { "0.2" })
            if (info.latestTag.isEmpty()) {
                message = info.changelog
            } else if (info.isNewer) {
                showUpdateNotice = UpdateNotice(info.latestTag, info.changelog)
            } else {
                message = "当前版本已是最新 (${info.latestTag})"
            }
        }
    }

    Column(
        modifier = modifier
            .background(Brush.verticalGradient(listOf(Color(0xFF2B144D), Color(0xFF0F0A1E))))
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text("设置", style = MaterialTheme.typography.headlineMedium, color = Color.White)

        // 从下到上排列按钮（但 UI 上从上到下显示）

        // 4. UI 设置
        GlassCard(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Icon(Icons.Filled.PhoneAndroid, contentDescription = null, tint = Color.White)
                    Text("  UI 设置", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    "FloatAI v0.2 · Jetpack Compose · 液态玻璃主题",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    "支持 Android 8~14 (API 26~34)，四核设备可运行",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
            }
        }

        // 3. 主题（含颜色 + 导入字体）
        GlassCard(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Icon(Icons.Filled.ColorLens, contentDescription = null, tint = Color.White)
                    Text("  主题", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Text("主题颜色", color = Color.White.copy(alpha = 0.8f), modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                OutlinedTextField(
                    value = themeColor, onValueChange = { themeColor = it },
                    placeholder = { Text("如 #FF6B6B") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("字体", color = Color.White.copy(alpha = 0.8f), modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                Button(onClick = { message = "字体导入需文件系统访问，建议放入 assets/fonts/" }, modifier = Modifier.fillMaxWidth()) {
                    Text("导入字体 (使用 assets/fonts)")
                }
                Text(
                    "主题设置保存后下次启动生效",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        // 权限设置（Shizuku / Dhizuku）
        GlassCard(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Column {
                Text("权限授权", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    Checkbox(checked = shizuku, onCheckedChange = { shizuku = it })
                    Text("授权 Shizuku（进程查看）", color = Color.White)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = dhizuku, onCheckedChange = { dhizuku = it })
                    Text("授权 Dhizuku", color = Color.White)
                }
                TextButton(onClick = { showPermissionNotice = true }) {
                    Text("查看权限说明", color = Color.White.copy(alpha = 0.8f))
                }
            }
        }

        // 保存按钮
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
            Text(message, color = Color.White.copy(alpha = 0.8f), modifier = Modifier.padding(top = 8.dp))
        }

        // 2. 检查更新（倒数第二）
        GlassCard(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Icon(Icons.Filled.Refresh, contentDescription = null, tint = Color.White)
                    Text("  检查更新", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { checkUpdate() },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                ) { Text("检查更新") }
                Text(
                    "自动从 GitHub Release 获取版本更新",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // 1. 关于（最底，含邮箱）
        GlassCard(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Icon(Icons.Filled.Description, contentDescription = null, tint = Color.White)
                    Text("  关于", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Text("FloatAI", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
                Text("开源 Android AI 悬浮助手 · MIT License", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                Text("v0.2 · Jetpack Compose · 液态玻璃", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                Button(
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:" + context.getString(R.string.contact_email))
                        })
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                ) { Text("联系邮箱：${context.getString(R.string.contact_email)}") }
                Button(
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.repo_url))))
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) { Text("打开开源仓库") }
            }
        }
    }

    // 强制更新公告弹窗
    showUpdateNotice?.let { notice ->
        AlertDialog(
            onDismissRequest = { showUpdateNotice = null },
            title = { Text("发现新版本 ${notice.tag}", color = Color(0xFFFF6B6B)) },
            text = {
                Column {
                    Text("请前往 GitHub Releases 下载更新：", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                    Text(
                        notice.changelog.ifEmpty { "无更新说明" },
                        color = Color.White,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.repo_releases_url))))
                    showUpdateNotice = null
                }) { Text("前往下载") }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateNotice = null }) { Text("稍后") }
            }
        )
    }

    if (showPermissionNotice) {
        AlertDialog(
            onDismissRequest = { showPermissionNotice = false },
            title = { Text("权限说明") },
            text = {
                Text(
                    context.getString(R.string.permission_notice),
                    fontSize = 12.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showPermissionNotice = false }) { Text("知道了") }
            }
        )
    }
}

data class UpdateNotice(val tag: String, val changelog: String)
