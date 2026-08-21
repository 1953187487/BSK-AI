package com.bskai.ui.screens.agent

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bskai.BskApp
import com.bskai.agent.AgentEngine
import com.bskai.agent.AgentEvent
import com.bskai.agent.AgentMessage
import com.bskai.agent.PromptFactory
import com.bskai.agent.tools.Tool
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap

sealed class AgentUiItem {
    data class User(val text: String) : AgentUiItem()
    data class Assistant(val text: String) : AgentUiItem()
    data class ToolItem(
        val id: String,
        val name: String,
        val args: JSONObject,
        val output: String? = null,
        val running: Boolean = false
    ) : AgentUiItem()

    data class SystemMsg(val text: String, val isError: Boolean = false) : AgentUiItem()
}

data class PermissionRequest(
    val id: String,
    val toolName: String,
    val args: JSONObject
)

class AgentViewModel(app: Application) : AndroidViewModel(app) {

    private val bskApp = app as BskApp
    private val providerStore get() = bskApp.providerStore
    private val settingsStore get() = bskApp.settingsStore

    private val workspaceDir: String
        get() {
            val dir = File(
                getApplication<BskApp>().getExternalFilesDir(null) ?: getApplication<BskApp>().filesDir,
                "workspace"
            )
            dir.mkdirs()
            return dir.absolutePath
        }

    private val _items = MutableStateFlow<List<AgentUiItem>>(emptyList())
    val items: StateFlow<List<AgentUiItem>> = _items.asStateFlow()

    private val _streaming = MutableStateFlow("")
    val streaming: StateFlow<String> = _streaming.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _permission = MutableStateFlow<PermissionRequest?>(null)
    val permission: StateFlow<PermissionRequest?> = _permission.asStateFlow()

    private val _input = MutableStateFlow("")
    val input: StateFlow<String> = _input.asStateFlow()

    private val permissionWaiters = ConcurrentHashMap<String, CompletableDeferred<Boolean>>()
    private var engine: AgentEngine? = null

    fun onInputChange(text: String) {
        _input.value = text
    }

    fun send(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _isRunning.value) return
        val provider = providerStore.activeProvider()
        if (provider == null) {
            _items.value = _items.value + AgentUiItem.SystemMsg("请先在「设置 -> 模型服务商」中配置 API 服务商", isError = true)
            return
        }
        _input.value = ""
        viewModelScope.launch {
            _isRunning.value = true
            _items.value = _items.value + AgentUiItem.User(trimmed)
            _streaming.value = ""

            val settings = settingsStore.settings.value
            val eng = AgentEngine(
                appContext = getApplication(),
                provider = provider,
                workspaceRoot = workspaceDir,
                autoApprove = settings.autoApproveTools,
                permissionResolver = { tool, args -> awaitPermission(tool, args) }
            )
            engine = eng
            val msgs = mutableListOf<AgentMessage>()
            val history = _items.value.filterIsInstance<AgentUiItem.Assistant>()
                .takeLast(8)
            history.forEach { msgs.add(AgentMessage("assistant", it.text)) }

            eng.run(
                messages = msgs,
                systemPrompt = PromptFactory.baseSystemPrompt("本地工作区 $workspaceDir"),
                onEvent = { handleEvent(it) }
            )
            if (_items.value.lastOrNull()?.let { it is AgentUiItem.Assistant && it.text.isEmpty() } == true) {
                _items.value = _items.value.dropLast(1)
            }
            _isRunning.value = false
        }
    }

    private suspend fun awaitPermission(tool: Tool, args: JSONObject): Boolean {
        val id = "perm_${System.nanoTime()}"
        _permission.value = PermissionRequest(id, tool.name, args)
        val deferred = CompletableDeferred<Boolean>()
        permissionWaiters[id] = deferred
        return deferred.await()
    }

    fun resolvePermission(allowed: Boolean) {
        val req = _permission.value ?: return
        _permission.value = null
        permissionWaiters.remove(req.id)?.complete(allowed)
    }

    private fun handleEvent(event: AgentEvent) {
        when (event) {
            is AgentEvent.Delta -> {
                _streaming.value += event.text
                val currentItems = _items.value.toMutableList()
                val last = currentItems.lastOrNull()
                if (last is AgentUiItem.Assistant) {
                    currentItems[currentItems.size - 1] = last.copy(text = last.text + event.text)
                } else {
                    currentItems.add(AgentUiItem.Assistant(event.text))
                }
                _items.value = currentItems
            }
            is AgentEvent.ToolCallStarted -> {
                _items.value = _items.value + AgentUiItem.ToolItem(
                    id = event.id,
                    name = event.name,
                    args = event.args,
                    running = true
                )
            }
            is AgentEvent.ToolCallFinished -> {
                _items.value = _items.value.map {
                    if (it is AgentUiItem.ToolItem && it.id == event.id) {
                        it.copy(output = event.output.take(4000), running = false)
                    } else it
                }
            }
            is AgentEvent.Error -> {
                _items.value = _items.value + AgentUiItem.SystemMsg(event.message, isError = true)
            }
            is AgentEvent.Done -> {
                _streaming.value = ""
            }
            else -> {}
        }
    }

    fun stop() {
        engine?.cancel()
        _isRunning.value = false
    }

    fun clear() {
        _items.value = emptyList()
        _streaming.value = ""
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = AgentViewModel(app) as T
    }
}
