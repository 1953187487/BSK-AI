package com.floatai.perm

import android.content.Context
import android.content.pm.PackageManager
import com.floatai.App
import rikka.shizuku.Shizuku

/**
 * 单个授权来源的实时状态。
 */
sealed class GrantState {
    object Detecting : GrantState()
    data class Unavailable(val reason: String) : GrantState()
    object NeedsPermission : GrantState()
    object Requesting : GrantState()
    object Granted : GrantState()
    data class Failed(val message: String) : GrantState()
}

/**
 * 高权限来源枚举。
 */
enum class ElevatedGrant { SHIZUKU, DHIZUKU, ROOT, NONE }

/**
 * 高权限检测与请求（接入 Shizuku 官方 SDK 13.1.5）。
 *
 * 设计要点：
 *  - SHIZUKU：
 *      检测 = Shizuku.pingBinder() && Shizuku.checkSelfPermission() == GRANTED
 *      关键：在 ElevatedGrantFlow 进入屏幕前 binder 必须已收到（ShizukuProvider
 *            自动触发 ContentProvider#call("sendBinder", ...)）。
 *      请求 = Shizuku.requestPermission(requestCode)；结果由调用方注册的
 *            OnRequestPermissionResultListener 异步回调。
 *  - DHIZUKU：反射调用 com.itsaky.androidide.dhizukudav.Dhizuku。
 *      该依赖并非默认包含；当用户把 Dhizuku 客户端库加入 classpath 时启用。
 *  - ROOT：执行 `su -c true`；成功 = 返回 0。
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

    /** 检测 Shizuku：服务可达 + 已授权。 */
    fun hasShizuku(): Boolean = runCatching {
        if (Shizuku.isPreV11()) return@runCatching false
        Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    }.getOrDefault(false)

    /** 检测 Dhizuku（反射，classpath 缺失时返回 false）。 */
    fun hasDhizuku(): Boolean = runCatching {
        val cls = Class.forName("com.itsaky.androidide.dhizukudav.Dhizuku")
        val isAvailable = cls.getMethod("isAvailable").invoke(null) as? Boolean ?: false
        if (!isAvailable) return@runCatching false
        cls.getMethod("isPermissionGranted").invoke(null) as? Boolean ?: false
    }.getOrDefault(false)

    /** 通过 PackageManager 检查 Shizuku / Dhizuku 是否已安装（用于 UI 提示，未运行时友好降级）。 */
    fun isShizukuInstalled(context: Context): Boolean = runCatching {
        context.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
        true
    }.getOrDefault(false) || runCatching {
        context.packageManager.getPackageInfo("dev.rikka.shizuku", 0)
        true
    }.getOrDefault(false) || runCatching {
        // 第三方 Shizuku 客户端：org.exthmui.shizuku / 其他
        context.packageManager.getPackageInfo("org.exthmui.shizuku", 0)
        true
    }.getOrDefault(false)

    fun isDhizukuInstalled(context: Context): Boolean = runCatching {
        context.packageManager.getPackageInfo("com.itsaky.androidide.dhizukudav", 0)
        true
    }.getOrDefault(false) || runCatching {
        context.packageManager.getPackageInfo("com.rosan.dhizuku", 0)
        true
    }.getOrDefault(false)

    /** 是否具备 root。 */
    fun hasRoot(): Boolean = runCatching {
        val process = ProcessBuilder("su", "-c", "true").start()
        process.waitFor() == 0
    }.getOrDefault(false)
}
