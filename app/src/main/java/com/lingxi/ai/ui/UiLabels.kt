package com.lingxi.ai.ui

import android.content.Context
import androidx.annotation.StringRes
import com.lingxi.ai.R
import com.lingxi.ai.data.ThemeStyle
import com.lingxi.ai.terminal.DevTools

internal fun Context.str(@StringRes id: Int, vararg args: Any?): String =
    resources.getString(id, *args)


@StringRes
internal fun themeLabelRes(style: ThemeStyle): Int = when (style) {
    ThemeStyle.AURORA -> R.string.theme_label_aurora
    ThemeStyle.NEON -> R.string.theme_label_neon
    ThemeStyle.GLASS -> R.string.theme_label_glass
    ThemeStyle.LIQUID -> R.string.theme_label_liquid
}

@StringRes
internal fun themeDescRes(style: ThemeStyle): Int = when (style) {
    ThemeStyle.AURORA -> R.string.theme_desc_aurora
    ThemeStyle.NEON -> R.string.theme_desc_neon
    ThemeStyle.GLASS -> R.string.theme_desc_glass
    ThemeStyle.LIQUID -> R.string.theme_desc_liquid
}

@StringRes
internal fun devToolCategoryLabelRes(key: String): Int = when (key) {
    "全部" -> R.string.devtools_cat_all
    "基础" -> R.string.devtools_cat_base
    "Android" -> R.string.devtools_cat_android
    "语言" -> R.string.devtools_cat_language
    "编译" -> R.string.devtools_cat_build
    "网络" -> R.string.devtools_cat_network
    "编辑" -> R.string.devtools_cat_editor
    else -> R.string.devtools_cat_all
}

@StringRes
internal fun devToolNameRes(tool: DevTools.ToolInfo): Int = when (tool.command) {
    "git" -> R.string.devtool_name_git
    "python3" -> R.string.devtool_name_python3
    "node" -> R.string.devtool_name_node
    "curl" -> R.string.devtool_name_curl
    "wget" -> R.string.devtool_name_wget
    "vim" -> R.string.devtool_name_vim
    "nano" -> R.string.devtool_name_nano
    "aapt2" -> R.string.devtool_name_aapt2
    "d8" -> R.string.devtool_name_d8
    "apksigner" -> R.string.devtool_name_apksigner
    "zipalign" -> R.string.devtool_name_zipalign
    "adb" -> R.string.devtool_name_adb
    "clang" -> R.string.devtool_name_clang
    "cmake" -> R.string.devtool_name_cmake
    "make" -> R.string.devtool_name_make
    "java" -> R.string.devtool_name_java
    "termux" -> R.string.devtool_name_termux
    else -> R.string.devtool_name_fallback
}

@StringRes
internal fun devToolDescRes(tool: DevTools.ToolInfo): Int = when (tool.command) {
    "git" -> R.string.devtool_desc_git
    "python3" -> R.string.devtool_desc_python3
    "node" -> R.string.devtool_desc_node
    "curl" -> R.string.devtool_desc_curl
    "wget" -> R.string.devtool_desc_wget
    "vim" -> R.string.devtool_desc_vim
    "nano" -> R.string.devtool_desc_nano
    "aapt2" -> R.string.devtool_desc_aapt2
    "d8" -> R.string.devtool_desc_d8
    "apksigner" -> R.string.devtool_desc_apksigner
    "zipalign" -> R.string.devtool_desc_zipalign
    "adb" -> R.string.devtool_desc_adb
    "clang" -> R.string.devtool_desc_clang
    "cmake" -> R.string.devtool_desc_cmake
    "make" -> R.string.devtool_desc_make
    "java" -> R.string.devtool_desc_java
    "termux" -> R.string.devtool_desc_termux
    else -> R.string.devtool_desc_fallback
}
