package com.floatai.ui.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.floatai.App
import com.floatai.R
import com.floatai.data.model.AppLanguage

/**
 * 字符串访问入口：根据当前 SettingsRepository 的语言返回对应文案。
 *  - ZH：读取 values/strings.xml
 *  - EN：读取 values-en/strings.xml
 *
 * 使用方式：`LocalStrings.current.chat_title`
 */
@Composable
fun localStrings(): LocalStrings {
    val context = LocalContext.current
    val app = context.applicationContext as App
    val lang by app.settingsRepository.settings.collectAsStateWithLifecycle()
    val res = context.resources
    val targetLocale = if (lang.language == AppLanguage.EN) java.util.Locale.ENGLISH else null
    val localized = remember(lang.language) {
        val cfg = res.configuration
        val newCfg = if (targetLocale != null) {
            android.content.res.Configuration(cfg).apply { setLocale(targetLocale) }
        } else cfg
        context.createConfigurationContext(newCfg).resources
    }
    return LocalStrings(localized)
}

/**
 * 将 string id 解析成字符串，传入 ARGS 参数。
 * 失败回退到 LocalContext 取默认字符串。
 */
class LocalStrings(private val res: android.content.res.Resources) {
    val app_name: String get() = res.getString(R.string.app_name)

    val user_notice_title: String get() = res.getString(R.string.user_notice_title)
    val user_notice_body: String get() = res.getString(R.string.user_notice_body)
    val permission_notice_title: String get() = res.getString(R.string.permission_notice_title)
    val permission_notice_body: String get() = res.getString(R.string.permission_notice_body)

    val oss_title: String get() = res.getString(R.string.oss_title)

    val language_choose_title: String get() = res.getString(R.string.language_choose_title)
    val language_choose_body: String get() = res.getString(R.string.language_choose_body)
    val language_zh: String get() = res.getString(R.string.language_zh)
    val language_en: String get() = res.getString(R.string.language_en)

    val nav_ai_chat: String get() = res.getString(R.string.nav_ai_chat)
    val nav_settings: String get() = res.getString(R.string.nav_settings)
    val nav_build: String get() = res.getString(R.string.nav_build)
    val nav_package_hub: String get() = res.getString(R.string.nav_package_hub)
    val nav_about: String get() = res.getString(R.string.nav_about)

    val chat_title: String get() = res.getString(R.string.chat_title)
    val chat_placeholder: String get() = res.getString(R.string.chat_placeholder)
    val chat_empty: String get() = res.getString(R.string.chat_empty)
    val chat_send: String get() = res.getString(R.string.chat_send)
    val chat_history: String get() = res.getString(R.string.chat_history)
    val chat_clear: String get() = res.getString(R.string.chat_clear)
    val chat_character: String get() = res.getString(R.string.chat_character)
    val chat_manage_models: String get() = res.getString(R.string.chat_manage_models)
    val chat_select_model: String get() = res.getString(R.string.chat_select_model)
    val chat_no_models: String get() = res.getString(R.string.chat_no_models)

    val api_title: String get() = res.getString(R.string.api_title)
    val api_subtitle: String get() = res.getString(R.string.api_subtitle)
    val api_base_url_label: String get() = res.getString(R.string.api_base_url_label)
    val api_key_label: String get() = res.getString(R.string.api_key_label)
    val api_fetch: String get() = res.getString(R.string.api_fetch)
    val api_save: String get() = res.getString(R.string.api_save)
    val api_custom_hint: String get() = res.getString(R.string.api_custom_hint)
    fun api_available(count: Int) = res.getString(R.string.api_available, count)
    val api_selected: String get() = res.getString(R.string.api_selected)

    val settings_general: String get() = res.getString(R.string.settings_general)
    val settings_theme_title: String get() = res.getString(R.string.settings_theme_title)
    val settings_dynamic_color: String get() = res.getString(R.string.settings_dynamic_color)
    val settings_theme_color: String get() = res.getString(R.string.settings_theme_color)
    val settings_language: String get() = res.getString(R.string.settings_language)
    val settings_float_section: String get() = res.getString(R.string.settings_float_section)
    val settings_float_title: String get() = res.getString(R.string.settings_float_title)
    val settings_float_desc: String get() = res.getString(R.string.settings_float_desc)
    val settings_permission_section: String get() = res.getString(R.string.settings_permission_section)
    val settings_permission_notice: String get() = res.getString(R.string.settings_permission_notice)
    val settings_github_section: String get() = res.getString(R.string.settings_github_section)
    val settings_github_token: String get() = res.getString(R.string.settings_github_token)
    val settings_github_token_desc: String get() = res.getString(R.string.settings_github_token_desc)
    val settings_about_section: String get() = res.getString(R.string.settings_about_section)
    val settings_about_app: String get() = res.getString(R.string.settings_about_app)
    val settings_about_desc: String get() = res.getString(R.string.settings_about_desc)
    val settings_about_license: String get() = res.getString(R.string.settings_about_license)
    val settings_contact_email: String get() = res.getString(R.string.settings_contact_email)
    val settings_open_repo: String get() = res.getString(R.string.settings_open_repo)

