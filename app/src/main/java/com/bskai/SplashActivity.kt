package com.bskai

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.bskai.core.admin.AnnouncementStore

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // 一进入自动弹出公告（广告已改为公告）
        val announcements = AnnouncementStore.load(this)
        if (announcements.isNotEmpty()) {
            val pinned = announcements.firstOrNull { it.pinned }
            Toast.makeText(this, "公告: ${pinned?.title ?: announcements.first().title}", Toast.LENGTH_LONG).show()
        }
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
