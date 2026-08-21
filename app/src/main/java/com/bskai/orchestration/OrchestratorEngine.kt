package com.bskai.orchestration

import android.content.Context
import com.bskai.agent.AgentEngine
import com.bskai.agent.AgentEvent
import com.bskai.agent.AgentMessage
import com.bskai.agent.PromptFactory
import com.bskai.models.ProviderConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * OpenClaw 风格多智能体编排器：规划器 -> 编码器 -> 审查器 的迭代流水线。
 */
class OrchestratorEngine(
    private val appContext: Context,
    private val provider: ProviderConfig,
    private val workspaceRoot: String,
    private val store: PipelineStore,
    private val autoApprove: Boolean = false,
    private val permissionResolver: suspend (com.bskai.agent.tools.Tool, org.json.JSONObject) -> Boolean = { _, _ -> false },
    private val maxRounds: Int = 2
) {

    private val _currentOutput = MutableStateFlow("")
    val currentOutput: StateFlow<String> = _currentOutput.asStateFlow()

    @Volatile
    private var cancelled = false

    fun cancel() {
        cancelled = true
    }

    suspend fun run(task: String) {
        cancelled = false
        val pipelineId = "pipe_${System.currentTimeMillis()}"
        val steps = mutableListOf(
            PipelineStep("planner", AgentRole.PLANNER, "规划执行步骤"),
            PipelineStep("coder_1", AgentRole.CODER, "编码实现（第 1 轮）"),
            PipelineStep("reviewer_1", AgentRole.REVIEWER, "审查实现（第 1 轮）")
        )
        if (maxRounds > 1) {
            steps += PipelineStep("coder_2", AgentRole.CODER, "修订编码（第 2 轮）")
            steps += PipelineStep("reviewer_2", AgentRole.REVIEWER, "复审（第 2 轮）")
        }
        store.addPipeline(
            Pipeline(pipelineId, task, steps, StepStatus.RUNNING)
        )
        store.setRunning(true)

        withContext(Dispatchers.IO) {
            try {
                // 1. 规划
                val plan = runRole(
                    pipelineId,
                    "planner",
                    AgentRole.PLANNER,
                    PromptFactory.plannerSystemPrompt(),
                    task,
                    withTools = false
                )
                if (cancelled) return@withContext
                val planLines = plan.lineSequence().filter { it.isNotBlank() }.joinToString("\n").take(4000)

                // 2. 编码
                var coderOutput = runRole(
                    pipelineId,
                    "coder_1",
                    AgentRole.CODER,
                    PromptFactory.coderSystemPrompt(planLines),
                    task,
                    withTools = true
                )
                if (cancelled) return@withContext

                // 3. 审查
                var review = runRole(
                    pipelineId,
                    "reviewer_1",
                    AgentRole.REVIEWER,
                    PromptFactory.reviewerSystemPrompt(planLines),
                    task + "\n\n【编码器输出】\n" + coderOutput.take(8000),
                    withTools = false
                )
                if (cancelled) return@withContext
                val passed = review.contains("通过") && !review.contains("不通过")

                // 4. 如有必要，第二轮修订
                if (!passed && maxRounds > 1 && !cancelled) {
                    val coder2 = runRole(
                        pipelineId,
                        "coder_2",
                        AgentRole.CODER,
                        PromptFactory.coderSystemPrompt(planLines) + "\n\n【审查意见】\n" + review.take(4000),
                        "根据审查意见修订", 
                        withTools = true
                    )
                    coderOutput = coder2
                    review = runRole(
                        pipelineId,
                        "reviewer_2",
                        AgentRole.REVIEWER,
                        PromptFactory.reviewerSystemPrompt(planLines),
                        task + "\n\n【修订后输出】\n" + coder2.take(8000),
                        withTools = false
                    )
                }

                store.updatePipeline(pipelineId) {
                    it.copy(status = StepStatus.DONE, finishedAt = System.currentTimeMillis())
                }
            } catch (e: Exception) {
                store.updatePipeline(pipelineId) {
                    it.copy(status = StepStatus.FAILED, finishedAt = System.currentTimeMillis())
                }
            } finally {
                store.setRunning(false)
            }
        }
    }

    private suspend fun runRole(
        pipelineId: String,
        stepId: String,
        role: AgentRole,
        systemPrompt: String,
        input: String,
        withTools: Boolean
    ): String {
        store.updateStep(pipelineId, stepId) {
            it.copy(status = StepStatus.RUNNING, input = input.take(500), model = provider.model)
        }
        val engine = AgentEngine(
            appContext = appContext,
            provider = provider,
            workspaceRoot = workspaceRoot,
            autoApprove = autoApprove,
            permissionResolver = permissionResolver
        )
        val messages = mutableListOf(AgentMessage("user", input))
        val sb = StringBuilder()
        val onEvent: (AgentEvent) -> Unit = { ev ->
            when (ev) {
                is AgentEvent.Delta -> {
                    sb.append(ev.text)
                    _currentOutput.value = sb.toString()
                    store.updateStep(pipelineId, stepId) { it.copy(output = sb.toString().takeLast(6000)) }
                }
                is AgentEvent.ToolCallStarted ->
                    store.updateStep(pipelineId, stepId) { it.copy(label = "编码实现 - 正在调用 ${ev.name}") }
                is AgentEvent.Error ->
                    store.updateStep(pipelineId, stepId) { it.copy(error = ev.message) }
                else -> {}
            }
        }
        val output = if (withTools) {
            engine.run(messages, systemPrompt, onEvent)
            sb.toString()
        } else {
            engine.complete(messages, systemPrompt, onEvent)
        }
        store.updateStep(pipelineId, stepId) {
            it.copy(
                status = if (cancelled) StepStatus.PENDING else StepStatus.DONE,
                output = output.takeLast(6000)
            )
        }
        return output
    }
}
