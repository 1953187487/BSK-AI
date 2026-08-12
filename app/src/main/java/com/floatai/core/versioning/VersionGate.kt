package com.floatai.core.versioning

import android.content.Context
import android.content.SharedPreferences

/**
 * 协议版本闸门：每次发布新版本时递增 BuildConfig.PROTOCOL_VERSION，
 * 启动时若与本地存储不一致则强制用户重新签署协议。
 */
object VersionGate {

    private const val PREFS = "float_ai_version_gate"
    private const val KEY_PROTOCOL_VERSION = "protocol_version_agreed"

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** 用户当前已签署的协议版本号（0 = 从未签署）。 */
    fun agreedProtocolVersion(context: Context): Int =
        prefs(context).getInt(KEY_PROTOCOL_VERSION, 0)

    /** 当前应用要求的协议版本号（来自 BuildConfig）。 */
    fun requiredProtocolVersion(): Int = try {
        com.floatai.BuildConfig.PROTOCOL_VERSION
    } catch (_: Throwable) {
        // BuildConfig 缺失时降级为 1
        1
    }

    /** 是否需要重新签署协议。 */
    fun needsReSign(context: Context): Boolean =
        agreedProtocolVersion(context) < requiredProtocolVersion()

    /** 记录用户已签署当前协议版本。 */
    fun markAgreed(context: Context) {
        prefs(context).edit()
            .putInt(KEY_PROTOCOL_VERSION, requiredProtocolVersion())
            .apply()
    }
}
