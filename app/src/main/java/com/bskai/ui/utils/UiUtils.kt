package com.bskai.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.Text
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.delay
import androidx.compose.ui.unit.sp

@Composable
fun FlowingText(text: String, style: TextStyle = TextStyle(fontSize = 16.sp, lineHeight = 22.sp)) {
    var displayedText by remember { mutableStateOf("") }

    LaunchedEffect(text) {
        displayedText = ""
        for (char in text) {
            delay(25L)
            displayedText += char
        }
    }

    Text(text = displayedText, style = style)
}
