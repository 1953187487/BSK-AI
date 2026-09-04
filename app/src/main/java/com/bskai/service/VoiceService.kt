package com.bskai.service
import com.bskai.AuraApp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Build
import androidx.core.app.NotificationCompat
import com.bskai.MainActivity
import com.bskai.R
import com.bskai.voice.VoiceEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class VoiceService : Service() {

    companion object {
        const val CHANNEL_ID = "aura_voice_service"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.bskai.action.START_LISTENING"
        const val ACTION_STOP = "com.bskai.action.STOP_LISTENING"
        const val ACTION_WAKE = "com.bskai.action.WAKE_WORD"

        val isRunning = MutableStateFlow(false)
    }

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private lateinit var voiceEngine: VoiceEngine

    override fun onCreate() {
        super.onCreate()
        voiceEngine = (applicationContext as AuraApp).voiceEngine
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startListening()
            }
            ACTION_STOP -> {
                stopListening()
            }
        }
        return START_STICKY
    }

    private fun startListening() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        isRunning.value = true
        voiceEngine.startBackgroundListening()
    }

    private fun stopListening() {
        voiceEngine.stopBackgroundListening()
        isRunning.value = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.service_running))
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "AURA 语音监听",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "AURA 后台语音监听服务"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        voiceEngine.stopBackgroundListening()
        serviceScope.cancel()
    }
}
