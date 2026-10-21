package com.cube.channelonly.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF334A),
    onPrimary = Color.White,
    secondary = Color(0xFFCACACA),
    background = Color(0xFF0F0F0F),
    surface = Color(0xFF181818),
    onBackground = Color(0xFFF5F5F5),
    onSurface = Color(0xFFF5F5F5)
)

@Composable
fun ChannelOnlyTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
