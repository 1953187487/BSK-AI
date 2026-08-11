package com.floatai.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.floatai.App
import com.floatai.R
import com.floatai.data.model.AppLanguage
import com.floatai.service.FloatService
import com.floatai.ui.components.GlassCard
import com.floatai.ui.components.SectionTitle
import com.floatai.ui.components.SettingsRow
import com.floatai.ui.i18n.localStrings
import com.floatai.ui.theme.AccentOptions

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.applicationContext as App
    val vm: SettingsViewModel = viewModel(
        key = "settings",
        factory = SettingsViewModel.factory(app.settingsRepository)
    )
    val settings by vm.settings.collectAsStateWithLifecycle()
    val updateState by vm.update.collectAsStateWithLifecycle()
    val strings = localStrings()

    var showPermissionNotice by remember { mutableStateOf(false) }
    var showFloatGrants by remember { mutableStateOf(false) }
    var showTokenDialog by remember { mutableStateOf(false) }
    var localMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(strings.app_name + " · " + strings.nav_settings, style = MaterialTheme.typography.headlineMedium)

        SectionTitle(strings.settings_general)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                SettingsRow(
                    title = strings.settings_theme_title,
                    subtitle = if (settings.darkTheme) "Dark" else "Light"
                ) {
                    Switch(checked = settings.darkTheme, onCheckedChange = vm::setDarkTheme)
                }
                SettingsRow(
                    title = strings.settings_dynamic_color,
                    subtitle = "Android 12+ wallpaper-based"
                ) {
                    Switch(checked = settings.dynamicColor, onCheckedChange = vm::setDynamicColor)
                }
                SettingsRow(
                    title = strings.settings_theme_color,
                    subtitle = settings.accentColor
                ) {
                    ThemeColorPicker(selected = settings.accentColor, onSelect = vm::setAccentColor)
                }
                SettingsRow(
                    title = strings.settings_language,
                    subtitle = if (settings.language == AppLanguage.ZH) strings.language_zh else strings.language_en
                ) {
                    Row {
                        TextButton(onClick = { vm.setLanguage(AppLanguage.ZH) }) { Text("ZH") }
                        TextButton(onClick = { vm.setLanguage(AppLanguage.EN) }) { Text("EN") }
                    }
                }
            }
        }

        SectionTitle(strings.settings_float_section)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                SettingsRow(
                    title = strings.settings_float_title,
                    subtitle = strings.settings_float_desc
                ) {
                    Switch(
                        checked = settings.floatEnabled,
                        onCheckedChange = { enabled ->
                            vm.setFloatEnabled(enabled)
                            if (enabled) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                                    !Settings.canDrawOverlays(context)
                                ) {
                                    showFloatGrants = true
                                } else {
                                    context.startForegroundService(
                                        Intent(context, FloatService::class.java)
                                    )
                                    localMessage = strings.settings_float_title
                                }
                            } else {
                                context.stopService(Intent(context, FloatService::class.java))
                                localMessage = strings.settings_float_title
                            }
                        }
                    )
                }
            }
        }

        SectionTitle(strings.settings_permission_section)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                SettingsRow(
                    title = "Display over other apps",
                    subtitle = "Float window"
                ) {
                    TextButton(onClick = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    }) { Text("→") }
                }
                TextButton(onClick = { showPermissionNotice = true }) {
                    Text(strings.settings_permission_notice)
                }
            }
        }

        SectionTitle(strings.settings_github_section)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                SettingsRow(
                    title = strings.settings_github_token,
                    subtitle = if (settings.githubToken.isNotBlank()) "●●●●●●●●" + settings.githubToken.takeLast(4)
                    else strings.atk_need_token
                ) {
                    TextButton(onClick = { showTokenDialog = true }) { Text("✎") }
                }
                Text(
                    text = strings.settings_github_token_desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }

        SectionTitle(strings.settings_about_section)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                SettingsRow(
                    title = strings.settings_check_update,
                    subtitle = "GitHub Releases"
                ) {
                    if (updateState.checking) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp))
                    } else {
                        Button(onClick = { vm.checkUpdate("v1.0.1") }) { Text("↻") }
                    }
                }
                if (updateState.message.isNotEmpty()) {
                    Text(
                        updateState.message,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                localMessage?.let { msg ->
                    Text(
                        msg,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                Spacer4dp()
                Text(strings.settings_about_app, style = MaterialTheme.typography.titleLarge)
                Text(
                    strings.settings_about_desc,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    strings.settings_about_license,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall
                )
                Button(
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:" + context.getString(R.string.contact_email))
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                ) { Text(strings.settings_contact_email) }
                Button(
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.repo_url)))
                        )
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) { Text(strings.settings_open_repo) }
            }
        }
    }

    if (showFloatGrants) {
        AlertDialog(
            onDismissRequest = { showFloatGrants = false },
            title = { Text(strings.settings_float_title) },
            text = { Text(strings.settings_float_desc) },
            confirmButton = {
                TextButton(onClick = {
                    showFloatGrants = false
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                }) { Text("→") }
            },
            dismissButton = {
                TextButton(onClick = { showFloatGrants = false }) { Text("✕") }
            }
        )
    }

    if (showPermissionNotice) {
        AlertDialog(
            onDismissRequest = { showPermissionNotice = false },
            title = { Text(strings.permission_notice_title) },
            text = { Text(strings.permission_notice_body, fontSize = 12.sp) },
            confirmButton = {
                TextButton(onClick = { showPermissionNotice = false }) { Text("✕") }
            }
        )
    }

    if (showTokenDialog) {
        var tokenInput by remember { mutableStateOf(settings.githubToken) }
        AlertDialog(
            onDismissRequest = { showTokenDialog = false },
            title = { Text(strings.settings_github_token) },
            text = {
                Column {
                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = { tokenInput = it },
                        placeholder = { Text("ghp_... or github_pat_...") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        strings.settings_github_token_desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.setGithubToken(tokenInput.trim())
                    showTokenDialog = false
                }) { Text("✓") }
            },
            dismissButton = {
                TextButton(onClick = { showTokenDialog = false }) { Text("✕") }
            }
        )
    }

    updateState.notice?.let { notice ->
        AlertDialog(
            onDismissRequest = vm::dismissUpdate,
            title = { Text("${strings.settings_check_update}: ${notice.latestTag}", color = MaterialTheme.colorScheme.primary) },
            text = {
                Column {
                    Text("→ GitHub Releases", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        notice.changelog.ifEmpty { "—" },
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.repo_releases_url)))
                    )
                    vm.dismissUpdate()
                }) { Text("→") }
            },
            dismissButton = {
                TextButton(onClick = vm::dismissUpdate) { Text("✕") }
            }
        )
    }
}

@Composable
private fun Spacer4dp() {
    Box(modifier = Modifier.size(8.dp))
}

@Composable
fun ThemeColorPicker(selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AccentOptions.forEach { option ->
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(option.color, CircleShape)
                    .clickable { onSelect(option.name) }
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                if (option.name == selected) {
                    Icon(
                        Icons.Filled.Circle,
                        contentDescription = option.name,
                        tint = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
