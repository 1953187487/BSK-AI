package com.floatai.ui

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Doorbell
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
    var renderEngine by remember { mutableStateOf(prefs.getString("render_engine", "compose") ?: "compose") }
    var floatEnabled by remember { mutableStateOf(prefs.getBoolean("float_enabled", false)) }
    var shizuku by remember { mutableStateOf(prefs.getBoolean("shizuku", false)) }
    var dhizuku by remember { mutableStateOf(prefs.getBoolean("dhizuku", false)) }
    var message by remember { mutableStateOf("") }
    var showUpdateNotice by remember { mutableStateOf<UpdateNotice?>(null) }
    var showPermissionNotice by remember { mutableStateOf(false) }
    var showFloatGrants by remember { mutableStateOf(false) }

    fun checkUpdate() {
        scope.launch {
            val info = UpdateChecker.checkLatest("v0.3.0")
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
            .background(Brush.verticalGradient(listOf(Color(0xFF1B0E3A), Color(0xFF0F0A1E))))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("设置", style = MaterialTheme.typography.headlineMedium, color = Color.White)

        // 分组 1: 常规设置
        Text("常规设置", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("界面主题", color = Color.White)
                        Text("液态玻璃 (v0.3)", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                    }
                    Box(
                        modifier = Modifier
                            .background(
                                Color(0xFF5B2A86),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("液态玻璃", color = Color.White, fontSize = 12.sp)
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("主题色", color = Color.White)
                        Box(
                            modifier = Modifier
                                .background(parseColorSafe(themeColor).copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .padding(top = 4.dp)
                        ) {
                            Text("预览", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
                Text("点击修改主色调，重启生效", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }

        // 分组 2: 视觉与 UI
        Text("视觉与 UI", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(
                    if (renderEngine == "flutter") "Flutter Embedding (跨端)" else "Jetpack Compose (原生)",
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text("切换引擎将重启应用", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = {
                        renderEngine = "compose"
                        prefs.edit().putString("render_engine", "compose").apply()
                        message = "正在切换 UI 引擎，应用即将重启..."
                    }, modifier = Modifier.weight(1f)) { Text("Jetpack Compose") }
                    Button(onClick = {
                        renderEngine = "flutter"
                        prefs.edit().putString("render_engine", "flutter").apply()
                        message = "正在切换 UI 引擎，应用即将重启..."
                    }, modifier = Modifier.weight(1f)) { Text("Flutter") }
                }
            }
        }

        // 分组 3: 悬浮窗控制
        Text("悬浮窗控制", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("启用悬浮窗按钮", color = Color.White)
                        Text("开启后将在其他应用上层显示快捷操作按钮", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                    }
                    Switch(
                        checked = floatEnabled,
                        onCheckedChange = { enabled ->
                            floatEnabled = enabled
                            prefs.edit().putBoolean("float_enabled", enabled).apply()
                            if (enabled) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                                    showFloatGrants = true
                                } else {
                                    message = "悬浮窗已启动"
                                }
                            } else {
                                message = "悬浮窗已关闭"
                            }
                        }
                    )
                }
            }
        }

        // 浮动权限跳转
        if (showFloatGrants) {
            AlertDialog(
                onDismissRequest = { showFloatGrants = false },
                title = { Text("需要悬浮窗权限") },
                text = { Text("FloatAI needs the Display over other apps permission to start the float window") },
                confirmButton = {
                    TextButton(onClick = {
                        showFloatGrants = false
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    }) { Text("去授权") }
                },
                dismissButton = {
                    TextButton(onClick = { showFloatGrants = false }) { Text("取消") }
                }
            )
        }

        // 分组 4: 权限管理
        Text("权限管理", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Text(if (shizuku) "\u2705" else "\u274C", color = if (shizuku) Color.Green else Color.Red, fontSize = 14.sp)
                    Text("Shizuku", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("  用于高级悬浮窗与进程查看", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Text(if (dhizuku) "\u2705" else "\u274C", color = if (dhizuku) Color.Green else Color.Red, fontSize = 14.sp)
                    Text("Dhizuku", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("  备用免 Root 权限方案", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp))
                }
                TextButton(onClick = { showPermissionNotice = true }) {
                    Text("查看权限说明", color = Color.White.copy(alpha = 0.8f))
                }
            }
        }

        // 分组 5: 系统维护
        Text("系统维护", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("检查更新", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Button(onClick = { checkUpdate() }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("点击检测") }
                Text("自动从 GitHub Release 拉取最新版本", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }

        // 分组 6: 关于
        Text("关于", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("FloatAI v0.3.0", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("开源 Android AI 悬浮助手", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                Text("MIT License", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                Button(
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:" + context.getString(R.string.contact_email))
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) { Text("发送邮件: 1953187487@qq.com") }
                Button(
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.repo_url)))
                        )
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) { Text("访问 GitHub 开源仓库") }
            }
        }

        if (message.isNotEmpty()) {
            Text(message, color = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(top = 12.dp))
        }
    }

    // 强制更新公告
    showUpdateNotice?.let { notice ->
        AlertDialog(
            onDismissRequest = { showUpdateNotice = null },
            title = { Text("发现新版本 ${notice.tag}", color = Color(0xFFFF6B6B)) },
            text = {
                Column {
                    Text("请前往 GitHub Releases 下载更新", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
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
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.repo_releases_url)))
                    )
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

private fun parseColorSafe(hex: String): Color {
    return try {
        val n = android.graphics.Color.parseColor(hex)
        Color(n)
    } catch (_: Exception) {
        Color(0xFFFF6B6B)
    }
}
