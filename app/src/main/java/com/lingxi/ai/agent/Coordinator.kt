package com.lingxi.ai.agent

import com.lingxi.ai.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * UI 输入 → AgentEngine 的桥接层。
 *
 * 提交的对话在独立协程中执行；[cancelActive] 可中止当前一轮
 * 工具循环或流式输出（UI 的停止按钮调用）。
 *
 * 生命周期：与宿主屏幕同寿命；宿主销毁时调用 [destroy]。
 */
class Coordinator(
    private val settings: SettingsRepository,
    private val agent: AgentEngine
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var activeJob: Job? = null

    /** 是否有正在执行的对话任务（可与 [AgentEngine.processing] 互相印证）。 */
    val busy: Boolean get() = activeJob?.isActive == true

    /**
     * 提交一段用户输入。重复提交会被忽略（上一轮未结束时）。
     */
    fun submit(text: String) {
        if (busy) return
        activeJob = scope.launch {
            agent.answer(text)
        }
    }

    /** 中止当前一轮处理（不回滚已追加的消息）。 */
    fun cancelActive() {
        activeJob?.cancel()
        activeJob = null
    }

    /** 释放协程域。宿主销毁后不得再调用 [submit]。 */
    fun destroy() {
        scope.cancel()
    }
}
