package com.floatai.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
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
 * 悬浮窗服务 v1.0.5 液态玻璃版：
 *  - 屏幕边缘一个圆形快捷按钮（FAB）
 *  - 拖拽移动
 *  - 点击 FAB 弹出面板
 *  - 面板内两个 Tab：
 *      1. AI 聊天：直接输入消息，调用当前配置的 OpenAI 兼容 API
 *      2. 抓包：启动/停止 VPN 抓包会话，查看会话列表
 *  - 顶部关闭按钮 + 状态徽章
 *  - 液态玻璃风格：半透明深色背景 + 高光描边 + 大圆角
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
            background = liquidGlassBackground()
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

    private var currentTab = 0 // 0 = AI 聊天, 1 = 抓包, 2 = 工具

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
            background = liquidGlassBackground()
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
        val tabCapture = makeTab("抓包", currentTab == 1) {
            currentTab = 1; renderTabs(); renderContent()
        }
        val tabTools = makeTab("工具", currentTab == 2) {
            currentTab = 2; renderTabs(); renderContent()
        }
        tabRow.addView(tabAi)
        tabRow.addView(tabCapture)
        tabRow.addView(tabTools)
        tabRow.addView(TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
        })
        tabRow.tag = listOf(tabAi, tabCapture, tabTools)
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
        for (i in tags.indices) {
            (tags[i] as? TextView)?.let {
                it.setBackgroundColor(if (currentTab == i) 0xFF5B5BEF.toInt() else 0x00000000)
                it.setTextColor(if (currentTab == i) 0xFFFFFFFF.toInt() else 0xFFAAAAAA.toInt())
            }
        }
    }

    private fun renderContent() {
        val panel = panelContainer ?: return
        val content = panel.findViewWithTag<LinearLayout>("content") ?: return
        content.removeAllViews()
        when (currentTab) {
            0 -> renderAiTab(content)
            1 -> renderCaptureTab(content)
            else -> renderToolsTab(content)
        }
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

    // ----- Capture Tab (v1.0.3 基础版) -----

    private fun renderCaptureTab(parent: LinearLayout) {
        val ctx = this
        val title = TextView(ctx).apply {
            text = "抓包会话 (v1.0.3 基础版)"
            textSize = 12f
            setTextColor(0xFFAAAAAA.toInt())
            setPadding(0, dp(4), 0, dp(8))
        }
        parent.addView(title)

        val status = TextView(ctx).apply {
            text = "状态：未启动\n" +
                "v1.0.3 已实现：VpnService 接口建立 + 通知。\n" +
                "v1.0.4 将加入：IP 包解析、TCP 重组、TLS 解密。"
            textSize = 11f
            setTextColor(0xFFCCCCCC.toInt())
            setPadding(dp(10), dp(8), dp(10), dp(8))
            background = roundedDrawable(dp(8).toFloat(), 0xFF1F1F30.toInt(), 0)
        }
        parent.addView(status)

        val startBtn = Button(ctx).apply {
            text = "启动 VpnService (需用户在系统弹窗中授权)"
            textSize = 11f
            setBackgroundColor(0xFF5B5BEF.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            setOnClickListener {
                try {
                    val intent = Intent(ctx, com.floatai.capture.CaptureService::class.java)
                    ctx.startForegroundService(intent)
                    Toast.makeText(ctx, "抓包服务已启动", Toast.LENGTH_SHORT).show()
                    status.text = "状态：VPN 接口已建立，等待流量..."
                } catch (e: Exception) {
                    Toast.makeText(ctx, "启动失败：${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
        val stopBtn = Button(ctx).apply {
            text = "停止抓包"
            textSize = 11f
            setBackgroundColor(0xFF884444.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            setOnClickListener {
                try {
                    ctx.stopService(Intent(ctx, com.floatai.capture.CaptureService::class.java))
                    Toast.makeText(ctx, "已停止", Toast.LENGTH_SHORT).show()
                    status.text = "状态：未启动"
                } catch (e: Exception) {
                    Toast.makeText(ctx, "停止失败：${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
        val btnRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, dp(8))
        }
        btnRow.addView(startBtn)
        SpacerView(ctx, dp(6)).also { btnRow.addView(it) }
        btnRow.addView(stopBtn)
        parent.addView(btnRow)

        // 历史
        val app = applicationContext as? App
        val repo = app?.let { com.floatai.capture.CaptureRepository(it) }
        val listView = ListView(ctx).apply {
            divider = null
            setBackgroundColor(0x00000000)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
        }
        val sessions = repo?.listSessions() ?: emptyList()
        listView.adapter = SessionAdapter(ctx, sessions.map { it.nameWithoutExtension })
        parent.addView(listView)
    }

    private class SessionAdapter(
        private val ctx: Context,
        private val items: List<String>
    ) : BaseAdapter() {
        override fun getCount(): Int = items.size
        override fun getItem(position: Int): String = items[position]
        override fun getItemId(position: Int): Long = position.toLong()
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val tv = (convertView as? TextView) ?: TextView(ctx).apply { setPadding(dp(10), dp(10), dp(10), dp(10)); textSize = 12f }
            tv.text = "会话 ${items[position]}"
            tv.setTextColor(0xFFFFFFFF.toInt())
            tv.setBackgroundColor(0xFF1A1A28.toInt())
            tv.background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 8f * ctx.resources.displayMetrics.density
                setColor(0xFF1A1A28.toInt())
            }
            tv.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, dp(2), 0, dp(2)) }
            return tv
        }
        private fun dp(v: Int): Int = (v * ctx.resources.displayMetrics.density).toInt()
    }

    private class SpacerView(ctx: Context, heightPx: Int) : View(ctx) {
        init { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, heightPx) }
    }

    // ----- Tools Tab -----

    private fun renderToolsTab(parent: LinearLayout) {
        val ctx = this

        // 工具 1：打开主应用
        addToolButton(parent, "打开 FloatAI 主界面", "📱") {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { startActivity(intent) }
                .onFailure { Toast.makeText(ctx, "启动失败: ${it.message}", Toast.LENGTH_SHORT).show() }
        }

        // 工具 2：启动字符选择（无障碍服务）
        addToolButton(parent, "启动字符选择器", "⌨") {
            val intent = Intent(ctx, com.floatai.MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { startActivity(intent) }
                .onFailure { Toast.makeText(ctx, "启动失败: ${it.message}", Toast.LENGTH_SHORT).show() }
        }

        // 工具 3：清空聊天历史
        addToolButton(parent, "清空当前 AI 聊天", "🗑") {
            Toast.makeText(ctx, "请在主界面操作", Toast.LENGTH_SHORT).show()
        }

        // 工具 4：检查更新
        addToolButton(parent, "检查应用更新", "🔄") {
            val intent = Intent(ctx, com.floatai.MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("route", "settings")
            }
            runCatching { startActivity(intent) }
        }

        // 工具 5：当前版本信息
        SpacerView(ctx, dp(8)).also { parent.addView(it) }
        val versionInfo = TextView(ctx).apply {
            text = buildString {
                append("版本：v${com.floatai.BuildConfig.VERSION_NAME}\n")
                append("协议：v${com.floatai.BuildConfig.PROTOCOL_VERSION}\n")
                append("Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            }
            textSize = 10f
            setTextColor(0xFFAAAAAA.toInt())
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = roundedDrawable(dp(8).toFloat(), 0xFF1F1F30.toInt(), 0)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, dp(8), 0, dp(4)) }
        }
        parent.addView(versionInfo)

        // 工具 6：快速收起面板
        addToolButton(parent, "收起悬浮窗", "▼") {
            closePanel()
        }
    }

    private fun addToolButton(parent: LinearLayout, label: String, icon: String, onClick: () -> Unit) {
        val ctx = this
        val btn = TextView(ctx).apply {
            text = "$icon  $label"
            textSize = 13f
            setTextColor(0xFFEEEEEE.toInt())
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = roundedDrawable(dp(10).toFloat(), 0xFF2A2A40.toInt(), 0xFF5B5BEF.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, dp(3), 0, dp(3)) }
            setOnClickListener { onClick() }
        }
        parent.addView(btn)
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

    /**
     * 液态玻璃背景：半透明深色 + 顶部高光描边 + 大圆角。
     * 使用 LayerDrawable 模拟 iOS 风格液态玻璃。
     */
    private fun liquidGlassBackground(): android.graphics.drawable.Drawable {
        val density = resources.displayMetrics.density
        val radius = 22f * density
        // 底层：半透明深色渐变
        val base = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            colors = intArrayOf(
                Color.argb(0xF0, 0x1A, 0x1A, 0x2A),  // 顶部稍亮
                Color.argb(0xF0, 0x10, 0x10, 0x1C)   // 底部更深
            )
            orientation = android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM
        }
        // 顶层：1px 高光描边
        val stroke = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setStroke((0.6f * density).toInt(), Color.argb(0x88, 0xFF, 0xFF, 0xFF))
            setColor(Color.TRANSPARENT)
        }
        return android.graphics.drawable.LayerDrawable(arrayOf(base, stroke))
    }

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

    companion object {
        private const val CHANNEL_ID = "float_service"
        private const val NOTIF_ID = 1
    }
}
