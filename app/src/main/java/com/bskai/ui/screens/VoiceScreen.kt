package com.bskai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bskai.voice.VoiceEngine
import com.bskai.ui.utils.FlowingText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceScreen(
    voiceEngine: VoiceEngine,
    currentResponse: String,
    isSpeaking: Boolean,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onClearHistory: () -> Unit,
    history: List<String>
) {
    var inputText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // History
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(history) { message ->
                ChatBubble(text = message, isUser = true)
            }
            if (currentResponse.isNotEmpty()) {
                item { ChatBubble(text = currentResponse, isUser = false) }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Input area
        if (isSpeaking) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("输入消息...") }
                )
                IconButton(onClick = { /* Send */ }) {
                    Text("发送", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun ChatBubble(text: String, isUser: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (isUser) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box(modifier = Modifier.padding(12.dp)) { FlowingText(text) }
    }
}
