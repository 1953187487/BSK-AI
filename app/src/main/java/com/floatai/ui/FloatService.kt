package com.floatai.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat

class FloatService : Service() {
    private var windowManager: WindowManager? = null
    private var floatView: TextView? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification())

        // 无悬浮窗权限则不显示悬浮窗，避免崩溃
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        showFloatView()
        return START_STICKY
    }

    private fun showFloatView() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        removeExistingView()

        val tv = TextView(this)
        tv.text = "FloatAI 悬浮窗 - 进程查看"
        tv.setTextSize(14f)
        tv.setTypeface(Typeface.MONOSPACE)
        tv.gravity = Gravity.CENTER
        tv.setPadding(28, 28, 28, 28)
        tv.setTextColor(0xFF4ECDC4.toInt())
        tv.setBackgroundResource(android.R.drawable.editbox_background)
        floatView = tv

        val type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 100
        params.y = 200
        try {
            windowManager?.addView(tv, params)
        } catch (e: Exception) {
            // 悬浮窗失败不崩溃
        }
    }

    private fun removeExistingView() {
        floatView?.let { v ->
            try {
                windowManager?.removeView(v)
            } catch (e: Exception) {
                // ignore
            }
            floatView = null
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "悬浮窗服务",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "FloatAI 悬浮窗后台服务"
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification() =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FloatAI")
            .setContentText("悬浮窗已开启，查看系统进程")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .build()

    override fun onDestroy() {
        removeExistingView()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "float_service"
        private const val NOTIF_ID = 1
    }
}
