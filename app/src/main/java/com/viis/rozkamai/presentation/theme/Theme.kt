package com.viis.rozkamai.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AppGreen = Color(0xFF2E7D32)
private val AppRed = Color(0xFFC62828)
private val AppAmber = Color(0xFFF57F17)
private val AppBackground = Color(0xFFFAFAFA)

private val LightColorScheme = lightColorScheme(
    primary = AppGreen,
    error = AppRed,
    background = AppBackground,
    surface = Color.White,
)

@Composable
fun RozKamaiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
