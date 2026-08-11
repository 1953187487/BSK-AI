package com.floatai.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationCompat

/**
 * 悬浮窗服务：圆形快捷按钮 + 展开菜单 + 拖拽移动。
 */
class FloatService : Service() {

    private var windowManager: WindowManager? = null
    private var container: LinearLayout? = null
    private var params: WindowManager.LayoutParams? = null

    private var downRawX = 0f
    private var downRawY = 0f
    private var downParamsX = 0
    private var downParamsY = 0
    private var moved = false

    private var isExpanded = false
    private var expandedMenu: LinearLayout? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "需要悬浮窗权限", Toast.LENGTH_SHORT).show()
            stopSelf()
            return START_NOT_STICKY
        }
        showFloat()
        return START_STICKY
    }

    private fun showFloat() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        removeView()

        val circleBtn = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_myplaces)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setBackgroundColor(0x8A181226.toInt())
            setPadding(dp(14), dp(14), dp(14), dp(14))
        }

        val menuView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(6), dp(8), dp(6))
            visibility = View.GONE
        }

        menuView.addView(makeMenuItem("AI 聊天", "展开对话", 0xFF4ECDC4.toInt()) { toggleMenu() })
        menuView.addView(makeMenuItem("进程列表", "查看运行进程", 0xFF5B8DEF.toInt()) {
            showProcessListDialog()
            toggleMenu()
        })
        menuView.addView(makeMenuItem("关于", "FloatAI v1.0.0", 0xFF888888.toInt()) {
            showAboutDialog()
            toggleMenu()
        })

        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            addView(circleBtn)
            addView(menuView)
            setOnClickListener {
                if (moved) {
                    moved = false
                    return@setOnClickListener
                }
                toggleMenu()
            }
            setOnTouchListener(object : View.OnTouchListener {
                override fun onTouch(v: View, event: MotionEvent): Boolean {
                    val p = params ?: return false
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            downRawX = event.rawX
                            downRawY = event.rawY
                            downParamsX = p.x
                            downParamsY = p.y
                            moved = false
                            return true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            p.x = (downParamsX + (event.rawX - downRawX)).toInt()
                            p.y = (downParamsY + (event.rawY - downRawY)).toInt()
                            try {
                                windowManager?.updateViewLayout(v, p)
                            } catch (_: Exception) {
                            }
                            if (Math.abs(event.rawX - downRawX) > touchSlop() ||
                                Math.abs(event.rawY - downRawY) > touchSlop()
                            ) {
                                moved = true
                            }
                            return true
                        }
                        else -> return false
                    }
                }
            })
        }

        expandedMenu = menuView

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.LEFT or Gravity.CENTER_VERTICAL
            x = 0
            y = 0
        }

        try {
            windowManager?.addView(outer, params)
            container = outer
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun makeMenuItem(
        title: String,
        subtitle: String,
        color: Int = 0xFF4ECDC4.toInt(),
        onClick: () -> Unit
    ): LinearLayout {
        val inner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(14), dp(8), dp(14), dp(8))
            background = roundedBg(dp(10), 0xAA181226.toInt())
        }

        inner.addView(TextView(this).apply {
            text = title
            textSize = 13f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, dp(4))
        })

        inner.addView(TextView(this).apply {
            text = subtitle
            textSize = 10f
            setTextColor(color)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(6))
        })

        inner.setOnClickListener {
            if (expandedMenu?.visibility != View.VISIBLE) {
                toggleMenu()
            }
            onClick()
        }

        inner.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0, dp(4), 0, dp(4)) }

        return inner
    }

    private fun toggleMenu() {
        expandedMenu?.visibility = if (isExpanded) View.GONE else View.VISIBLE
        isExpanded = !isExpanded
    }

    private fun showProcessListDialog() {
        val usageManager = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val oneWeek = 7 * 24 * 60 * 60 * 1000L
        val stats: List<UsageStats> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            usageManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - oneWeek, now)
        } else {
            emptyList()
        }

        val items = stats
            .groupBy { it.packageName }
            .mapNotNull { (pkg, list) ->
                val best = list.maxByOrNull { it.lastTimeUsed } ?: return@mapNotNull null
                ProcessItem(pkg, best.totalTimeInForeground, best.lastTimeUsed)
            }
            .sortedByDescending { it.lastTime }

        if (items.isEmpty()) {
            Toast.makeText(this, "未发现运行中的进程", Toast.LENGTH_SHORT).show()
            return
        }

        val titles = items.map { it.label }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("运行中的进程")
            .setItems(titles) { _, which ->
                Toast.makeText(this, "已选择: ${items[which].label}", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("刷新") { _, _ -> showProcessListDialog() }
            .show()
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle("关于 FloatAI")
            .setMessage(
                "版本: v1.0.0\n\n" +
                    "全新 1.0 版本：\n" +
                    "- MVVM + Navigation Compose 架构\n" +
                    "- Material 3 动态主题\n" +
                    "- 对话历史持久化\n" +
                    "- 优化悬浮窗交互"
            )
            .setPositiveButton("确定") { _, _ -> }
            .show()
    }

    private fun removeView() {
        container?.let { v ->
            try {
                windowManager?.removeView(v)
            } catch (_: Exception) {
            }
            container = null
            expandedMenu = null
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "悬浮窗", NotificationManager.IMPORTANCE_LOW)
            ch.description = "FloatAI 悬浮窗服务"
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    private fun buildNotification() =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FloatAI v1.0")
            .setContentText("悬浮窗服务运行中")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .build()

    override fun onDestroy() {
        removeView()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun touchSlop(): Int = dp(6)

    private fun roundedBg(radiusDp: Int, color: Int): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = (radiusDp * resources.displayMetrics.density)
            setColor(color)
        }

    data class ProcessItem(val label: String, val activeTime: Long, val lastTime: Long)

    companion object {
        private const val CHANNEL_ID = "float_service"
        private const val NOTIF_ID = 1
    }
}
