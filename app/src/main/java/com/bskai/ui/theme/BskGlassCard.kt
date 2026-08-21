package com.bskai.ui.theme

import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun BskGlassCard(
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    elevation: Dp = 4.dp,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = shape,
        colors = BskGlassCardDefaults.cardColors(),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        content()
    }
}

object BskGlassCardDefaults {
    @Composable
    fun cardColors() = CardDefaults.cardColors(
        containerColor = if (isSystemInDarkTheme()) Color(0x301A1730) else Color(0xCCFFFFFF)
    )
}
