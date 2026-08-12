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

    val elevated_title: String get() = res.getString(R.string.elevated_title)
    val elevated_intro: String get() = res.getString(R.string.elevated_intro)
    val elevated_shizuku: String get() = res.getString(R.string.elevated_shizuku)
    val elevated_shizuku_desc: String get() = res.getString(R.string.elevated_shizuku_desc)
    val elevated_dhizuku: String get() = res.getString(R.string.elevated_dhizuku)
    val elevated_dhizuku_desc: String get() = res.getString(R.string.elevated_dhizuku_desc)
    val elevated_root: String get() = res.getString(R.string.elevated_root)
    val elevated_root_desc: String get() = res.getString(R.string.elevated_root_desc)
    val elevated_open: String get() = res.getString(R.string.elevated_open)
    val elevated_recheck: String get() = res.getString(R.string.elevated_recheck)
    val elevated_continue: String get() = res.getString(R.string.elevated_continue)
    val elevated_required: String get() = res.getString(R.string.elevated_required)
    fun elevated_status(name: String) = res.getString(R.string.elevated_status, name)

    val shizuku_intro_title: String get() = res.getString(R.string.shizuku_intro_title)
    val shizuku_intro_body: String get() = res.getString(R.string.shizuku_intro_body)
    val dhizuku_intro_title: String get() = res.getString(R.string.dhizuku_intro_title)
    val dhizuku_intro_body: String get() = res.getString(R.string.dhizuku_intro_body)

    val language_choose_title: String get() = res.getString(R.string.language_choose_title)
    val language_choose_body: String get() = res.getString(R.string.language_choose_body)
    val language_zh: String get() = res.getString(R.string.language_zh)
    val language_en: String get() = res.getString(R.string.language_en)

    val nav_ai_chat: String get() = res.getString(R.string.nav_ai_chat)
    val nav_atk: String get() = res.getString(R.string.nav_atk)
    val nav_settings: String get() = res.getString(R.string.nav_settings)
    val nav_build: String get() = res.getString(R.string.nav_build)
    val nav_packages: String get() = res.getString(R.string.nav_packages)
    val nav_mcp: String get() = res.getString(R.string.nav_mcp)

    val chat_title: String get() = res.getString(R.string.chat_title)
    val chat_placeholder: String get() = res.getString(R.string.chat_placeholder)
    val chat_empty: String get() = res.getString(R.string.chat_empty)
    val chat_send: String get() = res.getString(R.string.chat_send)
    val chat_history: String get() = res.getString(R.string.chat_history)
    val chat_clear: String get() = res.getString(R.string.chat_clear)
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

    val atk_title: String get() = res.getString(R.string.atk_title)
    val atk_subtitle: String get() = res.getString(R.string.atk_subtitle)
    val atk_terminal: String get() = res.getString(R.string.atk_terminal)
    val atk_terminal_hint: String get() = res.getString(R.string.atk_terminal_hint)
    val atk_run: String get() = res.getString(R.string.atk_run)
    val atk_stop: String get() = res.getString(R.string.atk_stop)
    val atk_clear_log: String get() = res.getString(R.string.atk_clear_log)
    val atk_project: String get() = res.getString(R.string.atk_project)
    val atk_new_project: String get() = res.getString(R.string.atk_new_project)
    val atk_project_name: String get() = res.getString(R.string.atk_project_name)
    val atk_project_pkg: String get() = res.getString(R.string.atk_project_pkg)
    val atk_project_create: String get() = res.getString(R.string.atk_project_create)
    val atk_build_debug: String get() = res.getString(R.string.atk_build_debug)
    val atk_build_release: String get() = res.getString(R.string.atk_build_release)
    val atk_publish: String get() = res.getString(R.string.atk_publish)
    val atk_publish_repo: String get() = res.getString(R.string.atk_publish_repo)
    val atk_publish_desc: String get() = res.getString(R.string.atk_publish_desc)
    val atk_publish_private: String get() = res.getString(R.string.atk_publish_private)
    val atk_ai_diagnose: String get() = res.getString(R.string.atk_ai_diagnose)
    val atk_ai_fix: String get() = res.getString(R.string.atk_ai_fix)
    val atk_need_token: String get() = res.getString(R.string.atk_need_token)
    val atk_no_project: String get() = res.getString(R.string.atk_no_project)
    fun atk_scaffold_done(p: String) = res.getString(R.string.atk_scaffold_done, p)
    fun atk_build_done(p: String) = res.getString(R.string.atk_build_done, p)
    fun atk_publish_done(p: String) = res.getString(R.string.atk_publish_done, p)

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
    val settings_check_update: String get() = res.getString(R.string.settings_check_update)
}
