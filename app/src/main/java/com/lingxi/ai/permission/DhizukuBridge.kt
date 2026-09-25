package com.lingxi.ai.permission

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.rosan.dhizuku.api.Dhizuku
import com.rosan.dhizuku.api.DhizukuRequestPermissionListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Bridges the Dhizuku service (https://github.com/iamr0s/Dhizuku) for
 * DeviceOwner-privileged command execution.
 *
 * Dhizuku shares DeviceOwner privileges over IPC, giving the terminal a
 * third authorization path besides Shizuku (ADB) and root. Unlike Shizuku,
 * the Dhizuku API exposes no binder liveness listeners, so state detection
 * is pull-based via [refresh].
 */
class DhizukuBridge {

    /** Dhizuku authorization state. */
    enum class State { UNAVAILABLE, NEED_PERMISSION, GRANTED }

    private val _state = MutableStateFlow(State.UNAVAILABLE)

    /** Current Dhizuku state, observable by the UI. */
    val state: StateFlow<State> = _state.asStateFlow()

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var requestInFlight = false

    /**
     * Stores the application context and performs the first detection.
     * Safe to call multiple times.
     */
    fun init(context: Context) {
        appContext = context.applicationContext
        refresh()
    }

    /**
     * Re-detects the Dhizuku state. Internally calls [Dhizuku.init] which
     * performs a synchronous binder fetch, so this may block briefly.
     */
    fun refresh() {
        _state.value = detect()
    }

    /**
     * Whether Dhizuku permission has been granted.
     */
    fun isGranted(): Boolean = _state.value == State.GRANTED

    /**
     * Requests the Dhizuku privileged permission from the user. Dhizuku
     * launches its own confirmation activity and reports back asynchronously.
     */
    fun requestPermission() {
        val context = appContext ?: return
        if (requestInFlight) return
        try {
            if (!Dhizuku.init(context)) {
                _state.value = State.UNAVAILABLE
                return
            }
            if (Dhizuku.isPermissionGranted()) {
                _state.value = State.GRANTED
                return
            }
            requestInFlight = true
            _state.value = State.NEED_PERMISSION
            Dhizuku.requestPermission(object : DhizukuRequestPermissionListener() {
                override fun onRequestPermission(grantResult: Int) {
                    requestInFlight = false
                    _state.value = if (grantResult == PackageManager.PERMISSION_GRANTED)
                        State.GRANTED else State.NEED_PERMISSION
                }
            })
        } catch (e: Exception) {
            requestInFlight = false
            Log.w(TAG, "requestPermission failed", e)
            _state.value = State.UNAVAILABLE
        }
    }

    /**
     * Starts a privileged remote process via the Dhizuku server.
     *
     * @param cmd command vector, e.g. ["/system/bin/sh", "-c", command]
     * @param dir optional working directory
     */
    fun newProcess(cmd: Array<String>, dir: String?): Process =
        Dhizuku.newProcess(cmd, null, dir?.let { File(it) })

    /**
     * Whether the Dhizuku server is installed and active as device owner.
     */
    fun isAvailable(): Boolean = _state.value != State.UNAVAILABLE

    private fun detect(): State {
        val context = appContext ?: return State.UNAVAILABLE
        return try {
            if (!Dhizuku.init(context)) return State.UNAVAILABLE
            if (Dhizuku.isPermissionGranted()) State.GRANTED else State.NEED_PERMISSION
        } catch (t: Throwable) {
            Log.w(TAG, "detect failed", t)
            State.UNAVAILABLE
        }
    }

    /** Nothing to release; kept for API parity with [ShizukuBridge]. */
    fun shutdown() { /* Dhizuku API holds no disposable listeners */ }

    companion object {
        private const val TAG = "DhizukuBridge"
    }
}
