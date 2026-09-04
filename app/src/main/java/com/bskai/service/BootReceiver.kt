package com.bskai.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON" ||
            intent.action == "com.htc.intent.action.QUICKBOOT_POWERON") {
            val settings = (context.applicationContext as AuraApp).settingsStore.settings.value
            if (settings.autoStartService) {
                context.startService(Intent(context, VoiceService::class.java).apply {
                    action = VoiceService.ACTION_START
                })
            }
        }
    }
}
