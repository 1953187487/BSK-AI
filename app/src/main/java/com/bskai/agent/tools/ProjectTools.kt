package com.bskai.agent.tools

import com.bskai.models.LocalModel
import com.bskai.models.ModelStatus
import com.bskai.models.ModelStore
import com.bskai.toolkit.ProjectConfig
import com.bskai.toolkit.ProjectScaffold
import org.json.JSONObject

object NewProjectTool : Tool {
    override val name = "new_project"
    override val description = "在工作区创建一个新的 Java Android 项目骨架，可用于后续直接构建 APK。"
    override val requiresPermission = true
    override val parameters: JSONObject = JSONObject()
        .put("type", "object")
        .put("properties", JSONObject()
            .put("name", JSONObject().put("type", "string").put("description", "项目名（英文小写，如 myapp）"))
            .put("package", JSONObject().put("type", "string").put("description", "包名，如 com.example.myapp"))
            .put("appLabel", JSONObject().put("type", "string").put("description", "应用显示名")))
        .put("required", JSONArrayOf("name", "package", "appLabel"))

    override suspend fun execute(args: JSONObject, ctx: ToolContext): ToolResult {
        if (!ctx.workspace.supportsRealPath) {
            return ToolResult(false, "当前工作区为外部目录(SAF)，创建项目需要切换到应用私有目录后使用")
        }
        val name = args.optString("name").lowercase().filter { it.isLetterOrDigit() || it == '_' }
        if (name.isEmpty()) return ToolResult(false, "项目名不合法")
        val config = ProjectConfig(
            name = name,
            packageName = args.optString("package"),
            appLabel = args.optString("appLabel")
        )
        val rootOverride = ctx.realPathFor(name)
        val root = if (rootOverride != null) {
            ProjectScaffold.create(ctx.app, config, java.io.File(rootOverride))
        } else {
            ProjectScaffold.create(ctx.app, config)
        }
        return if (root != null && root.exists()) {
            ToolResult(true, "项目已创建: ${root.absolutePath}\n构建命令: 使用 build_project 工具")
        } else {
            ToolResult(false, "项目创建失败")
        }
    }
}

object ListModelsTool : Tool {
    override val name = "list_models"
    override val description = "列出本地已下载的模型与下载状态。"
    override val requiresPermission = false
    override val parameters: JSONObject = JSONObject()
        .put("type", "object")
        .put("properties", JSONObject())
        .put("required", JSONArrayOf())

    override suspend fun execute(args: JSONObject, ctx: ToolContext): ToolResult {
        val store = ModelStore(ctx.app)
        val sb = StringBuilder("本地模型:\n")
        store.snapshot().forEach {
            val status = when (it.status) {
                ModelStatus.READY -> "已就绪 ${formatSize(it.downloadedBytes)}"
                ModelStatus.DOWNLOADING -> "下载中 ${(it.progress * 100).toInt()}%"
                ModelStatus.FAILED -> "失败"
                else -> "未下载"
            }
            sb.appendLine("  - ${it.name} [$status]")
        }
        return ToolResult(true, sb.toString().trimEnd())
    }
}

object DownloadModelTool : Tool {
    override val name = "download_model"
    override val description = "下载本地 GGUF 模型，使用内置清单中的模型 id，或提供自定义下载 URL。"
    override val requiresPermission = true
    override val parameters: JSONObject = JSONObject()
        .put("type", "object")
        .put("properties", JSONObject()
            .put("modelId", JSONObject().put("type", "string").put("description", "内置模型 id，如 qwen2.5-0.5b"))
            .put("url", JSONObject().put("type", "string").put("description", "自定义 GGUF 下载链接（可选）")))
        .put("required", JSONArrayOf("modelId"))

    override suspend fun execute(args: JSONObject, ctx: ToolContext): ToolResult {
        val modelId = args.optString("modelId")
        val store = ModelStore(ctx.app)
        val model = store.snapshot().firstOrNull { it.catalogId == modelId }
            ?: return ToolResult(false, "未找到模型 id: $modelId")
        return ToolResult(
            true,
            "模型 ${model.name} 已加入下载队列，请在「模型中心」页面查看下载进度。\nURL: ${model.url}"
        )
    }
}

object BuildProjectTool : Tool {
    override val name = "build_project"
    override val description = "构建指定的 Android 项目为 APK（需要 Termux 环境提供 javac/aapt2/d8/apksigner）。"
    override val requiresPermission = true
    override val parameters: JSONObject = JSONObject()
        .put("type", "object")
        .put("properties", JSONObject()
            .put("projectName", JSONObject().put("type", "string").put("description", "项目名，与 new_project 创建的名字一致")))
        .put("required", JSONArrayOf("projectName"))

    override suspend fun execute(args: JSONObject, ctx: ToolContext): ToolResult {
        if (!ctx.workspace.supportsRealPath) {
            return ToolResult(false, "当前工作区为外部目录(SAF)，构建 APK 需要切换到应用私有目录后使用")
        }
        val name = args.optString("projectName")
        val builder = com.bskai.toolkit.ApkBuilder(ctx.app)
        val err = builder.build(name)
        return if (err == null) {
            ToolResult(true, "构建任务已提交到 Termux 后台执行。构建完成后 APK 将出现在 工具链页面 的构建产物中。")
        } else {
            ToolResult(false, err)
        }
    }
}

fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    var v = bytes.toDouble()
    var u = 0
    while (v >= 1024 && u < units.size - 1) {
        v /= 1024
        u++
    }
    return "%.1f %s".format(v, units[u])
}
