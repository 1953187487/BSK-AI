package com.bskai.core.admin

import java.security.MessageDigest

/**
 * 管理员认证：凭据以 SHA-256 哈希形式内置，界面与代码均不出现明文。
 * 如需修改凭据，请重新生成账号密码并替换下方常量，再同步更新 sha256 哈希。
 */
object AdminAuth {

    private const val ADMIN_USER_HASH =
        "1fa776b6785f0ada2f6c445be63475e796c44340039bf0f850738ed4fae67dd5"
    private const val ADMIN_PASS_HASH =
        "95a3182c2a8f5b505735f527a3ff9644dc7b7d29ff8552f5d99ef7f1bd65ce33"

    private val _session = java.util.concurrent.atomic.AtomicBoolean(false)

    val isLoggedIn: Boolean get() = _session.get()

    fun login(account: String, password: String): Boolean {
        val ok = sha256(account.trim()) == ADMIN_USER_HASH &&
            sha256(password) == ADMIN_PASS_HASH
        if (ok) _session.set(true)
        return ok
    }

    fun logout() {
        _session.set(false)
    }

    private fun sha256(input: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
