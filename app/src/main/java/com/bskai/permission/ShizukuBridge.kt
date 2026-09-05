package com.bskai.permission

import android.content.Context
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku

class ShizukuBridge(private val context: Context) {

    enum class State { UNAVAILABLE, NEED_PERMISSION, GRANTED }

    private val _state = MutableStateFlow(detect())
    val state: StateFlow<State> = _state.asStateFlow()

    private val requestPermissionListener =
        Shizuku.OnRequestPermissionResultListener { _, grantResult ->
            _state.value = if (grantResult == PackageManager.PERMISSION_GRANTED)
                State.GRANTED else State.NEED_PERMISSION
        }

    private val binderDeathListener = Shizuku.OnBinderDeadListener {
        _state.value = detect()
    }

    init {
        try {
            Shizuku.addRequestPermissionResultListener(requestPermissionListener)
            Shizuku.addBinderDeadListener(binderDeathListener)
        } catch (_: Throwable) {}
    }

    fun refresh() { _state.value = detect() }

    fun isGranted(): Boolean = _state.value == State.GRANTED

    fun requestPermission(requestCode: Int = 1001) {
        try {
            if (!Shizuku.pingBinder()) {
                _state.value = State.UNAVAILABLE
                return
            }
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                _state.value = State.GRANTED
                return
            }
            _state.value = State.NEED_PERMISSION
            Shizuku.requestPermission(requestCode)
        } catch (e: Exception) {
            Log.w(TAG, "requestPermission failed", e)
            _state.value = State.UNAVAILABLE
        }
    }

    fun onPermissionResult() {
        _state.value = detect()
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
        try { Shizuku.removeBinderDeadListener(binderDeathListener) } catch (_: Throwable) {}
    }

    companion object { private const val TAG = "ShizukuBridge" }
}
