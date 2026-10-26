package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Strict Black and White Palette
val TrueBlack = Color(0xFF000000)
val DarkSurface = Color(0xFF0D0D0D)
val DarkElevated = Color(0xFF161616)
val DarkBorder = Color(0xFF262626)
val DarkBorderSubtle = Color(0xFF1E1E1E)
val TextMuted = Color(0xFF888888)
val TextDim = Color(0xFF555555)
val PureWhite = Color(0xFFFFFFFF)

// Accent Color Presets
enum class AccentColorPreset(val displayName: String, val color: Color, val hex: String) {
    MONOCHROME("Monochrome White", Color(0xFFFFFFFF), "#FFFFFF"),
    CYBERPUNK_GREEN("Cyberpunk Green", Color(0xFF00FF66), "#00FF66"),
    NEON_BLUE("Neon Blue", Color(0xFF00E5FF), "#00E5FF"),
    HOT_PINK("Hot Pink", Color(0xFFFF007F), "#FF007F"),
    AMBER_GOLD("Amber Gold", Color(0xFFFFB800), "#FFB800"),
    ELECTRIC_PURPLE("Electric Purple", Color(0xFFB5179E), "#B5179E")
}
