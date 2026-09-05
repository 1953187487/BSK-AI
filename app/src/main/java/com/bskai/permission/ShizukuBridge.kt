package com.bskai.permission

import android.content.Context
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku

/**
 * Shizuku 状态 / 授权封装。
 *
 * Shizuku 是由 rikka 维护的"通过 ADB 或 Root 提权后给普通应用 binder IPC"的开源项目，
 * 遵循 Apache-2.0：https://github.com/RikkaApps/Shizuku
 */
class ShizukuBridge(private val context: Context) {

    enum class State { UNAVAILABLE, NEED_PERMISSION, GRANTED }

    private val _state = MutableStateFlow(detect())
    val state: StateFlow<State> = _state.asStateFlow()

    private val requestPermissionListener =
        Shizuku.OnRequestPermissionResultListener { _, grantResult ->
            _state.value = if (grantResult == PackageManager.PERMISSION_GRANTED)
                State.GRANTED else State.NEED_PERMISSION
        }

    init {
        try {
            Shizuku.addRequestPermissionResultListener(requestPermissionListener)
        } catch (_: Throwable) {}
    }

    fun refresh() { _state.value = detect() }

    fun isGranted(): Boolean = _state.value == State.GRANTED

    fun requestPermission(requestCode: Int = 1001) {
        if (_state.value == State.NEED_PERMISSION) {
            try {
                Shizuku.requestPermission(requestCode)
            } catch (e: Exception) {
                Log.w(TAG, "requestPermission failed", e)
            }
        }
    }

    fun binder(): IBinder? = try { Shizuku.getBinder() } catch (_: Throwable) { null }

    fun version(): Int = try { Shizuku.getVersion() } catch (_: Throwable) { -1 }

    private fun detect(): State {
        return try {
            if (!Shizuku.pingBinder()) return State.UNAVAILABLE
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED)
                State.GRANTED
            else State.NEED_PERMISSION
        } catch (t: Throwable) {
            Log.w(TAG, "Shizuku detect failed", t)
            State.UNAVAILABLE
        }
    }

    fun shutdown() {
        try { Shizuku.removeRequestPermissionResultListener(requestPermissionListener) } catch (_: Throwable) {}
    }

    companion object { private const val TAG = "ShizukuBridge" }
}
