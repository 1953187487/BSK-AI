package com.floatai.ui.screens.character

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.floatai.App
import com.floatai.data.model.Character
import com.floatai.ui.components.GlassCard
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

/**
 * 角色管理界面 v1.0.6：
 *  - 列表显示所有角色（内置 + 用户创建）
 *  - 当前激活角色标记 ✓
 *  - 点击角色切换；点击编辑进入编辑弹窗
 *  - FAB：创建新角色（AI 生成 / 手动 / 上传头像）
 */
@Composable
fun CharacterScreen(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as App
    val repo = app.characterRepository
    val chars by repo.characters.collectAsState()
    val activeId by repo.activeId.collectAsState()
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<Character?>(null) }
    var creating by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.Close, contentDescription = "关闭")
                }
                Text(
                    "角色管理",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                "为 AI 选择一个角色。每个角色都有自己的性格与开场白。",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(chars, key = { it.id }) { c ->
                    CharacterRow(
                        character = c,
                        isActive = c.id == activeId,
                        onSelect = { repo.setActive(c.id) },
                        onEdit = { editing = c },
                        onDelete = { repo.delete(c.id) }
                    )
                }
                item {
                    Spacer(Modifier.height(80.dp)) // 给 FAB 留空间
                }
            }
        }

        FloatingActionButton(
            onClick = { creating = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "新建角色")
        }
    }

    if (creating) {
        CharacterEditorDialog(
            initial = Character(
                id = System.currentTimeMillis().toString(),
                name = "",
                systemPrompt = "",
                greeting = ""
            ),
            onDismiss = { creating = false },
            onSave = { c ->
                repo.add(c)
                creating = false
            }
        )
    }
    editing?.let { c ->
        CharacterEditorDialog(
            initial = c,
            onDismiss = { editing = null },
            onSave = { updated ->
                repo.update(updated)
                editing = null
            },
            onDelete = if (!c.builtin) {
                { repo.delete(c.id); editing = null }
            } else null
        )
    }
}

