package com.floatai.security

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Debug

/**
 * 简易反调试检测（v1.0.3 应用内置加固）：
 *  - 检测 Debug.isDebuggerConnected() — 当前是否被调试器附加
 *  - 检测 ApplicationInfo.FLAG_DEBUGGABLE — APK 是否为可调试签名
 *  - 检测 ro.debuggable 系统属性（无法直接读但可间接推断）
 *
 * 注意：纯应用层反调试只能延缓破解，不能完全阻止。建议结合：
 *  - R8/ProGuard 混淆（已启用）
 *  - 关键逻辑放到 native 层（v1.0.4 实现）
 */
object AntiDebug {

    /** 检测当前进程是否处于调试状态。 */
    fun isDebugging(): Boolean {
        return try {
            Debug.isDebuggerConnected()
        } catch (_: Throwable) {
            false
        }
    }

    /** 检测应用 APK 是否被签为可调试（通常意味着攻击者重新签名）。 */
    fun isDebuggable(context: Context): Boolean {
        return try {
            val flags = context.applicationInfo.flags
            (flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * 综合检测：当前在调试 或 APK 是可调试签名 → true。
     * 调用方可在敏感操作前调用，若 true 则降级或拒绝。
     */
    fun check(context: Context): Boolean = isDebugging() || isDebuggable(context)
}
