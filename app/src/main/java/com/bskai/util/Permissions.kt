package com.bskai.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object Permissions {

    const val REQUEST_RECORD_AUDIO = 1001

    fun hasRecordAudio(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

    fun hasNotification(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun missingRuntimePermissions(context: Context): List<String> {
        val list = mutableListOf<String>()
        if (!hasRecordAudio(context)) list.add(Manifest.permission.RECORD_AUDIO)
        if (!hasNotification(context)) list.add(Manifest.permission.POST_NOTIFICATIONS)
        return list
    }
}