@Composable
private fun CharacterRow(
    character: Character,
    isActive: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(
            containerColor = if (isActive)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 头像
            CharacterAvatar(character, size = 48.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        character.name.ifBlank { "未命名" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (isActive) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    if (character.builtin) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                "内置",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
                if (character.greeting.isNotBlank()) {
                    Text(
                        character.greeting.take(60),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = "编辑",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!character.builtin) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun CharacterAvatar(character: Character, size: androidx.compose.ui.unit.Dp) {
    if (character.avatar != null) {
        AsyncImage(
            model = character.avatar,
            contentDescription = null,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                character.name.take(1).ifBlank { "?" },
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.4f).sp
            )
        }
    }
}

@Composable
private fun CharacterEditorDialog(
    initial: Character,
    onDismiss: () -> Unit,
    onSave: (Character) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(initial.name) }
    var greeting by remember { mutableStateOf(initial.greeting) }
    var systemPrompt by remember { mutableStateOf(initial.systemPrompt) }
    var avatarUri by remember { mutableStateOf(initial.avatar) }
    var temperature by remember { mutableStateOf(initial.temperature.toString()) }
    var generating by remember { mutableStateOf(false) }
    var aiError by remember { mutableStateOf<String?>(null) }

    // 头像选择 launcher（保存到私有目录，避免 SAF content:// 失效）
    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val saved = copyAvatarToInternal(context, uri, initial.id)
                if (saved != null) avatarUri = saved
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.name.isBlank()) "新建角色" else "编辑角色：${initial.name}") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CharacterAvatar(
                        character = initial.copy(avatar = avatarUri),
                        size = 64.dp
                    )
                    Spacer(Modifier.width(12.dp))
                    OutlinedButton(onClick = { pickImage.launch("image/*") }) {
                        Icon(Icons.Filled.Image, contentDescription = null)
                        Spacer(Modifier.size(6.dp))
                        Text("上传头像")
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("名称") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = greeting, onValueChange = { greeting = it },
                    label = { Text("欢迎语（首条 assistant 消息）") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = systemPrompt, onValueChange = { systemPrompt = it },
                    label = { Text("System Prompt（发送给 AI 的角色设定）") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = temperature, onValueChange = { temperature = it },
                    label = { Text("温度 (0.0-2.0)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        if (name.isBlank() && greeting.isBlank()) return@OutlinedButton
                        generating = true
                        aiError = null
                        scope.launch {
                            try {
                                val prompt = """
请为一个 AI 角色生成设定。

需求：
- 名称：${name.ifBlank { "根据需求取一个" }}
- 简介：${greeting.ifBlank { "幽默、专业" }}

请严格按如下 JSON 输出（不要任何其它文字）：
{
  "name": "角色名（不超过 8 字）",
  "systemPrompt": "角色人设 / 性格 / 说话风格（不超过 200 字）",
  "greeting": "开场白（不超过 60 字）",
  "temperature": 0.7
}
                                """.trimIndent()
                                val cfg = (context.applicationContext as App)
                                    .settingsRepository.apiConfig.value
                                if (cfg.apiKey.isBlank() || cfg.baseUrl.isBlank()) {
                                    throw IllegalStateException("请先在「设置 → AI 配置」中配置 Base URL 与 API Key")
                                }
                                val raw = com.floatai.data.remote.OpenAiClient.chatCompletions(
                                    baseUrl = cfg.baseUrl,
                                    apiKey = cfg.apiKey,
                                    model = cfg.model.ifBlank { "auto" },
                                    messages = listOf(
                                        com.floatai.data.model.ChatMessage("user", prompt)
                                    )
                                )
                                when (raw) {
                                    is com.floatai.data.remote.ChatResult.Success -> {
                                        val s = raw.content
                                        val a = s.indexOf("{")
                                        val b = s.lastIndexOf("}")
                                        if (a >= 0 && b > a) {
                                            val obj = JSONObject(s.substring(a, b + 1))
                                            name = obj.optString("name", name)
                                            systemPrompt = obj.optString("systemPrompt", systemPrompt)
                                            greeting = obj.optString("greeting", greeting)
                                            obj.optDouble("temperature", 0.7).let {
                                                temperature = it.toString()
                                            }
                                        } else aiError = "AI 返回无法解析"
                                    }
                                    is com.floatai.data.remote.ChatResult.Error -> {
                                        aiError = raw.message
                                    }
                                }
                            } catch (e: Exception) {
                                aiError = e.message
                            } finally {
                                generating = false
                            }
                        }
                    },
                    enabled = !generating,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                    Spacer(Modifier.size(6.dp))
                    Text(if (generating) "AI 生成中…" else "AI 生成设定")
                }
                aiError?.let {
                    Text(it, fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp))
                }
            }
        },
        confirmButton = {
            Row {
                onDelete?.let {
                    TextButton(onClick = it) {
                        Text("删除", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(
                    onClick = {
                        if (name.isBlank()) return@TextButton
                        onSave(
                            initial.copy(
                                name = name.trim(),
                                greeting = greeting.trim(),
                                systemPrompt = systemPrompt.trim(),
                                avatar = avatarUri,
                                temperature = temperature.toFloatOrNull()?.coerceIn(0f, 2f) ?: 0.7f
                            )
                        )
                    }
                ) { Text("保存") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

/**
 * 把 SAF/相册返回的 Uri 拷贝到应用私有 avatars 目录，返回新文件路径字符串。
 */
private suspend fun copyAvatarToInternal(context: Context, uri: Uri, characterId: String): String? {
    val dir = File(context.filesDir, "avatars").apply { mkdirs() }
    val ext = context.contentResolver.getType(uri)?.substringAfterLast("/") ?: "jpg"
    val out = File(dir, "${characterId}.${ext.take(4)}")
    return try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(out).use { os -> input.copyTo(os) }
        }
        out.absolutePath
    } catch (_: Exception) { null }
}
