package com.bskai.ui.welcome

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bskai.BuildConfig
import com.bskai.ui.glass.GlassButton
import com.bskai.ui.glass.GlassPanel
import com.bskai.ui.glass.rememberGlassColors

/**
 * 已进入用户的版本更新引导：展示当前版本重点变化，一键继续，无任何强制校验。
 */
@Composable
fun WhatsNewScreen(onDone: () -> Unit) {
    val glass = rememberGlassColors()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(24.dp))
            Text(
                text = "AURA",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "已更新至 v${BuildConfig.APP_VERSION}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(18.dp))

            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Text("本次更新", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))

                WhatsNewItem("底部导航栏", "AI 聊天与设置移动至底部导航栏，顶部导航已移除，操作更顺手。")
                WhatsNewItem("液态玻璃渲染引擎", "基于开源项目 AndroidLiquidGlass（Kyant0）移植的折射与高光渲染，全局液态玻璃质感全面升级。")
                WhatsNewItem("Shizuku / Dhizuku 引导", "新增引导内授权流程；终端与 IDE 需 root 级授权后使用。")
                WhatsNewItem("模型列表刷新修复", "已下载的本地模型现在会正确刷新并显示在模型列表中。")
                WhatsNewItem("依赖下载合并", "开发工具与 IDE 的依赖下载已合并为同一流程，安装成功率提升。")
                WhatsNewItem("视频生成（Beta）", "AI 聊天新增视频生成模式：配置模型负责撰写脚本，本地模型负责生成视频。")
            }

            GlassButton(
                text = "开始体验",
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                onClick = onDone
            )
        }
    }
}

@Composable
private fun WhatsNewItem(title: String, description: String) {
    val glass = rememberGlassColors()
    GlassPanel(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(14.dp)) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = glass.accent,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = glass.content)
                Spacer(Modifier.height(2.dp))
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = glass.contentMuted
                )
            }
        }
    }
    Spacer(Modifier.height(10.dp))
}
