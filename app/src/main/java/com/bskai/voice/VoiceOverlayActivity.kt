package com.bskai.voice

import android.content.Context
import android.os.Build
import android.provider.Settings

class VoiceOverlayActivity : androidx.appcompat.app.AppCompatActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        // Full-screen overlay for lock screen voice interaction
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = android.content.Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        }
        finish()
    }
}
