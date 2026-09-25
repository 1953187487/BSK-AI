package com.lingxi.ai.agent

import com.lingxi.ai.data.AppSettings
import com.lingxi.ai.data.SettingsRepository
import com.lingxi.ai.data.VideoArtifact
import com.lingxi.ai.data.VideoGenSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

/**
 * Dual-model video generation coordinator.
 *
 * Pipeline:
 * 1. A configured API provider / custom API model (script model) writes a shot script
 *    from the user prompt.
 * 2. A locally downloaded model (video model) generates the video on-device and
 *    writes it to filesDir/videos.
 *
 * The artifact is reported to the conversation flow via [VideoArtifact] and
 * rendered as a playable card by the UI.
 *
 * NOTE: local video generation is a placeholder stub. Real on-device video
 * inference is not yet implemented; see [generateLocalVideo] for details.
 */
class VideoGenCoordinator(
    private val app: com.lingxi.ai.LingXiApp,
    private val settings: SettingsRepository,
    private val agent: AgentEngine
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /**
     * Whether a video generation task is currently in progress.
     * Observed by the UI to show progress state.
     */
    val generating: kotlinx.coroutines.flow.MutableStateFlow<Boolean> =
        kotlinx.coroutines.flow.MutableStateFlow(false)

    /**
     * Starts a video generation pipeline for the given prompt.
     *
     * The pipeline runs on a background coroutine; callers can observe
     * [generating] for progress. Results are pushed to the agent's
     * conversation via [AgentEngine.notifyAssistant] and [AgentEngine.emitVideo].
     *
     * @param prompt user description of the desired video
     */
    fun generateVideo(prompt: String) {
        scope.launch {
            generating.value = true
            try {
                val s = settings.settings.value
                val vg = s.videoGen

                // Validate: local video model must be downloaded
                val videoModel = vg.videoModel
                if (videoModel.isBlank()) {
                    agent.notifyAssistant(
                        "请先在「模型配置」中选择一个本地视频生成模型，再发起视频生成。"
                    )
                    return@launch
                }

                // Phase 1: script model writes the shot script
                val scriptModel = vg.scriptModel.ifBlank { s.apiModel }
                val providerUrl = s.apiProviderUrl
                val apiKey = s.apiProviderKey
                val script = if (providerUrl.isNotBlank() && scriptModel.isNotBlank()) {
                    writeScript(providerUrl, apiKey, scriptModel, prompt)
                } else {
                    // No script model configured: fall back to local default
                    "（未配置脚本模型，使用默认分镜）\n" + defaultScript(prompt)
                }

                // Phase 2: local video model generates the video from the script
                val videoFile = generateLocalVideo(videoModel, script)

                val artifact = VideoArtifact(
                    script = script,
                    videoPath = videoFile.absolutePath,
                    scriptModel = scriptModel.ifBlank { "local" },
                    videoModel = videoModel
                )
                agent.emitVideo(artifact)
            } catch (e: Exception) {
                agent.notifyAssistant("视频生成失败：" + (e.message ?: "未知错误"))
            } finally {
                generating.value = false
            }
        }
    }

    /**
     * Writes a shot script using the configured script model.
     *
     * @param providerUrl API provider base URL
     * @param apiKey API key
     * @param scriptModel model name for script generation
     * @param prompt user prompt
     * @return the generated script text
     */
    private suspend fun writeScript(
        providerUrl: String,
        apiKey: String?,
        scriptModel: String,
        prompt: String
    ): String = withContext(Dispatchers.IO) {
        val llm = LlmClient(app.applicationContext)
        val messages = listOf(
            ChatMsg(
                "system",
                "你是视频分镜脚本写手。请根据用户提示词写一段适合生成短视频的分镜脚本，" +
                    "包含 3~5 个镜头，每个镜头给出画面描述、时长与镜头运动，" +
                    "并给出一句配乐建议。直接输出文本。"
            ),
            ChatMsg("user", prompt)
        )
        try {
            val resp = llm.chatWithModel(providerUrl, apiKey, scriptModel, messages)
            resp.content.ifBlank { defaultScript(prompt) }
        } catch (e: Exception) {
            "（脚本模型调用失败：${e.message}）\n" + defaultScript(prompt)
        }
    }

    /**
     * Returns a default shot script when no script model is configured.
     *
     * @param prompt user prompt
     * @return default script text
     */
    private fun defaultScript(prompt: String): String = buildString {
        append("分镜脚本（针对：$prompt）\n")
        append("镜头1：$prompt 的主体特写，缓慢推近，2s\n")
        append("镜头2：拉远展示环境，环绕 360°，3s\n")
        append("镜头3：回到主体，定格收尾，2s\n")
        append("配乐：轻快电子氛围")
    }

    /**
     * Placeholder for local video generation.
     *
     * Real on-device video inference is not yet implemented. This stub writes a
     * metadata sidecar file next to the intended .mp4 output, containing the
     * model name and script, so the pipeline can be exercised end-to-end without
     * a real video encoder.
     *
     * The .mp4 file itself contains plain-text metadata; it is NOT a valid MP4.
     * The corresponding sidecar file (with .meta suffix) holds structured metadata.
     *
     * @param videoModel name of the local video model
     * @param script the shot script to render
     * @return the file path where the (placeholder) video was written
     */
    private suspend fun generateLocalVideo(videoModel: String, script: String): File =
        withContext(Dispatchers.IO) {
            val dir = File(app.applicationContext.filesDir, "videos")
            dir.mkdirs()
            val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val target = File(dir, "video_${ts}_${videoModel}.mp4")

            // Placeholder: write metadata sidecar + stub .mp4 content.
            // Real video generation (on-device inference) is not yet implemented.
            val metaFile = File(dir, "video_${ts}_${videoModel}.meta")
            metaFile.writeText(
                "model=$videoModel\n" +
                    "generated_at=${System.currentTimeMillis()}\n" +
                    "script:\n$script\n"
            )
            target.writeText(
                "LINGXI-LOCAL-VIDEO-PLACEHOLDER\n" +
                    "model=$videoModel\n" +
                    "meta=${metaFile.name}\n" +
                    "note=real video inference not yet implemented\n"
            )
            target
        }
}
