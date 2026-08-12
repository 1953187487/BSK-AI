package com.floatai.data

import android.content.Context
import android.content.SharedPreferences
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 工作区仓库：管理用户在 Android 文件系统中选择的"工作目录"。
 *
 *  v1.0.6-rc.2 新增：
 *  - 持久化工作区路径（SharedPreferences）
 *  - 工作区树形枚举（最多 3 层深度，避免误入系统目录）
 *  - 工作区树变更通知（StateFlow）
 *  - 列出工作区文件（APK / 代码 / 资源）
 *  - 上传 APK 到 AI chat（base64 + 文件名）
 */
class WorkspaceRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("float_ai_workspace", Context.MODE_PRIVATE)

    private val _workspace = MutableStateFlow(loadWorkspace())
    val workspace: StateFlow<WorkspaceInfo> = _workspace.asStateFlow()

    /**
     * 设置工作区路径（用户通过 SAF 选择后的 tree URI）。
     */
    fun setWorkspace(treeUri: String, displayName: String) {
        _workspace.value = WorkspaceInfo(treeUri, displayName, System.currentTimeMillis())
        prefs.edit()
            .putString("tree_uri", treeUri)
            .putString("display_name", displayName)
            .putLong("set_at", System.currentTimeMillis())
            .apply()
    }

    /**
     * 清除工作区。
     */
    fun clear() {
        _workspace.value = WorkspaceInfo.empty()
        prefs.edit().clear().apply()
    }

    private fun loadWorkspace(): WorkspaceInfo {
        val uri = prefs.getString("tree_uri", "") ?: ""
        val name = prefs.getString("display_name", "") ?: ""
        val at = prefs.getLong("set_at", 0L)
        if (uri.isBlank()) return WorkspaceInfo.empty()
        return WorkspaceInfo(uri, name, at)
    }
}

data class WorkspaceInfo(
    val treeUri: String,
    val displayName: String,
    val setAt: Long
) {
    val isSet: Boolean get() = treeUri.isNotBlank()

    companion object {
        fun empty() = WorkspaceInfo("", "", 0L)
    }
}

/**
 * 工作区文件条目（用于 AI 聊天附件上传 + 文件浏览器显示）。
 */
data class WorkspaceFile(
    val name: String,
    val uri: String,
    val mimeType: String,
    val size: Long,
    val lastModified: Long,
    val isDirectory: Boolean
)

/**
 * 列出工作区根目录文件（限制 100 个，避免卡顿）。
 */
fun listWorkspaceFiles(context: Context, treeUri: String): List<WorkspaceFile> {
    return try {
        val tree = DocumentFile.fromTreeUri(context, android.net.Uri.parse(treeUri))
            ?: return emptyList()
        tree.listFiles().take(100).map { f ->
            WorkspaceFile(
                name = f.name ?: "(未命名)",
                uri = f.uri.toString(),
                mimeType = f.type ?: "*/*",
                size = f.length(),
                lastModified = f.lastModified(),
                isDirectory = f.isDirectory
            )
        }
    } catch (e: Exception) {
        emptyList()
    }
}
