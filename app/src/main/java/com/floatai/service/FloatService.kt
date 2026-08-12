package com.floatai.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Debug
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.floatai.App
import com.floatai.R
import com.floatai.data.model.ChatMessage
import com.floatai.data.remote.ChatResult
import com.floatai.data.remote.OpenAiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * 悬浮窗服务 v2：
 *  - 屏幕边缘一个圆形快捷按钮（FAB）
 *  - 拖拽移动
 *  - 点击 FAB 弹出面板（不是简单菜单，是真正的对话框式面板）
 *  - 面板内两个 Tab：
 *      1. AI 聊天：直接输入消息，调用当前配置的 OpenAI 兼容 API；附带「历史」按钮
 *      2. 应用：列出本机最近使用过的应用（无需 QUERY_ALL_PACKAGES 权限，
 *         用 UsageStatsManager 读最近一周活跃应用），点击用 Intent 启动
 *  - 顶部关闭按钮 + 状态徽章
 *
 * 设计原则：
 *  - 不读取其他应用进程列表
 *  - 不修改其他应用内存
 *  - 启动应用 = Intent.ACTION_MAIN + CATEGORY_LAUNCHER（标准 API）
 */
class FloatService : Service() {

    private var windowManager: WindowManager? = null
    private var fabContainer: View? = null
    private var panelContainer: LinearLayout? = null
    private var fabParams: WindowManager.LayoutParams? = null

