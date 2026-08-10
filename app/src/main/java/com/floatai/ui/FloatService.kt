package com.floatai.ui

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

class FloatService : Service() {
    private var windowManager: WindowManager? = null
    private var container: LinearLayout? = null
    private var params: WindowManager.LayoutParams? = null

    // 拖拽状态
    private var downRawX = 0f
    private var downRawY = 0f
    private var downParamsX = 0
    private var downParamsY = 0
    private var moved = false

    // 展开内容
    private var isExpanded = false
    private var expandedLinearLayout: LinearLayout? = null

    // 进程选择
    private var selectedApp: String? = null
    private var currentProcesses: List<ProcessItem> = emptyList()

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Need overlay permission", Toast.LENGTH_SHORT).show()
            stopSelf()
            return START_NOT_STICKY
        }
        showFloat()
        return START_STICKY
    }

    private fun showFloat() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        removeView()

        // 圆形主按钮
        val circleBtn = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_myplaces)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setBackgroundColor(0x8A1A1330.toInt())
            setPadding(dp(14), dp(14), dp(14), dp(14))
        }

        // 展开的子菜单
        val menuView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(6), dp(8), dp(6))
            setVisibility(View.GONE)
        }

        menuView.addView(makeMenuItem("AI 聊天", "\u2193 \u5C55\u5F00\u804A\u5929", color = 0xFF4ECDC4.toInt()) {
            toggleMenu()
        })

        menuView.addView(makeMenuItem("Process", "Process list", color = 0xFF4ECDC4.toInt()) {
            showProcessListDialog()
            toggleMenu()
        })

        menuView.addView(makeMenuItem("About", "About FloatAI v0.3.0", color = 0xFF888888.toInt()) {
            showAboutDialog()
            toggleMenu()
        })

        // 最终容器：圆形按钮 + 展开菜单
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
                            try { windowManager?.updateViewLayout(v, p) } catch (_: Exception) {}
                            if (Math.abs(event.rawX - downRawX) > touchSlop() ||
                                Math.abs(event.rawY - downRawY) > touchSlop()) {
                                moved = true
                            }
                            return true
                        }
                        else -> return false
                    }
                }
            })
        }

        expandedLinearLayout = menuView

        val type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
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
        onClick: () -> Unit = {}
    ): LinearLayout {
        val context = this
        val inner = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(14), dp(8), dp(14), dp(8))
            background = roundedBg(dp(10), 0xAA1A1330.toInt())
        }

        inner.addView(TextView(context).apply {
            text = title
            textSize = 13f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, dp(4))
        })

        inner.addView(TextView(context).apply {
            text = subtitle
            textSize = 10f
            setTextColor(color)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(6))
        })

        inner.setOnClickListener {
            if (expandedLinearLayout?.visibility != View.VISIBLE) {
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
        expandedLinearLayout?.visibility = if (isExpanded) View.GONE else View.VISIBLE
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

        currentProcesses = stats
            .groupBy { it.packageName }
            .map { (pkg, lst) ->
                val best = lst.maxByOrNull { it.lastTimeUsed } ?: return@map null
                val label = pkg
                val mem = best.totalTimeInForeground
                val pid = 0
                ProcessItem(label, pkg, pid, mem, best.lastTimeUsed)
            }
            .filterNotNull()
            .sortedByDescending { it.lastTime }

        val items = currentProcesses.toTypedArray()
        if (items.isEmpty()) {
            Toast.makeText(this, "No processes found", Toast.LENGTH_SHORT).show()
            return
        }

        val titles = items.map { "${it.label} (PID: ${it.pid})" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Running Processes")
            .setItems(titles) { _, which ->
                val item = items[which]
                selectedApp = item.packageName
                Toast.makeText(this, "Selected: ${item.label}", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Change") { _, _ -> showProcessListDialog() }
            .show()
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle("About FloatAI")
            .setMessage("Version: v0.3.0\n\nLog:\n- New float window process management\n- Refactored chat storage\n- Optimized liquid glass rendering\n- Dual UI engine switch")
            .setPositiveButton("OK") { _, _ -> {} }
            .show()
    }

    private fun removeView() {
        container?.let { v ->
            try { windowManager?.removeView(v) } catch (_: Exception) {}
            container = null
            expandedLinearLayout = null
        }
    }

    private fun createChannel() = run {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel("float_service", "悬浮窗", NotificationManager.IMPORTANCE_LOW)
            ch.description = "FloatAI float window"
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    private fun buildNotification() =
        NotificationCompat.Builder(this, "float_service")
            .setContentTitle("FloatAI v0.3")
            .setContentText("Float window service is running")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .build()

    override fun onDestroy() = run {
        removeView()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
    private fun touchSlop(): Int = dp(6)

    private fun roundedBg(radiusDp: Int, color: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = (radiusDp * resources.displayMetrics.density)
            setColor(color)
        }
    }

    data class ProcessItem(val label: String, val packageName: String, val pid: Int, val activeTime: Long, val lastTime: Long)

    companion object {
        private const val CHANNEL_ID = "float_service"
        private const val NOTIF_ID = 1
    }
}
