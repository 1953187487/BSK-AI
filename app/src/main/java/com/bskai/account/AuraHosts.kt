package com.bskai.account

/**
 * AURA 账号体系对接沙盒服务器(策划书第十四章/第十五章)。
 *
 * 沙盒服务器默认 127.0.0.1:18080(监听本地,不暴露公网)。
 * 生产部署时可改 base url。
 */
object AuraHosts {
    const val DEFAULT_BASE = "http://127.0.0.1:18080/"
}
