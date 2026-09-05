package com.bskai.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionsCoordinator(private val activity: ComponentActivity) {

    private var onResult: ((Boolean) -> Unit)? = null

    private val launcher: ActivityResultLauncher<Array<String>> =
        activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val granted = result.values.all { it }
            if (!granted) {
                if (hasPermanentlyDenied(activity)) {
                    onPermanentlyDenied?.invoke()
                }
            }
            onResult?.invoke(granted)
            onResult = null
        }

    var onPermanentlyDenied: (() -> Unit)? = null

    fun ensure(perms: List<String>, onResult: (Boolean) -> Unit) {
        val missing = perms.filter {
            ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            onResult(true)
            return
        }
        this.onResult = onResult
        launcher.launch(missing.toTypedArray())
    }

    fun ensureMic(onResult: (Boolean) -> Unit) {
        ensure(requiredRuntime(), onResult)
    }

    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", activity.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        activity.startActivity(intent)
    }

    companion object {
        fun requiredRuntime(): List<String> {
            val list = mutableListOf(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                list.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            return list
        }

        fun hasPermanentlyDenied(activity: Activity): Boolean {
            return requiredRuntime().any { perm ->
                ContextCompat.checkSelfPermission(activity, perm) != PackageManager.PERMISSION_GRANTED &&
                    !ActivityCompat.shouldShowRequestPermissionRationale(activity, perm)
            }
        }
    }
}
