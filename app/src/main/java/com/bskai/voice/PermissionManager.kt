package com.bskai.voice

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

data class PermissionRequest(
    val permission: String,
    val rationale: String,
    val group: String = ""
)

class PermissionManager(private val activity: Activity) {

    private val permissionLaunchers = mutableMapOf<String, ActivityResultLauncher<String>>()

    val requiredPermissions = listOf(
        PermissionRequest(
            Manifest.permission.RECORD_AUDIO,
            "需要录音权限来识别您的语音指令"
        ),
        PermissionRequest(
            Manifest.permission.FOREGROUND_SERVICE,
            "需要后台服务权限以保持语音监听持续运行"
        ),
        PermissionRequest(
            Manifest.permission.POST_NOTIFICATIONS,
            "需要通知权限来显示监听状态"
        )
    ) + when {
        Build.VERSION.SDK_INT <= Build.VERSION_CODES.P -> listOf(
            PermissionRequest(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                "需要存储权限来管理手机文件"
            ),
            PermissionRequest(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                "需要存储权限来管理手机文件"
            )
        )
        else -> emptyList()
    }

    fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun hasAllPermissions(): Boolean {
        return requiredPermissions.all { hasPermission(it.permission) }
    }

    fun requestPermission(permission: String, rationale: String = "") {
        val launcher = getOrCreateLauncher(permission)
        launcher.launch(permission)
    }

    fun requestMultiplePermissions(perms: List<String>) {
        val launcher = getOrCreateLauncher("group")
        launcher.launch(perms.toTypedArray())
    }

    fun shouldShowRationale(permission: String): Boolean {
        return activity.shouldShowRequestPermissionRationale(permission)
    }

    private fun getOrCreateLauncher(permission: String): ActivityResultLauncher<String> {
        return permissionLaunchers.getOrPut(permission) {
            activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                if (granted) {
                    onPermissionGranted(permission)
                }
            }
        }
    }

    private fun onPermissionGranted(permission: String) {
        when (permission) {
            Manifest.permission.RECORD_AUDIO -> {}
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE -> {}
            Manifest.permission.POST_NOTIFICATIONS -> {}
        }
    }

    fun buildIntent(): android.content.Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            android.content.Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                data = android.net.Uri.parse("package:${activity.packageName}")
            }
        } else {
            android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.parse("package:${activity.packageName}")
            }
        }
    }
}
