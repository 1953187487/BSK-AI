package com.floatai.lobster.bridge

import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo

/**
 * 小龙虾（AI 手机遥控）的 AI 操作意图集合。
 * 作为 sealed class 暴露给 AI chat function calling 使用。
 */
sealed class LobsterAction {
    /** 导出当前界面文本树。 */
    data class Dump(val maxDepth: Int = 6) : LobsterAction()

    /** 按文本点击屏幕上的元素。 */
    data class ClickByText(val text: String) : LobsterAction()

    /** 在当前焦点输入框中输入文本。 */
    data class InputText(val text: String) : LobsterAction()

    /** 返回键。 */
    data object GlobalBack : LobsterAction()

    /** Home 键。 */
    data object GlobalHome : LobsterAction()

    /** 启动指定包名的应用。 */
    data class LaunchApp(val packageName: String) : LobsterAction()

    /** 发送短信（占位，需 SEND_SMS 权限）。 */
    data class SendSms(val phone: String, val body: String) : LobsterAction()

    /** 截图（API 30+）。 */
    data object Snapshot : LobsterAction()
}

/**
 * AI 调用小龙虾的桥接对象。
 *  - attach/detach 由 LobsterAccessibilityService 在 onServiceConnected / onUnbind 调用
 *  - perform 由 AI chat 通过反射或显式调用触发
 */
object LobsterBridge {

    data class Result(val ok: Boolean, val message: String) {
        companion object {
            fun success(msg: String) = Result(true, msg)
            fun failure(msg: String) = Result(false, msg)
        }
    }

    @Volatile
    private var service: com.floatai.lobster.LobsterAccessibilityService? = null

    fun attach(s: com.floatai.lobster.LobsterAccessibilityService) {
        service = s
    }

    fun detach() {
        service = null
    }

    /** 当前是否启用小龙虾（无障碍服务已连接）。 */
    fun isReady(): Boolean = service != null

    /** 执行一个 AI 操作。服务未启用时返回 failure。 */
    fun perform(action: LobsterAction): Result {
        val s = service ?: return Result.failure("lobster-service-disabled")
        return s.perform(action)
    }

    /** 异步等待服务启用（最多 timeoutMs 毫秒）。 */
    suspend fun awaitReady(timeoutMs: Long = 3000L): Boolean {
        val start = SystemClock.uptimeMillis()
        while (!isReady() && SystemClock.uptimeMillis() - start < timeoutMs) {
            kotlinx.coroutines.delay(150)
        }
        return isReady()
    }
}
