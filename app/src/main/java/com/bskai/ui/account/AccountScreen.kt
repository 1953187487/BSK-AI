package com.bskai.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.bskai.AuraApp
import com.bskai.R
import com.bskai.account.AuraApi
import com.bskai.account.AuraClient
import com.bskai.account.AuraHosts
import com.bskai.account.OfficialLoginRequest
import com.bskai.account.AuraTokenResponse
import kotlinx.coroutines.launch

/**
 * AURA「账户」Tab(对齐策划书二/三/13.7):
 * - 普通用户:QQ/微信 登录(此处仅展示,真实 OAuth 由 SDK 完成)
 * - 官方账号:「官方登录」页,输入 ID + 账号名称 + 密码三字段
 *   成功即 role=ADMIN,加载管理员后台代码
 */
@Composable
fun AccountScreen(app: AuraApp) {
    var showOfficial by remember { mutableStateOf(false) }
    var loginResult by remember { mutableStateOf<AuraTokenResponse?>(null) }
    var accessToken by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        // 预留:此处可挂 QQ/微信 OAuth 回调
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.account_title), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        if (loginResult != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(
                        R.string.account_login_success,
                        loginResult!!.role.orEmpty(),
                        if (loginResult!!.official == true) stringResource(R.string.common_yes)
                        else stringResource(R.string.common_no)
                    ),
                    modifier = Modifier.padding(16.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        if (showOfficial) {
            OfficialLoginForm(app = app) { tokens ->
                accessToken = tokens.accessToken
                loginResult = tokens
            }
        } else {
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showOfficial = true },
            ) {
                Text(stringResource(R.string.account_official_login))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.account_oauth_hint),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun OfficialLoginForm(app: AuraApp, onSuccess: (AuraTokenResponse) -> Unit) {
    var id by remember { mutableStateOf("") }
    var accountName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val loginFailed = stringResource(R.string.account_login_failed)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.account_official_login_fields), style = MaterialTheme.typography.titleMedium)
        TextField(
            value = id,
            onValueChange = { id = it },
            label = { Text(stringResource(R.string.account_field_id)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        TextField(
            value = accountName,
            onValueChange = { accountName = it },
            label = { Text(stringResource(R.string.account_field_name)) },
            singleLine = true,
        )
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.account_field_password)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
        )
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = !busy && id.isNotBlank() && accountName.isNotBlank() && password.isNotBlank(),
            onClick = {
                busy = true
                error = null
                scope.launch {
                    try {
                        val api: AuraApi = AuraClient.create(AuraHosts.DEFAULT_BASE)
                        val resp = api.officialLogin(
                            OfficialLoginRequest(id, accountName, password)
                        )
                        onSuccess(resp)
                    } catch (e: Exception) {
                        error = e.message ?: loginFailed
                    } finally {
                        busy = false
                    }
                }
            },
        ) {
            Text(if (busy) stringResource(R.string.account_verifying) else stringResource(R.string.account_login))
        }
    }
}
