package com.bskai.workspace

import android.content.Context
import android.content.Intent
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
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * A workspace entry: a named, addressable file area the AI can read/write.
 *
 * @property id stable unique identifier
 * @property name display name
 * @property kind INTERNAL (app-private dir) or EXTERNAL (SAF tree)
 * @property treeUri content URI for EXTERNAL workspaces, null for INTERNAL
 */
data class WorkspaceEntry(
    val id: String,
    val name: String,
    val kind: Kind,
    val treeUri: String?
) {
    /** Workspace kind. */
    enum class Kind { INTERNAL, EXTERNAL }
}

/**
 * Manages internal and external workspaces for the AI.
 *
 * Workspaces are persisted in SharedPreferences; the active workspace is
 * the one the AI's file tools operate on.
 *
 * @param context Android [Context] for files/permissions access
 * @param settings [SettingsRepository] (reserved for future workspace-scoped settings)
 */
class WorkspaceManager(
    private val context: Context,
    private val settings: SettingsRepository
) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)

    private val _workspaces = MutableStateFlow(loadAll())
    /** All workspaces, observable by the UI. */
    val workspaces: StateFlow<List<WorkspaceEntry>> = _workspaces.asStateFlow()

    private val _activeId = MutableStateFlow(prefs.getString(KEY_ACTIVE, null))
    /** The active workspace id, or null. */
    val activeId: StateFlow<String?> = _activeId.asStateFlow()

    /** The currently active workspace entry. */
    val active: WorkspaceEntry?
        get() = _workspaces.value.firstOrNull { it.id == _activeId.value }
            ?: _workspaces.value.firstOrNull()

    /**
     * Creates the default internal workspace when none exists.
     *
     * Call once at app startup.
     */
    fun ensureDefault() {
        if (_workspaces.value.isEmpty()) {
            val id = "default"
            val name = "默认工作区"
            createInternal(id, name)
        }
    }

    /**
     * Creates (or re-uses) an internal workspace backed by a directory under
     * filesDir/workspaces/<id>. A README.md is seeded on first creation.
     *
     * @param id workspace identifier
     * @param name display name
     * @return the created or existing [WorkspaceEntry]
     */
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

    /**
     * Imports an external SAF tree as a workspace.
     *
     * A stable, content-based key is derived from the URI so the same tree
     * maps to the same id across app runs (unlike [Uri.hashCode()], which
     * is unstable). Persistent read+write URI permissions are requested.
     *
     * @param name user-supplied display name
     * @param treeUri SAF tree URI
     * @return the imported [WorkspaceEntry]
     */
    fun importExternal(name: String, treeUri: Uri): WorkspaceEntry {
        val id = "ext_" + stableKey(treeUri)
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
        return upsert(WorkspaceEntry(id = id, name = name, kind = WorkspaceEntry.Kind.EXTERNAL, treeUri = treeUri.toString()))
    }

    /**
     * Removes a workspace by id and re-activates the remaining first one.
     *
     * @param id the workspace to remove
     */
    fun remove(id: String) {
        val list = _workspaces.value.filter { it.id != id }
        persistAll(list)
        _workspaces.value = list
        if (_activeId.value == id) {
            val next = list.firstOrNull()?.id
            setActive(next)
        }
    }

    /**
     * Sets the active workspace.
     *
     * @param id workspace id to activate, or null to clear
     */
    fun setActive(id: String?) {
        _activeId.value = id
        prefs.edit().putString(KEY_ACTIVE, id).apply()
    }

    /**
     * Renames a workspace.
     *
     * @param id the workspace to rename
     * @param name new display name
     */
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

    /**
     * Lists the root entries of the active workspace.
     *
     * @return list of [WorkspaceNode]s, empty when no active workspace
     */
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

    /**
     * Reads a text file relative to the active workspace root.
     *
     * @param relPath relative path (must not escape the root)
     * @return file text, or null when missing / unreadable / unsafe
     */
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

    /**
     * Writes text to a file relative to the active workspace root,
     * creating intermediate directories as needed.
     *
     * @param relPath relative path (must not escape the root)
     * @param content text to write
     * @return true on success
     */
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

    /**
     * Lists the entries under a relative path in the active workspace.
     *
     * @param relPath relative directory path (empty = root)
     * @return list of [WorkspaceNode]s, empty when path is not a directory
     */
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
     * Exports the active workspace to a ZIP file.
     *
     * @param outputFile destination ZIP file
     * @return true on success
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

    /**
     * Normalizes a relative path, rejecting any path that escapes the root.
     *
     * @param path raw relative path
     * @return the sanitized path, or null when unsafe
     */
    private fun sanitizeRel(path: String): String? {
        if (path.isBlank()) return null
        if (path.contains("..")) return null
        return path.trim('/')
    }

    /**
     * Derives a stable, content-based identifier for an external SAF tree.
     *
     * Uses an MD5 hex digest of the URI string so the same tree maps to the
     * same id across app runs (unlike [Uri.hashCode()], which depends on
     * the object's memory address / hash seed).
     *
     * @param uri the SAF tree URI
     * @return a 32-char hex string
     */
    private fun stableKey(uri: Uri): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            val bytes = md.digest(uri.toString().toByteArray())
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            // Fallback: use the hash of the string representation
            Integer.toHexString(uri.toString().hashCode())
        }
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

/**
 * A node in a workspace listing.
 *
 * @property name file / directory name
 * @property path relative path
 * @property isDirectory true for directories
 * @property size byte size (0 for directories)
 */
data class WorkspaceNode(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long
)
