package com.smarthome.gitops.presentation.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Color Palette ────────────────────────────────────────────────────────────

// Normal (secure) state — deep teal/cyan
val NormalBackground    = Color(0xFF0A1628)
val NormalSurface       = Color(0xFF0D2137)
val NormalAccent        = Color(0xFF00E5C3)   // vibrant teal
val NormalSecondary     = Color(0xFF1A3A5C)
val NormalGreen         = Color(0xFF00C896)
val NormalGreenDim      = Color(0xFF004D3A)

// Alert state — deep crimson
val AlertBackground     = Color(0xFF1A0A0A)
val AlertSurface        = Color(0xFF2D0D0D)
val AlertAccent         = Color(0xFFFF3B3B)   // vivid red
val AlertSecondary      = Color(0xFF5C1A1A)
val AlertOrange         = Color(0xFFFF8C00)

// Text
val TextPrimary         = Color(0xFFE8F4F8)
val TextSecondary       = Color(0xFF8AB0C8)
val TextMuted           = Color(0xFF4A6A80)

// Shared
val CardBackground      = Color(0x1AFFFFFF)   // 10% white overlay
val DividerColor        = Color(0x26FFFFFF)   // 15% white

private val DarkColorScheme = darkColorScheme(
    primary = NormalAccent,
    onPrimary = Color.Black,
    secondary = NormalSecondary,
    background = NormalBackground,
    surface = NormalSurface,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun SmartHomeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
