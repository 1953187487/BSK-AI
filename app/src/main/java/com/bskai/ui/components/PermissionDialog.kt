package com.bskai.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject

@Composable
fun PermissionDialog(
    toolName: String,
    args: JSONObject,
    onAllow: () -> Unit,
    onDeny: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDeny,
        title = { Text("工具权限请求") },
        text = {
            Column {
                Text("智能体请求调用工具：", style = MaterialTheme.typography.bodyMedium)
                Text(toolName, style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Monospace)
                Text(args.toString(), fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            Row {
                OutlinedButton(onClick = onDeny) {
                    Text("拒绝")
                }
                Spacer(Modifier.width(8.dp))
                Button(onClick = onAllow) {
                    Text("允许")
                }
            }
        }
    )
}
