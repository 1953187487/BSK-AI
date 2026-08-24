package com.bskai.agent

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.IOException

data class WorkspaceEntry(
    val name: String,
    val isDirectory: Boolean,
    val size: Long
)

sealed class Workspace {
    abstract val displayName: String
    abstract val supportsRealPath: Boolean

    abstract suspend fun exists(rel: String): Boolean
    abstract suspend fun isDirectory(rel: String): Boolean
    abstract suspend fun readText(rel: String): String
    abstract suspend fun writeText(rel: String, content: String)
    abstract suspend fun list(rel: String): List<WorkspaceEntry>
    abstract fun realPathFor(rel: String): String?

    protected fun segments(rel: String): List<String> =
        rel.split('/').filter { it.isNotEmpty() && it != "." }
}

class FileWorkspace(val root: File) : Workspace() {
    override val displayName: String get() = root.absolutePath
    override val supportsRealPath: Boolean get() = true

    private fun file(rel: String): File = File(root, rel)

    override suspend fun exists(rel: String): Boolean = file(rel).exists()
    override suspend fun isDirectory(rel: String): Boolean = file(rel).isDirectory
    override suspend fun readText(rel: String): String = file(rel).readText()
    override suspend fun writeText(rel: String, content: String) {
        val f = file(rel)
        f.parentFile?.mkdirs()
        f.writeText(content)
    }

    override suspend fun list(rel: String): List<WorkspaceEntry> {
        val dir = file(rel)
        if (!dir.exists()) return emptyList()
        return (dir.listFiles() ?: emptyArray())
            .map { WorkspaceEntry(it.name, it.isDirectory, if (it.isDirectory) 0 else it.length()) }
            .sortedBy { it.name }
    }

    override fun realPathFor(rel: String): String? = file(rel).absolutePath
}

class SafWorkspace(
    private val context: Context,
    private val treeUri: Uri
) : Workspace() {
    override val displayName: String get() = treeUri.toString()
    override val supportsRealPath: Boolean get() = false

    private fun doc(rel: String): DocumentFile? {
        var cur = DocumentFile.fromTreeUri(context, treeUri) ?: return null
        for (seg in segments(rel)) {
            cur = cur.findFile(seg) ?: return null
        }
        return cur
    }

    override suspend fun exists(rel: String): Boolean = doc(rel)?.exists() ?: false

    override suspend fun isDirectory(rel: String): Boolean = doc(rel)?.isDirectory ?: false

    override suspend fun readText(rel: String): String {
        val d = doc(rel) ?: throw IOException("文件不存在: $rel")
        val input = context.contentResolver.openInputStream(d.uri)
            ?: throw IOException("无法打开文件: $rel")
        return input.bufferedReader().use { it.readText() }
    }

    override suspend fun writeText(rel: String, content: String) {
        val slash = rel.lastIndexOf('/')
        val parentRel = if (slash < 0) "" else rel.substring(0, slash)
        val name = if (slash < 0) rel else rel.substring(slash + 1)
        val parent = ensureDir(parentRel)
        val existing = parent.findFile(name)
        val target = existing ?: parent.createFile("*/*", name)
            ?: throw IOException("无法创建文件: $name")
        val output = context.contentResolver.openOutputStream(target.uri)
            ?: throw IOException("无法写入文件: $rel")
        output.bufferedWriter().use { it.write(content) }
    }

    private suspend fun ensureDir(rel: String): DocumentFile {
        var cur = DocumentFile.fromTreeUri(context, treeUri)
            ?: throw IOException("无法访问工作区根目录")
        for (seg in segments(rel)) {
            var next = cur.findFile(seg)
            if (next == null) {
                next = cur.createDirectory(seg) ?: throw IOException("无法创建目录: $seg")
            }
            cur = next
        }
        return cur
    }

    override suspend fun list(rel: String): List<WorkspaceEntry> {
        val d = doc(rel) ?: return emptyList()
        return (d.listFiles() ?: emptyArray())
            .map { WorkspaceEntry(it.name ?: "", it.isDirectory, if (it.isDirectory) 0 else it.length()) }
            .sortedBy { it.name }
    }

    override fun realPathFor(rel: String): String? = null
}
