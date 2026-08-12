package com.floatai.tools

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import java.io.File
import java.util.zip.ZipFile

/**
 * APK 反编译器 v1.0.6-rc.2：
 *  - 解析 APK 元数据（包名、版本号、权限、Activity 等）
 *  - 列出 APK 内所有文件（classes.dex / resources.arsc / assets / res / lib）
 *  - 提取 AndroidManifest.xml 原始字节（提示用户用 aapt2 / apktool 进一步反编译）
 *
 *  局限：纯 Java 不能直接解析二进制 AndroidManifest.xml。需要进一步反编译
 *  请用：apktool d xxx.apk（外部工具）或 dex2jar + jd-cli。
 *
 *  后续：可集成 baksmali（dex 反汇编）的 JAR 库。
 */
object ApkInspector {

    data class ApkMetadata(
        val packageName: String,
        val versionName: String,
        val versionCode: Long,
        val minSdk: Int,
        val targetSdk: Int,
        val permissions: List<String>,
        val activities: List<String>,
        val services: List<String>,
        val receivers: List<String>,
        val providers: List<String>,
        val fileSize: Long,
        val apkFilePath: String
    )

    data class ApkEntry(
        val path: String,
        val size: Long,
        val compressedSize: Long,
        val isDirectory: Boolean
    )

    /**
     * 读取 APK 基本元数据（不需要解压，PackageManager 直接解析）。
     */
    fun readMetadata(context: Context, apkFile: File): ApkMetadata? {
        val pm = context.packageManager
        val packageInfo: PackageInfo = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageArchiveInfo(
                    apkFile.absolutePath,
                    PackageManager.PackageInfoFlags.of(0L)
                )
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageArchiveInfo(apkFile.absolutePath, 0)
            } ?: return null
        } catch (_: Exception) {
            return null
        }
        // 补全 applicationInfo（PackageManager 不会自动填）
        return ApkMetadata(
            packageName = packageInfo.packageName ?: "",
            versionName = packageInfo.versionName ?: "",
            versionCode = packageInfo.longVersionCodeCompat(),
            minSdk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                packageInfo.applicationInfo?.minSdkVersion ?: 0
            } else 0,
            targetSdk = packageInfo.applicationInfo?.targetSdkVersion ?: 0,
            permissions = packageInfo.requestedPermissions?.toList() ?: emptyList(),
            activities = packageInfo.activities?.map { it.name } ?: emptyList(),
            services = packageInfo.services?.map { it.name } ?: emptyList(),
            receivers = packageInfo.receivers?.map { it.name } ?: emptyList(),
            providers = packageInfo.providers?.map { it.name } ?: emptyList(),
            fileSize = apkFile.length(),
            apkFilePath = apkFile.absolutePath
        )
    }

    /**
     * 列出 APK 内所有文件（最多 500 条）。
     */
    fun listEntries(apkFile: File): List<ApkEntry> {
        return try {
            ZipFile(apkFile).use { zip ->
                zip.entries().asSequence().take(500).map { e ->
                    ApkEntry(
                        path = e.name,
                        size = e.size,
                        compressedSize = e.compressedSize,
                        isDirectory = e.isDirectory
                    )
                }.toList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * 提取 APK 内的 AndroidManifest.xml 原始字节（需要 aapt2 才能解析）。
     */
    fun extractManifestBytes(apkFile: File): ByteArray? {
        return try {
            ZipFile(apkFile).use { zip ->
                val entry = zip.getEntry("AndroidManifest.xml") ?: return null
                zip.getInputStream(entry).use { it.readBytes() }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun PackageInfo.longVersionCodeCompat(): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            longVersionCode
        } else {
            @Suppress("DEPRECATION")
            versionCode.toLong()
        }
    }
}
