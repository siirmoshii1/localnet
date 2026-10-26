package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalAccentColor = staticCompositionLocalOf { AccentColorPreset.MONOCHROME }

@Composable
fun GhostLinkTheme(
    accentPreset: AccentColorPreset = AccentColorPreset.MONOCHROME,
    content: @Composable () -> Unit
) {
    val accent = accentPreset.color

    val colorScheme = darkColorScheme(
        primary = accent,
        onPrimary = TrueBlack,
        primaryContainer = DarkElevated,
        onPrimaryContainer = accent,
        secondary = accent,
        onSecondary = TrueBlack,
        secondaryContainer = DarkElevated,
        onSecondaryContainer = PureWhite,
        tertiary = accent,
        onTertiary = TrueBlack,
        background = TrueBlack,
        onBackground = PureWhite,
        surface = DarkSurface,
        onSurface = PureWhite,
        surfaceVariant = DarkElevated,
        onSurfaceVariant = TextMuted,
        outline = DarkBorder,
        outlineVariant = DarkBorderSubtle,
        error = Color(0xFFFF453A),
        onError = TrueBlack
    )

    CompositionLocalProvider(LocalAccentColor provides accentPreset) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
