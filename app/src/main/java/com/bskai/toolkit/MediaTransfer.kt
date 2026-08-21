package com.bskai.toolkit

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

/**
 * 通过公共 Download 目录在 BSK AI 与 Termux 之间传输项目与 APK。
 */
object MediaTransfer {

    fun publicProjectRoot(): File =
        File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "BSKAI/projects"
        )

    fun copyProjectToPublic(context: Context, projectName: String): File? {
        val src = File(
            (context.getExternalFilesDir(null) ?: context.filesDir),
            "projects/$projectName"
        )
        if (!src.exists()) return null
        val destDir = File(publicProjectRoot(), projectName)
        copyRecursive(src, destDir)
        scan(context, destDir)
        return destDir
    }

    fun publicApkFile(projectName: String): File =
        File(publicProjectRoot(), "$projectName/dist/$projectName.apk")

    private fun copyRecursive(src: File, dest: File) {
        if (src.isDirectory) {
            dest.mkdirs()
            src.listFiles()?.forEach { copyRecursive(it, File(dest, it.name)) }
        } else {
            dest.parentFile?.mkdirs()
            src.copyTo(dest, overwrite = true)
        }
    }

    fun importApkToDownloads(context: Context, apk: File): Uri? {
        if (!apk.exists()) return null
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, apk.name)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.android.package-archive")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/BSKAI")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = context.contentResolver.insert(collection, values) ?: return null
        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                apk.inputStream().use { it.copyTo(out) }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.contentResolver.update(uri, ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }, null, null)
        }
        scan(context, apk)
        return uri
    }

    private fun scan(context: Context, path: File) {
        runCatching {
            MediaScannerConnection.scanFile(context, arrayOf(path.absolutePath), null, null)
        }
    }
}
