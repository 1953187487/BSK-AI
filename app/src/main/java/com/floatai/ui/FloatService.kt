package com.floatai.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat

class FloatService : Service() {
    private var windowManager: WindowManager? = null
    private var menuView: LinearLayout? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private var downRawX = 0f
    private var downRawY = 0f
    private var downParamsX = 0
    private var downParamsY = 0
    private var moved = false

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification())

        // 无悬浮窗权限则不显示悬浮窗，避免崩溃
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "需要悬浮窗权限", Toast.LENGTH_SHORT).show()
            stopSelf()
            return START_NOT_STICKY
        }

        showMenu()
        return START_STICKY
    }

    private fun showMenu() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        removeExistingView()

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(6), dp(8), dp(6), dp(8))
        }

        // ① AI 聊天（打开 AI 聊天页）
        container.addView(makeButton("AI") {
            openMain(0)
        })
        // ② 进程查看（需 Shizuku/Dhizuku 授权）
        container.addView(makeButton("进程") {
            Toast.makeText(
                this@FloatService,
                "进程查看需 Shizuku/Dhizuku 授权，请到设置中授权",
                Toast.LENGTH_SHORT
            ).show()
            openMain(2)
        })
        // ③ API 配置（打开 API 配置页）
        container.addView(makeButton("API") {
            openMain(1)
        })

        // 拖动手势（拖动不触发点击）
        container.setOnTouchListener(dragListener())

        val type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.LEFT or Gravity.CENTER_VERTICAL
        params.x = 0
        params.y = 0
        layoutParams = params

        try {
            windowManager?.addView(container, params)
            menuView = container
        } catch (e: Exception) {
            // 悬浮窗失败不崩溃
        }
    }

    private fun makeButton(text: String, onClick: () -> Unit): TextView {
        val btn = TextView(this)
        btn.text = text
        btn.textSize = 13f
        btn.setTextColor(0xFFFFFFFF.toInt())
        btn.gravity = Gravity.CENTER
        btn.setPadding(dp(12), dp(10), dp(12), dp(10))
        btn.background = roundedBg(18, 0xE61A1330.toInt())
        btn.setOnClickListener {
            if (moved) {
                moved = false
                return@setOnClickListener
            }
            onClick()
        }
        (btn.layoutParams as? LinearLayout.LayoutParams)?.topMargin = dp(4)
        btn.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(4) }
        return btn
    }

    private fun dragListener() = View.OnTouchListener { view, event ->
        val params = layoutParams ?: return@OnTouchListener false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downRawX = event.rawX
                downRawY = event.rawY
                downParamsX = params.x
                downParamsY = params.y
                moved = false
                true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - downRawX
                val dy = event.rawY - downRawY
                if (Math.abs(dx) > touchSlop() || Math.abs(dy) > touchSlop()) {
                    moved = true
                }
                params.x = downParamsX + dx.toInt()
                params.y = downParamsY + dy.toInt()
                try {
                    windowManager?.updateViewLayout(view, params)
                } catch (e: Exception) {
                    // ignore
                }
                true
            }
            else -> false
        }
    }

    private fun openMain(tab: Int) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("tab", tab)
        }
        startActivity(intent)
    }

    private fun removeExistingView() {
        menuView?.let { v ->
            try {
                windowManager?.removeView(v)
            } catch (e: Exception) {
                // ignore
            }
            menuView = null
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
            .setContentText("悬浮窗已开启（AI / 进程 / API）")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .build()

    override fun onDestroy() {
        removeExistingView()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun touchSlop(): Int = dp(8)

    private fun roundedBg(radiusDp: Int, color: Int): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = (radiusDp * resources.displayMetrics.density)
            setColor(color)
        }

    companion object {
        private const val CHANNEL_ID = "float_service"
        private const val NOTIF_ID = 1
    }
}
