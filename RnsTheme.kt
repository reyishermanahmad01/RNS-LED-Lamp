package com.rns.controller.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val RnsBackground = Color(0xFF0A1026)
val RnsNightTop = Color(0xFF060B26)
val RnsNightBottom = Color(0xFF005AA7)
val RnsHeaderBlue = Color(0xFF0084D8)
val RnsBlueAccent = Color(0xFF1D9BF0)
val RnsSurface = Color(0xFF101935)
val RnsSurfaceElevated = Color(0xFF182447)
val RnsBorder = Color(0xFF283A63)
val RnsPrimary = Color(0xFF0084D8)
val RnsSecondary = Color(0xFFF59E0B)
val RnsSuccess = Color(0xFF22C55E)
val RnsDanger = Color(0xFFEF4444)
val RnsTextPrimary = Color(0xFFF8FAFC)
val RnsTextMuted = Color(0xFF94A3B8)
val RnsLightBg = Color(0xFFF4F6FA)

private val RnsDarkScheme = darkColorScheme(
    primary = RnsPrimary,
    secondary = RnsSecondary,
    background = RnsBackground,
    surface = RnsSurface,
    surfaceVariant = RnsSurfaceElevated,
    onPrimary = Color.White,
    onBackground = RnsTextPrimary,
    onSurface = RnsTextPrimary,
    onSurfaceVariant = RnsTextMuted,
    error = RnsDanger
)

@Composable
fun RnsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RnsDarkScheme,
        content = content
    )
}
