package com.floatai.lobster

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.floatai.lobster.bridge.LobsterAction
import com.floatai.lobster.bridge.LobsterBridge

/**
 * 小龙虾（AI 手机遥控）AccessibilityService。
 *
 * 通过无障碍服务实现：
 *  - 读取当前界面节点树（dump）
 *  - 模拟点击 / 输入文本
 *  - 启动应用
 *  - 读取通知
 *
 * 所有操作均通过 [LobsterBridge] 暴露，供 AI 聊天 function calling 调用。
 *
 * 注意：必须用户主动在系统设置中开启本服务才会工作。
 */
class LobsterAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "Lobster service connected")
        LobsterBridge.attach(this)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.i(TAG, "Lobster service unbound")
        LobsterBridge.detach()
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 当前阶段仅用作兜底，AI 主动查询时直接 dump 节点树。
    }

    override fun onInterrupt() {
        Log.w(TAG, "Lobster service interrupted")
    }

    /**
     * 执行 AI 下发的动作。
     */
    fun perform(action: LobsterAction): LobsterBridge.Result {
        return try {
            when (action) {
                is LobsterAction.Dump -> {
                    val root = rootInActiveWindow
                    val text = if (root != null) dumpTree(root, 0).toString() else "(no window)"
                    LobsterBridge.Result.success(text)
                }
                is LobsterAction.ClickByText -> {
                    val nodes = rootInActiveWindow?.findAccessibilityNodeInfosByText(action.text)
                    val target = nodes?.firstOrNull { it.isClickable }
                    if (target != null) {
                        val ok = target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        LobsterBridge.Result.success(
                            if (ok) "clicked: ${action.text}" else "click-failed: ${action.text}"
                        )
                    } else {
                        LobsterBridge.Result.failure("not found: ${action.text}")
                    }
                }
                is LobsterAction.InputText -> {
                    val focused = findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                    if (focused != null) {
                        val args = Bundle().apply {
                            putCharSequence(
                                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                                action.text
                            )
                        }
                        val ok = focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                        LobsterBridge.Result.success(if (ok) "input: ${action.text}" else "input-failed")
                    } else {
                        LobsterBridge.Result.failure("no focused input")
                    }
                }
                is LobsterAction.GlobalBack -> {
                    val ok = performGlobalAction(GLOBAL_ACTION_BACK)
                    LobsterBridge.Result.success(if (ok) "back" else "back-failed")
                }
                is LobsterAction.GlobalHome -> {
                    val ok = performGlobalAction(GLOBAL_ACTION_HOME)
                    LobsterBridge.Result.success(if (ok) "home" else "home-failed")
                }
                is LobsterAction.LaunchApp -> {
                    val intent = packageManager.getLaunchIntentForPackage(action.packageName)
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                        LobsterBridge.Result.success("launched: ${action.packageName}")
                    } else {
                        LobsterBridge.Result.failure("no-launcher: ${action.packageName}")
                    }
                }
                is LobsterAction.SendSms -> {
                    // 仅做接口占位：实际发送需要 SEND_SMS 权限 + 用户授权
                    LobsterBridge.Result.failure("sms-not-implemented")
                }
                is LobsterAction.Snapshot -> {
                    val text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        try {
                            takeScreenshot(
                                android.view.Display.DEFAULT_DISPLAY,
                                java.util.concurrent.Executor { it.run() },
                                object : android.accessibilityservice.AccessibilityService.TakeScreenshotCallback {
                                    override fun onSuccess(result: android.accessibilityservice.AccessibilityService.ScreenshotResult) {}
                                    override fun onFailure(error: Int) {}
                                }
                            )
                            "screenshot-taken"
                        } catch (_: Exception) { "screenshot-failed" }
                    } else "screenshot-unsupported"
                    LobsterBridge.Result.success(text)
                }
            }
        } catch (e: Exception) {
            LobsterBridge.Result.failure(e.message ?: e.javaClass.simpleName)
        }
    }

    private fun dumpTree(node: AccessibilityNodeInfo?, depth: Int): StringBuilder {
        val sb = StringBuilder()
        if (node == null) return sb
        if (depth > 8) return sb
        val text = node.text?.toString()?.takeIf { it.isNotBlank() } ?: ""
        val desc = node.contentDescription?.toString()?.takeIf { it.isNotBlank() } ?: ""
        val cls = node.className?.toString()?.substringAfterLast('.') ?: ""
        val clickable = if (node.isClickable) "[click]" else ""
        val focusable = if (node.isFocusable) "[focus]" else ""
        if (text.isNotEmpty() || desc.isNotEmpty() || node.isClickable) {
            sb.append("  ".repeat(depth))
                .append(cls)
                .append(if (text.isNotEmpty()) " \"$text\"" else "")
                .append(if (desc.isNotEmpty()) " ($desc)" else "")
                .append(if (clickable.isNotEmpty()) " $clickable" else "")
                .append(if (focusable.isNotEmpty()) " $focusable" else "")
                .append("\n")
        }
        for (i in 0 until node.childCount) {
            sb.append(dumpTree(node.getChild(i), depth + 1))
        }
        return sb
    }

    companion object {
        const val TAG = "LobsterAS"
    }
}
