package com.bskai.permission

import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku

/**
 * Bridges the Shizuku IPC service for privileged command execution.
 *
 * State is exposed as a [StateFlow] for UI observation. This class registers
 * Shizuku listeners once (the previous implementation registered the
 * binder-received listener twice in [init], which could cause the state to
 * be overwritten by a duplicate callback).
 */
class ShizukuBridge {

    /** Shizuku authorization state. */
    enum class State { UNAVAILABLE, NEED_PERMISSION, GRANTED }

    private val _state = MutableStateFlow(detect())
    /** Current Shizuku state, observable by the UI. */
    val state: StateFlow<State> = _state.asStateFlow()

    private val requestPermissionListener =
        Shizuku.OnRequestPermissionResultListener { _, grantResult ->
            val newState = if (grantResult == PackageManager.PERMISSION_GRANTED)
                State.GRANTED else State.NEED_PERMISSION
            _state.value = newState
        }

    private val binderDeathListener = Shizuku.OnBinderDeadListener {
        _state.value = State.UNAVAILABLE
    }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        _state.value = detect()
    }

    init {
        try {
            Shizuku.addRequestPermissionResultListener(requestPermissionListener)
            Shizuku.addBinderDeadListener(binderDeathListener)
            Shizuku.addBinderReceivedListener(binderReceivedListener)
        } catch (t: Throwable) {
            Log.w(TAG, "Shizuku init failed", t)
        }
    }

    /**
     * Re-detects the Shizuku state and updates [state].
     */
    fun refresh() {
        _state.value = detect()
    }

    /**
     * Whether Shizuku permission has been granted.
     */
    fun isGranted(): Boolean = _state.value == State.GRANTED

    /**
     * Requests Shizuku permission from the user.
     *
     * @param requestCode request code for the permission result
     */
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

    /**
     * Call after receiving the permission result to refresh state.
     */
    fun onPermissionResult() {
        _state.value = detect()
    }

    /**
     * Returns the Shizuku binder, or null when unavailable.
     */
    fun binder(): IBinder? = try { Shizuku.getBinder() } catch (_: Throwable) { null }

    /**
     * Returns the Shizuku service version, or -1 when unavailable.
     */
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

    /**
     * Unregisters all Shizuku listeners. Call when the bridge is no longer needed.
     */
    fun shutdown() {
        try { Shizuku.removeRequestPermissionResultListener(requestPermissionListener) } catch (_: Throwable) {}
        try { Shizuku.removeBinderDeadListener(binderDeathListener) } catch (_: Throwable) {}
        try { Shizuku.removeBinderReceivedListener(binderReceivedListener) } catch (_: Throwable) {}
    }

    companion object {
        private const val TAG = "ShizukuBridge"
    }
}
