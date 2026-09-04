package com.bskai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bskai.ui.viewmodel.MainViewModel

data class SkillItem(
    val name: String,
    val description: String,
    val icon: ImageVector,
    val category: SkillCategory,
    val example: String
)

enum class SkillCategory {
    MEDIA, FILES, SYSTEM, COMMUNICATION, INFORMATION
}

private val skills = listOf(
    SkillItem("播放音乐", "语音控制音乐播放", Icons.Outlined.MusicNote, SkillCategory.MEDIA, "播放周杰伦的稻香"),
    SkillItem("停止音乐", "停止当前播放", Icons.Outlined.PauseCircle, SkillCategory.MEDIA, "停止播放"),
    SkillItem("下一首", "切换到下一首歌", Icons.Outlined.SkipNext, SkillCategory.MEDIA, "下一首"),
    SkillItem("上一首", "切换到上一首歌", Icons.Outlined.SkipPrevious, SkillCategory.MEDIA, "上一首"),
    SkillItem("调节音量", "设置音量大小", Icons.Outlined.VolumeUp, SkillCategory.MEDIA, "音量调到50%"),
    SkillItem("移动文件", "移动文件到其他位置", Icons.Outlined.DirectionsRun, SkillCategory.FILES, "把照片移动到相册"),
    SkillItem("复制文件", "复制文件到指定位置", Icons.Outlined.ContentCopy, SkillCategory.FILES, "复制这个文件到桌面"),
    SkillItem("删除文件", "删除指定文件", Icons.Outlined.Delete, SkillCategory.FILES, "删除这张照片"),
    SkillItem("查找文件", "搜索指定文件", Icons.Outlined.Search, SkillCategory.FILES, "找一下下载目录"),
    SkillItem("打开应用", "启动指定应用", Icons.Outlined.Apps, SkillCategory.SYSTEM, "打开微信"),
    SkillItem("切换设置", "开启/关闭系统设置", Icons.Outlined.Tune, SkillCategory.SYSTEM, "打开蓝牙"),
    SkillItem("截图", "截取当前屏幕", Icons.Outlined.Screenshot, SkillCategory.SYSTEM, "截个图"),
    SkillItem("拨打电话", "拨打联系人电话", Icons.Outlined.Call, SkillCategory.COMMUNICATION, "打电话给张三"),
    SkillItem("发送消息", "发送短信或消息", Icons.Outlined.Message, SkillCategory.COMMUNICATION, "发信息给李四说见面"),
    SkillItem("查看时间", "获取当前时间日期", Icons.Outlined.Schedule, SkillCategory.INFORMATION, "现在几点了"),
    SkillItem("查看日期", "获取当前日期", Icons.Outlined.CalendarToday, SkillCategory.INFORMATION, "今天几号"),
    SkillItem("系统状态", "获取设备状态信息", Icons.Outlined.Info, SkillCategory.INFORMATION, "我的手机状态"),
    SkillItem("你好", "打招呼问候", Icons.Outlined.EmojiObjects, SkillCategory.INFORMATION, "你好AURA")
)

@Composable
fun SkillsScreen(viewModel: MainViewModel) {
    var selectedCategory by remember { mutableStateOf(SkillCategory.MEDIA) }
    val categories = SkillCategory.entries

    Column(modifier = Modifier.fillMaxSize()) {
        // Category selector
        HorizontalDivider(color = Color(0xFF1E1E3A))
        TabRow(
            selectedTabIndex = categories.indexOf(selectedCategory),
            containerColor = Color(0xFF0D0D1A),
            contentColor = Color.White,
            divider = {}
        ) {
            categories.forEachIndexed { index, cat ->
                Tab(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    text = {
                        Text(
                            text = cat.displayName,
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                            color = if (selectedCategory == cat) Color(0xFF6C5CE7) else Color(0xFF6B6B8D)
                        )
                    },
                    selectedContentColor = Color(0xFF6C5CE7),
                    unselectedContentColor = Color(0xFF6B6B8D)
                )
            }
        }

        // Skills grid
        val filtered = skills.filter { it.category == selectedCategory }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filtered) { skill ->
                SkillCard(skill = skill)
            }
        }
    }
}

@Composable
private fun SkillCard(skill: SkillItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E3A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFF6C5CE7).copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = skill.icon,
                    contentDescription = skill.name,
                    tint = Color(0xFF6C5CE7),
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = skill.name,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                color = Color.White
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = skill.example,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF6B6B8D),
                textAlign = TextAlign.Center
            )
        }
    }
}

private val SkillCategory.displayName: String
    get() = when (this) {
        SkillCategory.MEDIA -> "媒体"
        SkillCategory.FILES -> "文件"
        SkillCategory.SYSTEM -> "系统"
        SkillCategory.COMMUNICATION -> "通讯"
        SkillCategory.INFORMATION -> "查询"
    }
