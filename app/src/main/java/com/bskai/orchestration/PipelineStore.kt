package com.bskai.orchestration

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AgentRole(val key: String, val display: String) {
    ORCHESTRATOR("orchestrator", "编排器"),
    PLANNER("planner", "规划器"),
    CODER("coder", "编码器"),
    REVIEWER("reviewer", "审查器");

    companion object {
        fun fromKey(key: String?): AgentRole =
            entries.firstOrNull { it.key == key } ?: ORCHESTRATOR
    }
}

enum class StepStatus(val key: String) {
    PENDING("pending"),
    RUNNING("running"),
    DONE("done"),
    FAILED("failed");

    companion object {
        fun fromKey(key: String?): StepStatus =
            entries.firstOrNull { it.key == key } ?: PENDING
    }
}

data class PipelineStep(
    val id: String,
    val role: AgentRole,
    val label: String,
    val status: StepStatus = StepStatus.PENDING,
    val input: String = "",
    val output: String = "",
    val model: String = "",
    val error: String = ""
)

data class Pipeline(
    val id: String,
    val task: String,
    val steps: List<PipelineStep>,
    val status: StepStatus = StepStatus.PENDING,
    val startedAt: Long = System.currentTimeMillis(),
    val finishedAt: Long = 0
) {
    val isFinished: Boolean get() = status == StepStatus.DONE || status == StepStatus.FAILED
}

/**
 * OpenClaw 风格多智能体编排流水线状态容器。
 */
class PipelineStore {
    private val _pipelines = MutableStateFlow<List<Pipeline>>(emptyList())
    val pipelines: StateFlow<List<Pipeline>> = _pipelines.asStateFlow()

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    fun addPipeline(pipeline: Pipeline) {
        _pipelines.value = listOf(pipeline) + _pipelines.value
    }

    fun updatePipeline(id: String, transform: (Pipeline) -> Pipeline) {
        _pipelines.value = _pipelines.value.map { if (it.id == id) transform(it) else it }
    }

    fun updateStep(pipelineId: String, stepId: String, transform: (PipelineStep) -> PipelineStep) {
        updatePipeline(pipelineId) { p ->
            p.copy(steps = p.steps.map { if (it.id == stepId) transform(it) else it })
        }
    }

    fun setRunning(running: Boolean) {
        _running.value = running
    }

    fun clear() {
        _pipelines.value = emptyList()
    }
}
