package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = Color.White,
    secondary = LightBlue,
    onSecondary = Color.White,
    background = DarkBg,
    onBackground = TextWhite,
    surface = DarkCard,
    onSurface = TextWhite,
    surfaceVariant = DarkButton,
    onSurfaceVariant = TextWhite,
    outline = DarkBorder,
    surfaceContainer = BottomNavBg
)

private val PitchBlackColorScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = Color.White,
    secondary = LightBlue,
    onSecondary = Color.White,
    background = PitchBlackBg,
    onBackground = TextWhite,
    surface = PitchBlackCard,
    onSurface = TextWhite,
    surfaceVariant = PitchBlackButton,
    onSurfaceVariant = TextWhite,
    outline = PitchBlackBorder,
    surfaceContainer = PitchBlackBottomNav
)

@Composable
fun MyApplicationTheme(
    isBlackTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = if (isBlackTheme) PitchBlackColorScheme else DarkColorScheme
    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}
