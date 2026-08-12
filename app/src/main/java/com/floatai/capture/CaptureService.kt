package com.floatai.capture

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.floatai.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * VpnService 抓包服务 v1.0.5：
 *  - 真实解析 IP/TCP/HTTP（明文）
 *  - 每条 HTTP 请求/响应写入 CaptureRepository
 *  - 通过 SharedFlow<HttpMessage> 暴露给 UI 实时刷新
 *
 * 仅处理 IPv4 + TCP（HTTPS 因 TLS 加密看不到内容）。
 */
class CaptureService : VpnService() {

    private var pfd: ParcelFileDescriptor? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var readerJob: Job? = null

    // 共享事件流：UI 可通过 service binder / flow collector 接收
    private val _events = MutableSharedFlow<HttpEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<HttpEvent> = _events.asSharedFlow()

    // 当前会话 id
    private var sessionId: String = newSessionId()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification())
        establish()
        return START_STICKY
    }

    override fun onRevoke() {
        Log.i(TAG, "VPN revoked by system")
        cleanup()
        super.onRevoke()
    }

    override fun onDestroy() {
        cleanup()
        super.onDestroy()
    }

    private fun cleanup() {
        readerJob?.cancel()
        readerJob = null
        pfd?.close()
        pfd = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
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
        val fd = builder.establish() ?: return
        pfd = fd
        sessionId = newSessionId()
        startReader(fd)
    }

    private fun startReader(fd: ParcelFileDescriptor) {
        readerJob = scope.launch {
            val input = java.io.FileInputStream(fd.fileDescriptor)
            val buf = ByteArray(32767)
            val streams = HashMap<String, IpTcpParser.TcpStream>()
            try {
                while (isActive) {
                    val n = try { input.read(buf) } catch (_: Exception) { -1 }
                    if (n < 0) break
                    if (n == 0) continue
                    // 拷贝一份用于解析（buf 是循环缓冲）
                    val packet = buf.copyOfRange(0, n)
                    val ip = IpTcpParser.parseIp(packet) ?: continue
                    val tcp = IpTcpParser.parseTcp(ip) ?: continue
                    if (tcp.payload.isEmpty()) continue

                    val key = "${ip.srcIp}:${tcp.srcPort}-${ip.dstIp}:${tcp.dstPort}"
                    val stream = streams.getOrPut(key) { IpTcpParser.TcpStream() }
                    val acc = stream.feed(tcp) ?: continue

                    // 解析 HTTP
                    val parsed = IpTcpParser.extractHttpMessage(acc) ?: continue
                    val (msg, bodyStart) = parsed
                    // 只保留 header 部分（body 可能很大，截断 4KB）
                    val bodyPreview = msg.body.take(4096)
                    val evt = HttpEvent(
                        timestamp = System.currentTimeMillis(),
                        sessionId = sessionId,
                        clientIp = ip.srcIp,
                        clientPort = tcp.srcPort,
                        serverIp = ip.dstIp,
                        serverPort = tcp.dstPort,
                        method = msg.method,
                        url = msg.url,
                        statusCode = msg.statusCode,
                        statusText = msg.statusText,
                        contentType = msg.headers["Content-Type"] ?: msg.headers["content-type"],
                        bodyPreview = bodyPreview
                    )
                    _events.tryEmit(evt)
                    // 持久化
                    val repo = CaptureRepository(applicationContext)
                    repo.appendRecord(
                        CaptureRecord(
                            id = "${sessionId}-${System.nanoTime()}",
                            sessionId = sessionId,
                            timestamp = evt.timestamp,
                            sourceApp = ip.dstIp,
                            method = evt.method ?: "-",
                            url = "${evt.method ?: "RESP"} ${evt.url ?: "${evt.statusCode}"}",
                            status = evt.statusCode ?: 0,
                            requestBody = if (evt.method != null) bodyPreview else "",
                            responseBody = if (evt.method == null) bodyPreview else "",
                            note = "${ip.srcIp}:${tcp.srcPort} → ${ip.dstIp}:${tcp.dstPort}"
                        )
                    )
                    // 消费已解析的数据，避免重复解析
                    stream.buffered = acc.copyOfRange(bodyStart, acc.size)
                }
            } catch (e: Exception) {
                Log.w(TAG, "reader error", e)
            }
        }
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
            .setContentText("正在捕获本机 HTTP 流量（HTTPS 内容因 TLS 加密不可见）")
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

    /** 实时捕获到的事件（HTTP 请求/响应）。 */
    data class HttpEvent(
        val timestamp: Long,
        val sessionId: String,
        val clientIp: String,
        val clientPort: Int,
        val serverIp: String,
        val serverPort: Int,
        val method: String?,
        val url: String?,
        val statusCode: Int?,
        val statusText: String?,
        val contentType: String?,
        val bodyPreview: String
    )

    companion object {
        private const val TAG = "CaptureService"
        private const val CHANNEL_ID = "capture_service"
        private const val NOTIF_ID = 2

        fun prepareVpn(activity: android.app.Activity): Intent? =
            VpnService.prepare(activity)

        private fun newSessionId(): String = java.util.UUID.randomUUID().toString().take(8)
    }
}
