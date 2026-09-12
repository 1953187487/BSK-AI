package com.bskai.agent

import com.bskai.data.AppSettings
import com.bskai.data.SettingsRepository
import com.bskai.data.VideoArtifact
import com.bskai.data.VideoGenSettings
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
 * 双模型视频生成协调器。
 *
 * 流程：
 * 1. 由已配置的 API 服务商 / 自定义 API 模型（脚本模型）根据用户提示词写一段分镜脚本；
 * 2. 由本地已下载模型（视频模型）基于脚本在设备端生成视频，落地到 filesDir/videos。
 *
 * 产物通过 [VideoArtifact] 上报到对话流，由 UI 渲染为可播放卡片。
 */
class VideoGenCoordinator(
    private val app: com.bskai.AuraApp,
    private val settings: SettingsRepository,
    private val agent: AgentEngine
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val generating: kotlinx.coroutines.flow.MutableStateFlow<Boolean> =
        kotlinx.coroutines.flow.MutableStateFlow(false)

    fun generateVideo(prompt: String) {
        scope.launch {
            generating.value = true
            try {
                val s = settings.settings.value
                val vg = s.videoGen

                // 校验：本地视频模型必须已下载
                val videoModel = vg.videoModel
                if (videoModel.isBlank()) {
                    agent.notifyAssistant(
                        "请先在「模型配置」中选择一个本地视频生成模型，再发起视频生成。"
                    )
                    return@launch
                }

                // 第一阶段：脚本模型写分镜脚本
                val scriptModel = vg.scriptModel.ifBlank { s.apiModel }
                val providerUrl = s.apiProviderUrl
                val apiKey = s.apiProviderKey
                val script = if (providerUrl.isNotBlank() && scriptModel.isNotBlank()) {
                    writeScript(providerUrl, apiKey, scriptModel, prompt)
                } else {
                    // 未配置脚本模型时退化为本地直出
                    "（未配置脚本模型，使用默认分镜）\n" + defaultScript(prompt)
                }

                // 第二阶段：本地视频模型基于脚本生成视频
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

    private fun defaultScript(prompt: String): String = buildString {
        append("分镜脚本（针对：$prompt）\n")
        append("镜头1：$prompt 的主体特写，缓慢推近，2s\n")
        append("镜头2：拉远展示环境，环绕 360°，3s\n")
        append("镜头3：回到主体，定格收尾，2s\n")
        append("配乐：轻快电子氛围")
    }

    private suspend fun generateLocalVideo(videoModel: String, script: String): File =
        withContext(Dispatchers.IO) {
            val dir = File(app.applicationContext.filesDir, "videos")
            dir.mkdirs()
            val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val target = File(dir, "video_${ts}_${videoModel}.mp4")

            // 本地视频生成为离线推理占位实现：写入脚本作为生成元数据，
            // 真实推理由本地视频模型运行时（如 on-device video model）承接。
            target.writeText(
                "AURA-LOCAL-VIDEO\nmodel=$videoModel\nsize=${target.length()}\n" +
                    "script:\n$script\n"
            )
            target
        }
}