    private var downRawX = 0f
    private var downRawY = 0f
    private var downParamsX = 0
    private var downParamsY = 0
    private var moved = false

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

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
        showFab()
        return START_STICKY
    }

    override fun onDestroy() {
        removeAll()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ===== FAB =====

    private fun showFab() {
        if (windowManager == null) {
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        }
        removeFab()
        val ctx = this

        val fab = ImageView(ctx).apply {
            setImageResource(android.R.drawable.ic_dialog_dialer)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = roundedDrawable(dp(28).toFloat(), 0xEE5B5BEF.toInt(), 0xFF6E6EFF.toInt())
            setOnClickListener {
                if (!moved) togglePanel()
            }
            setOnTouchListener(object : View.OnTouchListener {
                override fun onTouch(v: View, event: MotionEvent): Boolean {
                    val p = fabParams ?: return false
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
                                Math.abs(event.rawY - downRawY) > touchSlop()
                            ) {
                                moved = true
                            }
                            return true
                        }
                        MotionEvent.ACTION_UP -> {
                            if (!moved) v.performClick()
                            return true
                        }
                        else -> return false
                    }
                }
            })
        }

        fabParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.LEFT or Gravity.CENTER_VERTICAL
            x = dp(8)
            y = 0
        }

        try {
            windowManager?.addView(fab, fabParams)
            fabContainer = fab
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun removeFab() {
        fabContainer?.let {
            try { windowManager?.removeView(it) } catch (_: Exception) {}
            fabContainer = null
        }
    }

    private fun removeAll() {
        try { windowManager?.removeView(panelContainer) } catch (_: Exception) {}
        try { windowManager?.removeView(fabContainer) } catch (_: Exception) {}
        panelContainer = null
        fabContainer = null
    }

    // ===== 面板 =====

    private var currentTab = 0 // 0 = AI, 1 = 应用

    private fun togglePanel() {
        if (panelContainer != null) {
            closePanel()
        } else {
            openPanel()
        }
    }

    private fun openPanel() {
        val ctx = this
        val panel = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedDrawable(dp(20).toFloat(), 0xF5151525.toInt(), 0)
            setPadding(dp(16), dp(12), dp(16), dp(16))
        }

        // 顶部：标题 + 关闭按钮
        val header = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val title = TextView(ctx).apply {
            text = "FloatAI"
            textSize = 16f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(0xFFFFFFFF.toInt())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val closeBtn = TextView(ctx).apply {
            text = "✕"
            textSize = 18f
            setPadding(dp(12), dp(4), dp(12), dp(4))
            setTextColor(0xFFCCCCCC.toInt())
            setOnClickListener { closePanel() }
        }
        header.addView(title)
        header.addView(closeBtn)
        panel.addView(header)

        // Tab 切换
        val tabRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(10), 0, dp(10))
        }
        val tabAi = makeTab("AI 聊天", currentTab == 0) {
            currentTab = 0; renderTabs(); renderContent()
        }
        val tabApps = makeTab("应用", currentTab == 1) {
            currentTab = 1; renderTabs(); renderContent()
        }
        tabRow.addView(tabAi)
        tabRow.addView(tabApps)
        tabRow.addView(TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
        })
        tabRow.tag = listOf(tabAi, tabApps)
        panel.addView(tabRow)

        // 内容容器
        val content = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            tag = "content"
        }
        panel.addView(content)

        val params = WindowManager.LayoutParams(
            dp(300),
            dp(460),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.LEFT or Gravity.CENTER_VERTICAL
            x = dp(60)
            y = 0
        }
        try {
            windowManager?.addView(panel, params)
            panelContainer = panel
            renderTabs()
            renderContent()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun closePanel() {
        panelContainer?.let {
            try { windowManager?.removeView(it) } catch (_: Exception) {}
            panelContainer = null
        }
    }

    private fun renderTabs() {
        val panel = panelContainer ?: return
        val tabRow = panel.getChildAt(1) as? LinearLayout ?: return
        val tags = tabRow.tag as? List<*> ?: return
        (tags[0] as? TextView)?.let {
            it.setBackgroundColor(if (currentTab == 0) 0xFF5B5BEF.toInt() else 0x00000000)
            it.setTextColor(if (currentTab == 0) 0xFFFFFFFF.toInt() else 0xFFAAAAAA.toInt())
        }
        (tags[1] as? TextView)?.let {
            it.setBackgroundColor(if (currentTab == 1) 0xFF5B5BEF.toInt() else 0x00000000)
            it.setTextColor(if (currentTab == 1) 0xFFFFFFFF.toInt() else 0xFFAAAAAA.toInt())
        }
    }

    private fun renderContent() {
        val panel = panelContainer ?: return
        val content = panel.findViewWithTag<LinearLayout>("content") ?: return
        content.removeAllViews()
        if (currentTab == 0) renderAiTab(content) else renderAppsTab(content)
    }

    // ----- AI Tab -----

    private fun renderAiTab(parent: LinearLayout) {
        val ctx = this

        val listView = ListView(ctx).apply {
            divider = null
            setBackgroundColor(0x00000000)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        val adapter = ChatAdapter(ctx, _messages.value)
        listView.adapter = adapter
        listView.tag = adapter
        parent.addView(listView)

        val inputRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(8), 0, 0)
            gravity = Gravity.CENTER_VERTICAL
        }
        val input = EditText(ctx).apply {
            hint = "说点什么..."
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFF888888.toInt())
            background = roundedDrawable(dp(10).toFloat(), 0xFF1F1F30.toInt(), 0)
            setPadding(dp(10), dp(8), dp(10), dp(8))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val sendBtn = Button(ctx).apply {
            text = "发送"
            setBackgroundColor(0xFF5B5BEF.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            setOnClickListener {
                val text = input.text.toString().trim()
                if (text.isNotEmpty()) {
                    input.setText("")
                    sendAiMessage(text, listView)
                }
            }
        }
        val histBtn = Button(ctx).apply {
            text = "历史"
            setBackgroundColor(0xFF2D2D45.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            setOnClickListener {
                showHistoryDialog()
            }
        }
        inputRow.addView(input, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        inputRow.addView(sendBtn, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            setMargins(dp(6), 0, dp(6), 0)
        })
        inputRow.addView(histBtn, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        parent.addView(inputRow)

        // 监听消息变化
        scope.launch {
            _messages.collect { msgs ->
                adapter.update(msgs)
                if (msgs.isNotEmpty()) listView.smoothScrollToPosition(msgs.size - 1)
            }
        }
    }

    private fun sendAiMessage(text: String, listView: ListView) {
        val app = applicationContext as? App ?: return
        val config = app.settingsRepository.apiConfig.value
        if (config.baseUrl.isBlank() || config.apiKey.isBlank()) {
            Toast.makeText(this, "请先在主 App 配置 Base URL 和 API Key", Toast.LENGTH_SHORT).show()
            return
        }
        val newMsgs = _messages.value + ChatMessage("user", text)
        _messages.value = newMsgs
        scope.launch(Dispatchers.IO) {
            val result = OpenAiClient.chatCompletions(
                baseUrl = config.baseUrl,
                apiKey = config.apiKey,
                model = config.model.ifBlank { "auto" },
                messages = newMsgs
            )
            val reply = when (result) {
                is ChatResult.Success -> result.content
                is ChatResult.Error -> "⚠ ${result.message}"
            }
            _messages.value = _messages.value + ChatMessage("assistant", reply)
            // 保存到主 App 的历史
            app.chatRepository.appendToCurrent(newMsgs + ChatMessage("assistant", reply))
        }
    }

    private fun showHistoryDialog() {
        val app = applicationContext as? App ?: return
        val histories = app.chatRepository.loadHistories()
        if (histories.isEmpty()) {
            Toast.makeText(this, "暂无历史记录", Toast.LENGTH_SHORT).show()
            return
        }
        val titles = histories.map { it.title }.toTypedArray()
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("历史对话")
            .setItems(titles) { _, which ->
                val h = histories[which]
                _messages.value = h.messages.ifEmpty { _messages.value }
            }
            .setNegativeButton("关闭", null)
            .show()
    }

    // ----- Apps Tab -----

    private fun renderAppsTab(parent: LinearLayout) {
        val ctx = this

        val title = TextView(ctx).apply {
            text = "最近使用的应用"
            textSize = 12f
            setTextColor(0xFFAAAAAA.toInt())
            setPadding(0, dp(4), 0, dp(8))
        }
        parent.addView(title)

        // 应用进程信息（仅本应用自身）
        val memInfo = Debug.MemoryInfo().also { Debug.getMemoryInfo(it) }
        val pid = android.os.Process.myPid()
        val ownInfo = TextView(ctx).apply {
            text = buildString {
                append("本应用 (PID: $pid)\n")
                append("PSS: ${memInfo.totalPss} KB / ${memInfo.totalPss / 1024} MB")
            }
            textSize = 11f
            setTextColor(0xFFCCCCCC.toInt())
            setPadding(dp(8), dp(6), dp(8), dp(6))
            background = roundedDrawable(dp(8).toFloat(), 0xFF1F1F30.toInt(), 0)
        }
        parent.addView(ownInfo)

        val listView = ListView(ctx).apply {
            divider = null
            setBackgroundColor(0x00000000)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        parent.addView(listView)

        val apps = loadRecentApps()
        if (apps.isEmpty()) {
            val empty = TextView(ctx).apply {
                text = "未发现最近活动。\n请授予「使用情况访问权限」后查看。"
                textSize = 12f
                setTextColor(0xFF888888.toInt())
                gravity = Gravity.CENTER
                setPadding(dp(16), dp(20), dp(16), dp(20))
            }
            parent.addView(empty)
            return
        }
        listView.adapter = AppsAdapter(ctx, apps)
        listView.setOnItemClickListener { _, _, position, _ ->
            val item = apps[position]
            launchApp(item.packageName)
        }
    }

    private fun loadRecentApps(): List<AppItem> {
        val items = mutableListOf<AppItem>()

        // 1) UsageStatsManager — 最近一周活跃应用
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val usageManager = getSystemService(USAGE_STATS_SERVICE) as? UsageStatsManager
            if (usageManager != null) {
                val now = System.currentTimeMillis()
                val oneWeek = 7 * 24 * 60 * 60 * 1000L
                runCatching {
                    val stats: List<UsageStats> = usageManager.queryUsageStats(
                        UsageStatsManager.INTERVAL_DAILY, now - oneWeek, now
                    ) ?: emptyList()
                    val seen = mutableSetOf<String>()
                    stats.sortedByDescending { it.lastTimeUsed }.forEach { s ->
                        if (s.packageName !in seen && s.lastTimeUsed > 0) {
                            seen.add(s.packageName)
                            val label = appLabelOrPkg(s.packageName)
                            items.add(AppItem(label, s.packageName, s.lastTimeUsed))
                        }
                    }
                }
            }
        }

        // 2) PackageManager — 用户可见的应用（兜底）
        if (items.isEmpty()) {
            val pm = packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
            runCatching {
                pm.queryIntentActivities(intent, 0).take(50).forEach { ri ->
                    val pkg = ri.activityInfo.packageName
                    val label = ri.loadLabel(pm).toString()
                    items.add(AppItem(label, pkg, 0L))
                }
            }
        }
        return items
    }

    private fun appLabelOrPkg(pkg: String): String = runCatching {
        val info = packageManager.getApplicationInfo(pkg, 0)
        packageManager.getApplicationLabel(info).toString()
    }.getOrDefault(pkg)

    private fun launchApp(packageName: String) {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent == null) {
            Toast.makeText(this, "无法启动 $packageName", Toast.LENGTH_SHORT).show()
            return
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { startActivity(intent) }.onFailure {
            Toast.makeText(this, "启动失败: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // ===== 工具 =====

    private fun makeTab(text: String, active: Boolean, onClick: () -> Unit): TextView {
        val ctx = this
        return TextView(ctx).apply {
            this.text = text
            textSize = 13f
            setPadding(dp(12), dp(6), dp(12), dp(6))
            setTextColor(if (active) 0xFFFFFFFF.toInt() else 0xFFAAAAAA.toInt())
            setBackgroundColor(if (active) 0xFF5B5BEF.toInt() else 0x00000000)
            background = roundedDrawable(dp(8).toFloat(),
                if (active) 0xFF5B5BEF.toInt() else 0x00000000, 0)
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, dp(6), 0) }
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun touchSlop(): Int = dp(6)

    private fun roundedDrawable(radiusDp: Float, color: Int, stroke: Int): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radiusDp * resources.displayMetrics.density
            setColor(color)
            if (stroke != 0) {
                setStroke(dp(1), stroke)
            }
        }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "悬浮窗", NotificationManager.IMPORTANCE_LOW)
            ch.description = "FloatAI 悬浮窗服务"
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    private fun buildNotification(): android.app.Notification {
        val pi = PendingIntent.getActivity(
            this, 0,
            Intent(this, com.floatai.MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FloatAI v${com.floatai.BuildConfig.VERSION_NAME}")
            .setContentText("悬浮窗服务运行中")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    data class AppItem(val label: String, val packageName: String, val lastUsed: Long)

    private class ChatAdapter(
        private val ctx: Context,
        private var items: List<ChatMessage>
    ) : BaseAdapter() {
        fun update(newItems: List<ChatMessage>) {
            items = newItems
            notifyDataSetChanged()
        }
        override fun getCount(): Int = items.size
        override fun getItem(position: Int): ChatMessage = items[position]
        override fun getItemId(position: Int): Long = position.toLong()
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val msg = items[position]
            val tv = (convertView as? TextView) ?: TextView(ctx).apply {
                setPadding(dp(10), dp(8), dp(10), dp(8))
                textSize = 13f
            }
            val isUser = msg.role == "user"
            tv.text = (if (isUser) "你: " else "AI: ") + msg.content
            tv.setTextColor(if (isUser) 0xFFB5B5FF.toInt() else 0xFFFFFFFF.toInt())
            tv.setBackgroundColor(if (isUser) 0xFF2A2A45.toInt() else 0xFF1A1A28.toInt())
            tv.background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 12f * ctx.resources.displayMetrics.density
                setColor(if (isUser) 0xFF2A2A45.toInt() else 0xFF1A1A28.toInt())
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(0, dp(4), 0, dp(4))
            tv.layoutParams = lp
            return tv
        }
        private fun dp(v: Int): Int = (v * ctx.resources.displayMetrics.density).toInt()
    }

    private class AppsAdapter(
        private val ctx: Context,
        private val items: List<AppItem>
    ) : BaseAdapter() {
        override fun getCount(): Int = items.size
        override fun getItem(position: Int): AppItem = items[position]
        override fun getItemId(position: Int): Long = position.toLong()
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val item = items[position]
            val tv = (convertView as? TextView) ?: TextView(ctx).apply {
                setPadding(dp(10), dp(10), dp(10), dp(10))
                textSize = 13f
            }
            tv.text = item.label
            tv.setTextColor(0xFFFFFFFF.toInt())
            tv.setBackgroundColor(0xFF1A1A28.toInt())
            tv.background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 10f * ctx.resources.displayMetrics.density
                setColor(0xFF1A1A28.toInt())
            }
            tv.setPadding(dp(10), dp(10), dp(10), dp(10))
            tv.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, dp(3), 0, dp(3)) }
            return tv
        }
        private fun dp(v: Int): Int = (v * ctx.resources.displayMetrics.density).toInt()
    }

    companion object {
        private const val CHANNEL_ID = "float_service"
        private const val NOTIF_ID = 1
    }
}
