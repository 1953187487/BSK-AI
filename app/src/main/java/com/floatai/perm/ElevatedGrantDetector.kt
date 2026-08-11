package com.floatai.perm

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

/**
 * 高权限来源枚举。
 * - SHIZUKU：通过 Shizuku 服务获得 ADB 级权限
 * - DHIZUKU：通过 Dhizuku（设备管理员激活）授权
 * - ROOT：通过 su 直接提权
 */
enum class ElevatedGrant { SHIZUKU, DHIZUKU, ROOT, NONE }

/**
 * 高权限检测器。
 *
 * - SHIZUKU：dev.rikka.shizuku:api 13.1.5；通过 pingBinder() 判断服务是否在线
 * - DHIZUKU：反射调用 com.itsaky.androidide.dhizukudav.Dhizuku（仅在 classpath 含此依赖时生效）
 * - ROOT：执行 `su -c true`，返回 0 即具备 root
 */
object ElevatedGrantDetector {

    fun detect(): ElevatedGrant = when {
        hasShizuku() -> ElevatedGrant.SHIZUKU
        hasDhizuku() -> ElevatedGrant.DHIZUKU
        hasRoot() -> ElevatedGrant.ROOT
        else -> ElevatedGrant.NONE
    }

    fun describe(grant: ElevatedGrant): String = when (grant) {
        ElevatedGrant.SHIZUKU -> "Shizuku 已授权"
        ElevatedGrant.DHIZUKU -> "Dhizuku 已授权"
        ElevatedGrant.ROOT -> "Root 已授权"
        ElevatedGrant.NONE -> "未授权"
    }

    private fun hasShizuku(): Boolean = runCatching {
        if (Shizuku.isPreV11()) return@runCatching false
        Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    }.getOrDefault(false)

    private fun hasDhizuku(): Boolean = runCatching {
        val cls = Class.forName("com.itsaky.androidide.dhizukudav.Dhizuku")
        val isAvailable = cls.getMethod("isAvailable").invoke(null) as? Boolean ?: false
        if (!isAvailable) return@runCatching false
        val isGranted = cls.getMethod("isPermissionGranted").invoke(null) as? Boolean ?: false
        isGranted
    }.getOrDefault(false)

    private fun hasRoot(): Boolean = runCatching {
        val process = ProcessBuilder("su", "-c", "true").start()
        process.waitFor() == 0
    }.getOrDefault(false)
}
