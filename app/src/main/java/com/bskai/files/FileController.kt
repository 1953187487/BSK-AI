package com.bskai.intent

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileOutputStream

class FileController(private val context: Context) {

    fun listFiles(path: String): List<FileEntry> {
        val file = resolvePath(path) ?: return emptyList()
        return file.listFiles()?.map { FileEntry(it.name, it.isDirectory, it.length(), it.absolutePath) }
            ?.sortedBy { it.name } ?: emptyList()
    }

    fun readFile(path: String): String? {
        return runCatching {
            resolvePath(path)?.readText()
        }.getOrNull()
    }

    fun writeFile(path: String, content: String): Result<String> {
        return runCatching {
            val file = resolvePath(path) ?: return@runCatching Result.failure(Exception("路径不存在: $path"))
            file.parentFile?.mkdirs()
            file.writeText(content)
            "已写入 ${content.length} 字符到 $path"
        }
    }

    fun moveFile(fromPath: String, toPath: String): Result<String> {
        return runCatching {
            val src = resolvePath(fromPath) ?: throw Exception("源文件不存在: $fromPath")
            val dest = resolvePath(toPath) ?: throw Exception("目标路径不存在: $toPath")
            if (!dest.parentFile!!.exists()) dest.parentFile!!.mkdirs()
            src.renameTo(dest)
            "已将 $fromPath 移动到 $toPath"
        }
    }

    fun copyFile(fromPath: String, toPath: String): Result<String> {
        return runCatching {
            val src = resolvePath(fromPath) ?: throw Exception("源文件不存在: $fromPath")
            val dest = resolvePath(toPath) ?: throw Exception("目标路径不存在: $toPath")
            if (!dest.parentFile!!.exists()) dest.parentFile!!.mkdirs()
            src.copyTo(dest, overwrite = true)
            "已将 $fromPath 复制到 $toPath"
        }
    }

    fun deleteFile(path: String, recursive: Boolean = false): Result<String> {
        return runCatching {
            val file = resolvePath(path) ?: throw Exception("文件不存在: $path")
            if (file.isDirectory && recursive) {
                file.deleteRecursively()
            } else {
                file.delete()
            }
            "已删除 $path"
        }
    }

    fun createDir(path: String): Result<String> {
        return runCatching {
            val dir = resolvePath(path) ?: throw Exception("路径不存在: $path")
            dir.mkdirs()
            "已创建目录: $path"
        }
    }

    fun searchFiles(keyword: String, maxResults: Int = 20): List<FileEntry> {
        val results = mutableListOf<FileEntry>()
        val storageDirs = listOf(
            Environment.getExternalStorageDirectory(),
            File("/sdcard"),
            File("/storage/emulated/0")
        )
        for (rootDir in storageDirs) {
            if (!rootDir.exists()) continue
            searchRecursive(rootDir, keyword.lowercase(), results, maxResults - results.size)
            if (results.size >= maxResults) break
        }
        return results
    }

    fun getStorageInfo(): StorageInfo {
        val ext = Environment.getExternalStorageDirectory()
        val total = ext.totalSpace
        val free = ext.freeSpace
        val used = total - free
        return StorageInfo(total, used, free)
    }

    private fun resolvePath(path: String): File? {
        return when {
            path.startsWith("/") -> File(path)
            path.startsWith("content://") -> null
            else -> File(Environment.getExternalStorageDirectory(), path)
        }.takeIf { it?.exists() == true || path.isEmpty() }
    }

    private fun searchRecursive(dir: File, keyword: String, results: MutableList<FileEntry>, limit: Int) {
        if (results.size >= limit) return
        if (!dir.exists() || !dir.isDirectory) return
        for (file in dir.listFiles() ?: emptyArray()) {
            if (results.size >= limit) return
            if (file.isDirectory) {
                searchRecursive(file, keyword, results, limit - results.size)
            } else if (file.name.lowercase().contains(keyword)) {
                results.add(FileEntry(file.name, false, file.length(), file.absolutePath))
            }
        }
    }
}

data class FileEntry(
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val path: String
)

data class StorageInfo(
    val total: Long,
    val used: Long,
    val free: Long
) {
    val usedPercent: Float get() = if (total > 0) used.toFloat() / total * 100 else 0f
}
