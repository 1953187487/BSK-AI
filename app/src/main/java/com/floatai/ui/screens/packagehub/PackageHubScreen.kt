package com.floatai.ui.screens.packagehub

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.floatai.data.remote.ApiClient
import com.floatai.data.SettingsRepository
import com.floatai.ui.i18n.localStrings
import com.floatai.ui.screens.mcp.McpScreen
import com.floatai.ui.screens.packages.PackagesScreen
import kotlinx.coroutines.launch
import org.json.JSONObject

private enum class HubTab(
    val title: String,
    val icon: ImageVector
) {
    Packages("包管理", Icons.Filled.Storage),
    Plugins("插件", Icons.Filled.Extension),
    Skills("技能", Icons.Filled.AutoAwesome),
    Mcp("MCP 服务", Icons.Filled.SmartToy);
}

/**
 * Package Hub v1.0.4：
 *  - Packages: 复用 PackageRegistry
 *  - Plugins: 本地持久化（filesDir/plugins.json），支持 AI 生成（用本应用配置 API 调用）
 *  - Skills: 本地持久化（filesDir/skills.json），支持 AI 生成
 *  - Mcp: 复用 McpRegistry
 */
@Composable
fun PackageHubScreen() {
    var tabIndex by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            TabRow(tabIndex = tabIndex, onTabSelected = { tabIndex = it })
            Box(modifier = Modifier.fillMaxSize()) {
                when (HubTab.entries[tabIndex]) {
                    HubTab.Packages -> PackagesScreen()
                    HubTab.Plugins -> PluginsScreen()
                    HubTab.Skills -> SkillsScreen()
                    HubTab.Mcp -> McpScreen()
                }
            }
        }
    }
}

@Composable
private fun TabRow(tabIndex: Int, onTabSelected: (Int) -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    val tabs = HubTab.entries
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            tabs.forEachIndexed { i, tab ->
                val selected = i == tabIndex
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTabSelected(i) }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        tab.icon,
                        contentDescription = tab.title,
                        tint = if (selected) primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        tab.title,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    val animProgress by animateFloatAsState(
                        targetValue = if (selected) 1f else 0f,
                        animationSpec = tween(220),
                        label = "tab-indicator"
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(3.dp)
                            .background(
                                color = primary.copy(alpha = animProgress),
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                }
            }
        }
    }
}

// ───────────────────── Plugins ─────────────────────
private data class PluginItem(
    val id: String,
    val name: String,
    val description: String,
    val source: String,    // user / ai
    val createdAt: Long
)

@Composable
private fun PluginsScreen() {
    val context = LocalContext.current
    val items = remember { mutableStateListOf<PluginItem>().apply { addAll(loadPlugins(context)) } }
    var showCreate by remember { mutableStateOf(false) }
    var showAiGen by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (items.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Extension,
                title = "暂无插件",
                desc = "点击右下角 + 创建，或用 AI 自动生成"
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items, key = { it.id }) { p ->
                    ItemCard(
                        title = p.name,
                        subtitle = p.description,
                        badge = if (p.source == "ai") "AI" else "用户",
                        onDelete = {
                            items.remove(p)
                            savePlugins(context, items)
                        }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { showCreate = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)
        ) { Icon(Icons.Filled.Add, contentDescription = "新建插件") }
    }

    if (showCreate) {
        CreateItemDialog(
            title = "新建插件",
            onDismiss = { showCreate = false },
            onConfirm = { name, desc ->
                items.add(
                    PluginItem(
                        id = System.currentTimeMillis().toString(),
                        name = name,
                        description = desc,
                        source = "user",
                        createdAt = System.currentTimeMillis()
                    )
                )
                savePlugins(context, items)
                showCreate = false
            },
            extraButton = "用 AI 生成" to { showAiGen = true }
        )
    }
    if (showAiGen) {
        AiGenDialog(
            kind = "plugin",
            onDismiss = { showAiGen = false; showCreate = false },
            onGenerated = { name, desc ->
                items.add(
                    PluginItem(
                        id = System.currentTimeMillis().toString(),
                        name = name,
                        description = desc,
                        source = "ai",
                        createdAt = System.currentTimeMillis()
                    )
                )
                savePlugins(context, items)
                showAiGen = false
                showCreate = false
            }
        )
    }
}

