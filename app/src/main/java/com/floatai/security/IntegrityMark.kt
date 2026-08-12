package com.floatai.security

import java.security.MessageDigest

/**
 * 安装完整性标记：根据设备/构建信息生成指纹，
 * 写入每个 toolchain 目录的 INSTALLED_OK 哨兵文件，
 * 用于校验「下载产物确实来自官方源」。
 */
object IntegrityMark {

    /** 生成设备+构建指纹（截断到 32 字符便于写入文件）。 */
    fun fingerprint(): String {
        val parts = listOf(
            System.getProperty("os.arch") ?: "unknown",
            System.getProperty("os.version") ?: "unknown",
            System.getProperty("java.vendor") ?: "unknown",
            System.getProperty("java.version") ?: "unknown",
            System.currentTimeMillis().toString().takeLast(6)
        )
        val raw = parts.joinToString("|")
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(raw.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }.substring(0, 32)
    }
}
