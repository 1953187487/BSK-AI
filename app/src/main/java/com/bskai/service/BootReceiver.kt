package com.bskai.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.bskai.AuraApp

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            val app = AuraApp.of(context)
            if (app.settings.settings.value.autoStartService) {
                VoiceService.start(context)
            }
        }
    }
}