// ───────────────────── Skills ─────────────────────
private data class SkillItem(
    val id: String,
    val name: String,
    val description: String,
    val trigger: String,
    val source: String,
    val createdAt: Long
)

@Composable
private fun SkillsScreen() {
    val context = LocalContext.current
    val items = remember { mutableStateListOf<SkillItem>().apply { addAll(loadSkills(context)) } }
    var showCreate by remember { mutableStateOf(false) }
    var showAiGen by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (items.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.AutoAwesome,
                title = "暂无技能",
                desc = "技能是一段可被 AI 触发的 prompt 模板"
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items, key = { it.id }) { s ->
                    ItemCard(
                        title = s.name,
                        subtitle = "${s.description}\n触发词：${s.trigger}",
                        badge = if (s.source == "ai") "AI" else "用户",
                        onDelete = {
                            items.remove(s)
                            saveSkills(context, items)
                        }
                    )
                }
            }
        }
        FloatingActionButton(
            onClick = { showCreate = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)
        ) { Icon(Icons.Filled.Add, contentDescription = "新建技能") }
    }

    if (showCreate) {
        var name by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        var trigger by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("新建技能") },
            text = {
                Column {
                    OutlinedTextField(value = name, onValueChange = { name = it },
                        label = { Text("名称") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = trigger, onValueChange = { trigger = it },
                        label = { Text("触发词 (如 /翻译)") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                    OutlinedTextField(value = desc, onValueChange = { desc = it },
                        label = { Text("prompt 模板") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                }
            },
            confirmButton = {
                Row {
                    TextButton(onClick = { showAiGen = true }) { Text("AI 生成") }
                    TextButton(onClick = {
                        if (name.isNotBlank()) {
                            items.add(
                                SkillItem(
                                    id = System.currentTimeMillis().toString(),
                                    name = name, description = desc,
                                    trigger = trigger, source = "user",
                                    createdAt = System.currentTimeMillis()
                                )
                            )
                            saveSkills(context, items)
                            showCreate = false
                        }
                    }) { Text("保存") }
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreate = false }) { Text("取消") }
            }
        )
    }
    if (showAiGen) {
        AiGenDialog(
            kind = "skill",
            onDismiss = { showAiGen = false },
            onGenerated = { name, desc ->
                // AI 生成技能：trigger 用 "ai:" + 时间戳作默认
                items.add(
                    SkillItem(
                        id = System.currentTimeMillis().toString(),
                        name = name,
                        description = desc,
                        trigger = "/$name",
                        source = "ai",
                        createdAt = System.currentTimeMillis()
                    )
                )
                saveSkills(context, items)
                showAiGen = false
                showCreate = false
            }
        )
    }
}

@Composable
private fun EmptyState(icon: ImageVector, title: String, desc: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(80.dp))
        Icon(icon, contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(desc, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ItemCard(
    title: String,
    subtitle: String,
    badge: String,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(badge, fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CreateItemDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, desc: String) -> Unit,
    extraButton: Pair<String, () -> Unit>? = null
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("名称") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = desc, onValueChange = { desc = it },
                    label = { Text("描述") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            }
        },
        confirmButton = {
            Row {
                extraButton?.let { (label, action) ->
                    TextButton(onClick = action) { Text(label) }
                }
                TextButton(onClick = {
                    if (name.isNotBlank()) onConfirm(name, desc)
                }) { Text("保存") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

/**
 * AI 生成插件/技能描述：调用本应用配置的 OpenAI 兼容 API，
 * 用户输入需求，AI 返回 (name, description)。
 *
 * 注：如果未配置 API Key，回退为「用 AI 生成」按钮显示提示并不调用。
 */
@Composable
private fun AiGenDialog(
    kind: String,
    onDismiss: () -> Unit,
    onGenerated: (name: String, description: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var requirement by remember { mutableStateOf("") }
    var generating by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var previewName by remember { mutableStateOf("") }
    var previewDesc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("用 AI 生成${if (kind == "plugin") "插件" else "技能"}") },
        text = {
            Column {
                Text(
                    "用一句话描述你想要的${if (kind == "plugin") "插件" else "技能"}，" +
                        "AI 会基于本应用配置的 OpenAI 兼容 API 生成名称与描述。",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = requirement,
                    onValueChange = { requirement = it },
                    label = { Text("需求描述") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                if (previewName.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("预览：$previewName", style = MaterialTheme.typography.bodyMedium)
                    Text(previewDesc, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                error?.let {
                    Text(it, fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp))
                }
            }
        },
        confirmButton = {
            Row {
                if (previewName.isEmpty()) {
                    TextButton(
                        onClick = {
                            if (requirement.isBlank()) return@TextButton
                            generating = true
                            error = null
                                scope.launch {
                                try {
                                    val prompt = """
请为一个 Android AI 助手应用设计一个${if (kind == "plugin") "插件" else "AI 技能"}。
用户需求：$requirement

请严格按如下 JSON 输出（不要任何其它文字）：
{
  "name": "简短名称（不超过 12 字）",
  "description": "描述这个${if (kind == "plugin") "插件" else "技能"}做什么、用什么触发（不超过 80 字）"
}
                                    """.trimIndent()
                                    val cfg = (context.applicationContext as? com.floatai.App)
                                        ?.settingsRepository?.apiConfig?.value
                                    if (cfg == null || cfg.apiKey.isBlank() || cfg.baseUrl.isBlank()) {
                                        throw IllegalStateException("请先在「设置 → AI 配置」中配置 Base URL 与 API Key")
                                    }
                                    val result = ApiClient.simpleChat(
                                        baseUrl = cfg.baseUrl,
                                        apiKey = cfg.apiKey,
                                        model = cfg.model.ifBlank { "auto" },
                                        prompt = prompt
                                    )
                                    val jsonStart = result.indexOf("{")
                                    val jsonEnd = result.lastIndexOf("}")
                                    if (jsonStart >= 0 && jsonEnd > jsonStart) {
                                        val obj = JSONObject(result.substring(jsonStart, jsonEnd + 1))
                                        previewName = obj.optString("name", "AI生成")
                                        previewDesc = obj.optString("description", result.take(80))
                                    } else {
                                        previewName = requirement.take(12)
                                        previewDesc = result.take(80)
                                    }
                                } catch (e: Exception) {
                                    error = "AI 调用失败：${e.message}"
                                } finally {
                                    generating = false
                                }
                            }
                        },
                        enabled = !generating && requirement.isNotBlank()
                    ) {
                        if (generating) Text("生成中…") else Text("生成")
                    }
                } else {
                    TextButton(onClick = {
                        onGenerated(previewName, previewDesc)
                    }) { Text("✓ 应用") }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

private fun saveSkills(context: android.content.Context, list: List<SkillItem>) {
    val arr = org.json.JSONArray()
    list.forEach { s ->
        arr.put(JSONObject()
            .put("id", s.id)
            .put("name", s.name)
            .put("description", s.description)
            .put("trigger", s.trigger)
            .put("source", s.source)
            .put("createdAt", s.createdAt))
    }
    java.io.File(context.filesDir, "skills.json").writeText(arr.toString())
}

private fun loadPlugins(context: android.content.Context): List<PluginItem> {
    val f = java.io.File(context.filesDir, "plugins.json")
    if (!f.exists()) return emptyList()
    return try {
        val arr = org.json.JSONArray(f.readText())
        (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            PluginItem(
                id = o.optString("id"),
                name = o.optString("name"),
                description = o.optString("description"),
                source = o.optString("source", "user"),
                createdAt = o.optLong("createdAt")
            )
        }
    } catch (_: Exception) { emptyList() }
}

private fun savePlugins(context: android.content.Context, list: List<PluginItem>) {
    val arr = org.json.JSONArray()
    list.forEach { p ->
        arr.put(JSONObject()
            .put("id", p.id)
            .put("name", p.name)
            .put("description", p.description)
            .put("source", p.source)
            .put("createdAt", p.createdAt))
    }
    java.io.File(context.filesDir, "plugins.json").writeText(arr.toString())
}

private fun loadSkills(context: android.content.Context): List<SkillItem> {
    val f = java.io.File(context.filesDir, "skills.json")
    if (!f.exists()) return emptyList()
    return try {
        val arr = org.json.JSONArray(f.readText())
        (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            SkillItem(
                id = o.optString("id"),
                name = o.optString("name"),
                description = o.optString("description"),
                trigger = o.optString("trigger"),
                source = o.optString("source", "user"),
                createdAt = o.optLong("createdAt")
            )
        }
    } catch (_: Exception) { emptyList() }
}