    val about_app: String get() = res.getString(R.string.about_app)
    val about_version: String get() = res.getString(R.string.about_version)
    val about_protocol: String get() = res.getString(R.string.about_protocol)
    val about_build_type: String get() = res.getString(R.string.about_build_type)
    val about_repo: String get() = res.getString(R.string.about_repo)
    val about_open_source: String get() = res.getString(R.string.about_open_source)
    val about_license: String get() = res.getString(R.string.about_license)
    val about_github: String get() = res.getString(R.string.about_github)
    val about_updates: String get() = res.getString(R.string.about_updates)
    val about_check_now: String get() = res.getString(R.string.about_check_now)
    val about_new_version_available: String get() = res.getString(R.string.about_new_version_available)
    val about_no_network: String get() = res.getString(R.string.about_no_network)
    val about_history: String get() = res.getString(R.string.about_history)

    // v1.0.4
    val settings_permission_request: String get() = res.getString(R.string.settings_permission_request)
    val settings_advanced_section: String get() = res.getString(R.string.settings_advanced_section)
    val settings_ai_section: String get() = res.getString(R.string.settings_ai_section)
    val settings_api_base: String get() = res.getString(R.string.settings_api_base)
    val settings_api_model: String get() = res.getString(R.string.settings_api_model)
    val perm_overlay: String get() = res.getString(R.string.perm_overlay)
    val perm_overlay_desc: String get() = res.getString(R.string.perm_overlay_desc)
    val perm_notifications: String get() = res.getString(R.string.perm_notifications)
    val perm_notifications_desc: String get() = res.getString(R.string.perm_notifications_desc)
    val perm_microphone: String get() = res.getString(R.string.perm_microphone)
    val perm_microphone_desc: String get() = res.getString(R.string.perm_microphone_desc)
    val perm_camera: String get() = res.getString(R.string.perm_camera)
    val perm_camera_desc: String get() = res.getString(R.string.perm_camera_desc)
    val perm_usage: String get() = res.getString(R.string.perm_usage)
    val perm_usage_desc: String get() = res.getString(R.string.perm_usage_desc)
    val perm_vpn: String get() = res.getString(R.string.perm_vpn)
    val perm_vpn_desc: String get() = res.getString(R.string.perm_vpn_desc)
    val perm_lobster: String get() = res.getString(R.string.perm_lobster)
    val perm_lobster_desc: String get() = res.getString(R.string.perm_lobster_desc)
    val perm_granted: String get() = res.getString(R.string.perm_granted)
    val perm_not_granted: String get() = res.getString(R.string.perm_not_granted)
    val perm_no_settings: String get() = res.getString(R.string.perm_no_settings)

    val lobster_section: String get() = res.getString(R.string.lobster_section)
    val lobster_panel_title: String get() = res.getString(R.string.lobster_panel_title)
    val lobster_panel_desc: String get() = res.getString(R.string.lobster_panel_desc)
    val lobster_status_on: String get() = res.getString(R.string.lobster_status_on)
    val lobster_status_off: String get() = res.getString(R.string.lobster_status_off)

    val update_check: String get() = res.getString(R.string.update_check)
    val update_download: String get() = res.getString(R.string.update_download)
    val update_downloading: String get() = res.getString(R.string.update_downloading)
    val update_ready: String get() = res.getString(R.string.update_ready)
    val update_install: String get() = res.getString(R.string.update_install)
    val update_failed: String get() = res.getString(R.string.update_failed)
    val update_no_apk: String get() = res.getString(R.string.update_no_apk)
    val update_latest: String get() = res.getString(R.string.update_latest)
    val update_history: String get() = res.getString(R.string.update_history)

    val chat_voice_input: String get() = res.getString(R.string.chat_voice_input)
    val chat_voice_recording: String get() = res.getString(R.string.chat_voice_recording)
    val chat_voice_empty: String get() = res.getString(R.string.chat_voice_empty)

    val plugin_section: String get() = res.getString(R.string.plugin_section)
    val skill_section: String get() = res.getString(R.string.skill_section)
    val lobster_extension_section: String get() = res.getString(R.string.lobster_extension_section)
    val plugin_empty: String get() = res.getString(R.string.plugin_empty)
    val skill_empty: String get() = res.getString(R.string.skill_empty)
    val lobster_create: String get() = res.getString(R.string.lobster_create)
}
