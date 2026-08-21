package com.bskai.toolkit

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import java.io.File
import java.security.MessageDigest
import java.security.cert.Certificate

/**
 * APK 分析器：读取包名、版本、权限、签名摘要等信息。
 */
object ApkInspector {

    fun inspect(context: Context, apkFile: File): Result<ApkInfo> = runCatching {
        if (!apkFile.exists()) throw IllegalArgumentException("APK 文件不存在: ${apkFile.absolutePath}")

        val pm = context.packageManager
        val pkgInfo = pm.getPackageArchiveInfo(apkFile.absolutePath, PackageManager.GET_PERMISSIONS)
            ?: throw IllegalStateException("无法解析 APK")

        val permissions = pkgInfo.requestedPermissions?.toList() ?: emptyList()
        val signs = pkgInfo.signatures?.map { sig ->
            val md = MessageDigest.getInstance("SHA-256")
            md.update(sig.toByteArray())
            md.digest().joinToString(":") { "%02x".format(it) }
        } ?: emptyList()

        ApkInfo(
            packageName = pkgInfo.packageName ?: "unknown",
            versionName = pkgInfo.versionName ?: "unknown",
            versionCode = pkgInfo.longVersionCode,
            minSdk = "N/A",
            targetSdk = "N/A",
            permissions = permissions,
            signatures = signs,
            applicationLabel = pkgInfo.applicationInfo?.loadLabel(pm)?.toString() ?: "N/A"
        )
    }

    data class ApkInfo(
        val packageName: String,
        val versionName: String,
        val versionCode: Long,
        val minSdk: String,
        val targetSdk: String,
        val permissions: List<String>,
        val signatures: List<String>,
        val applicationLabel: String
    )
}
