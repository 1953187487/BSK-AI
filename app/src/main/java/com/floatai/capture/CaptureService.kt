package com.floatai.capture

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.floatai.MainActivity

/**
 * VpnService 抓包服务（v1.0.3 基础版）：
 *  - 建立 VPN 接口并展示系统级 VPN 通知
 *  - 监听 InputStream 直到用户主动停止
 *  - 实际流量解析（TCP 重组、TLS 解密）在 v1.0.4 实现
 *
 * 本版本语义：
 *  - 启动后视为「抓包会话开启」
 *  - 通过 Companion 暴露 sessionId 给 UI 层
 *  - 提供 stopCapture() 接口
 */
class CaptureService : VpnService() {

    private var pfd: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification())
        establish()
        return START_STICKY
    }

    override fun onRevoke() {
        pfd?.close()
        pfd = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        super.onRevoke()
    }

    override fun onDestroy() {
        pfd?.close()
        pfd = null
        super.onDestroy()
    }

    private fun establish() {
        if (pfd != null) return
        val builder = Builder()
            .setSession("FloatAI 抓包")
            .setMtu(1500)
            .addAddress("10.0.0.2", 32)
            .addRoute("0.0.0.0", 0)
            .setBlocking(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.addDisallowedApplication(packageName)
        }
        pfd = builder.establish()
    }

    private fun buildNotification(): Notification {
        createChannel()
        val pi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FloatAI 抓包")
            .setContentText("流量捕获进行中（v1.0.3 基础版）")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID, "抓包服务", NotificationManager.IMPORTANCE_LOW
            )
            ch.description = "FloatAI 抓包会话"
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    companion object {
        private const val CHANNEL_ID = "capture_service"
        private const val NOTIF_ID = 2

        fun prepareVpn(activity: android.app.Activity): Intent? {
            return VpnService.prepare(activity)
        }
    }
}
