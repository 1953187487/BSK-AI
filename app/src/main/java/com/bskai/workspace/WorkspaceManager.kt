package com.bskai.workspace

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.bskai.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class WorkspaceEntry(
    val id: String,
    val name: String,
    val kind: Kind,
    val treeUri: String?
) {
    enum class Kind { INTERNAL, EXTERNAL }
}

class WorkspaceManager(
    private val context: Context,
    private val settings: SettingsRepository
) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)

    private val _workspaces = MutableStateFlow(loadAll())
    val workspaces: StateFlow<List<WorkspaceEntry>> = _workspaces.asStateFlow()

    private val _activeId = MutableStateFlow(prefs.getString(KEY_ACTIVE, null))
    val activeId: StateFlow<String?> = _activeId.asStateFlow()

    val active: WorkspaceEntry?
        get() = _workspaces.value.firstOrNull { it.id == _activeId.value }
            ?: _workspaces.value.firstOrNull()

    fun ensureDefault() {
        if (_workspaces.value.isEmpty()) {
            val id = "default"
            val name = "默认工作区"
            createInternal(id, name)
        }
    }

    fun createInternal(id: String, name: String): WorkspaceEntry {
        val dir = File(context.filesDir, "workspaces/$id")
        if (!dir.exists()) dir.mkdirs()
        val readme = File(dir, "README.md")
        if (!readme.exists()) {
            readme.writeText(
                """# AURA 工作区 · $name

你在对话中让 AURA 读写文件时，操作的就是此目录。

- 输入 `/` 唤起命令菜单
- 在聊天栏右侧切换是否允许 AI 读写工作区
- 切换工作区：右上 MoreVert → 工作区
"""
            )
        }
        return upsert(WorkspaceEntry(id = id, name = name, kind = WorkspaceEntry.Kind.INTERNAL, treeUri = null))
    }

    fun importExternal(name: String, treeUri: Uri): WorkspaceEntry {
        val id = "ext_${treeUri.hashCode()}"
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                treeUri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
        return upsert(WorkspaceEntry(id = id, name = name, kind = WorkspaceEntry.Kind.EXTERNAL, treeUri = treeUri.toString()))
    }

    fun remove(id: String) {
        val list = _workspaces.value.filter { it.id != id }
        persistAll(list)
        _workspaces.value = list
        if (_activeId.value == id) {
            val next = list.firstOrNull()?.id
            setActive(next)
        }
    }

    fun setActive(id: String?) {
        _activeId.value = id
        prefs.edit().putString(KEY_ACTIVE, id).apply()
    }

    fun rename(id: String, name: String) {
        val list = _workspaces.value.map { if (it.id == id) it.copy(name = name) else it }
        persistAll(list)
        _workspaces.value = list
    }

    private fun upsert(e: WorkspaceEntry): WorkspaceEntry {
        val list = _workspaces.value.toMutableList()
        val idx = list.indexOfFirst { it.id == e.id }
        if (idx >= 0) list[idx] = e else list.add(e)
        persistAll(list)
        _workspaces.value = list
        if (_activeId.value == null) setActive(e.id)
        return e
    }

    fun listRoot(): List<WorkspaceNode> {
        val active = active ?: return emptyList()
        return when (active.kind) {
            WorkspaceEntry.Kind.INTERNAL -> listInternal(File(context.filesDir, "workspaces/${active.id}"))
            WorkspaceEntry.Kind.EXTERNAL -> {
                val uri = Uri.parse(active.treeUri) ?: return emptyList()
                val tree = DocumentFile.fromTreeUri(context, uri) ?: return emptyList()
                tree.listFiles().map { it.toNode() }
            }
        }
    }

    fun readRelative(relPath: String): String? {
        val active = active ?: return null
        val safe = sanitizeRel(relPath) ?: return null
        return when (active.kind) {
            WorkspaceEntry.Kind.INTERNAL -> {
                val f = File(File(context.filesDir, "workspaces/${active.id}"), safe)
                if (f.exists() && f.isFile) f.readText() else null
            }
            WorkspaceEntry.Kind.EXTERNAL -> {
                val uri = Uri.parse(active.treeUri) ?: return null
                val tree = DocumentFile.fromTreeUri(context, uri) ?: return null
                val file = tree.findFile(relPath)
                file?.let { context.contentResolver.openInputStream(it.uri)?.bufferedReader()?.use { r -> r.readText() } }
            }
        }
    }

    fun writeRelative(relPath: String, content: String): Boolean {
        val active = active ?: return false
        val safe = sanitizeRel(relPath) ?: return false
        return when (active.kind) {
            WorkspaceEntry.Kind.INTERNAL -> {
                val f = File(File(context.filesDir, "workspaces/${active.id}"), safe)
                f.parentFile?.mkdirs()
                f.writeText(content)
                true
            }
            WorkspaceEntry.Kind.EXTERNAL -> {
                val uri = Uri.parse(active.treeUri) ?: return false
                val tree = DocumentFile.fromTreeUri(context, uri) ?: return false
                val parent = relPath.substringBeforeLast('/', "").ifEmpty { "" }
                val name = relPath.substringAfterLast('/')
                val parentDoc: DocumentFile? = if (parent.isBlank()) tree
                else tree.findFile(parent) ?: tree.createDirectory(parent)
                if (parentDoc == null) return false
                val existing = parentDoc.findFile(name)
                if (existing != null && existing.isFile) {
                    context.contentResolver.openOutputStream(existing.uri, "wt")?.use {
                        it.write(content.toByteArray())
                    }
                    true
                } else {
                    val created = parentDoc.createFile("text/plain", name)
                    created?.let { d ->
                        context.contentResolver.openOutputStream(d.uri)?.use {
                            it.write(content.toByteArray())
                        }
                        true
                    } ?: false
                }
            }
        }
    }

    fun listRelative(relPath: String): List<WorkspaceNode> {
        val active = active ?: return emptyList()
        return when (active.kind) {
            WorkspaceEntry.Kind.INTERNAL -> {
                val f = File(File(context.filesDir, "workspaces/${active.id}"), sanitizeRel(relPath) ?: "")
                if (f.exists() && f.isDirectory) f.listFiles()?.map { it.toNode() } ?: emptyList()
                else emptyList()
            }
            WorkspaceEntry.Kind.EXTERNAL -> {
                val uri = Uri.parse(active.treeUri) ?: return emptyList()
                val tree = DocumentFile.fromTreeUri(context, uri) ?: return emptyList()
                val dir = if (relPath.isBlank()) tree else tree.findFile(relPath)
                dir?.listFiles()?.map { it.toNode() } ?: emptyList()
            }
        }
    }

    /**
     * 导出工作区为 ZIP 文件。
     */
    fun exportToZip(outputFile: File): Boolean {
        val active = active ?: return false
        return try {
            ZipOutputStream(FileOutputStream(outputFile)).use { zos ->
                when (active.kind) {
                    WorkspaceEntry.Kind.INTERNAL -> {
                        val dir = File(context.filesDir, "workspaces/${active.id}")
                        zipDir(dir, dir, zos)
                    }
                    WorkspaceEntry.Kind.EXTERNAL -> {
                        val uri = Uri.parse(active.treeUri) ?: return false
                        val tree = DocumentFile.fromTreeUri(context, uri) ?: return false
                        zipDocumentFile(tree, "", zos)
                    }
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun zipDir(baseDir: File, dir: File, zos: ZipOutputStream) {
        val files = dir.listFiles() ?: return
        for (f in files) {
            val rel = f.relativeTo(baseDir).path
            if (f.isDirectory) {
                zos.putNextEntry(ZipEntry("$rel/"))
                zos.closeEntry()
                zipDir(baseDir, f, zos)
            } else {
                zos.putNextEntry(ZipEntry(rel))
                f.inputStream().use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }
    }

    private fun zipDocumentFile(dir: DocumentFile, basePath: String, zos: ZipOutputStream) {
        val files = dir.listFiles()
        for (f in files) {
            val rel = if (basePath.isEmpty()) f.name ?: "" else "$basePath/${f.name}"
            if (f.isDirectory) {
                zos.putNextEntry(ZipEntry("$rel/"))
                zos.closeEntry()
                zipDocumentFile(f, rel, zos)
            } else {
                zos.putNextEntry(ZipEntry(rel))
                context.contentResolver.openInputStream(f.uri)?.use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }
    }

    private fun listInternal(dir: File): List<WorkspaceNode> {
        if (!dir.exists()) return emptyList()
        return (dir.listFiles() ?: emptyArray()).map { it.toNode() }
    }

    private fun File.toNode(): WorkspaceNode {
        return WorkspaceNode(
            name = name,
            path = name,
            isDirectory = isDirectory,
            size = if (isFile) length() else 0L
        )
    }

    private fun DocumentFile.toNode(): WorkspaceNode {
        return WorkspaceNode(
            name = name ?: "(unnamed)",
            path = name ?: "(unnamed)",
            isDirectory = isDirectory,
            size = if (isFile) length() else 0L
        )
    }

    private fun sanitizeRel(path: String): String? {
        if (path.isBlank()) return null
        if (path.contains("..")) return null
        return path.trim('/')
    }

    private fun persistAll(list: List<WorkspaceEntry>) {
        val arr = JSONArray()
        list.forEach { e ->
            arr.put(
                JSONObject().apply {
                    put("id", e.id)
                    put("name", e.name)
                    put("kind", e.kind.name)
                    put("treeUri", e.treeUri ?: JSONObject.NULL)
                }
            )
        }
        prefs.edit().putString(KEY_LIST, arr.toString()).apply()
    }

    private fun loadAll(): List<WorkspaceEntry> {
        val raw = prefs.getString(KEY_LIST, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                WorkspaceEntry(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    kind = WorkspaceEntry.Kind.valueOf(o.optString("kind", "INTERNAL")),
                    treeUri = if (o.isNull("treeUri")) null else o.optString("treeUri")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    companion object {
        private const val KEY_LIST = "workspace_list"
        private const val KEY_ACTIVE = "workspace_active"
    }
}

data class WorkspaceNode(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long
)
