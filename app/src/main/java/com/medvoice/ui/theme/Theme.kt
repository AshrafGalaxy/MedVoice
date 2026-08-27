package com.medvoice.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = SafeGreen,
    onPrimary = TextWhite,
    secondary = ReticleCyan,
    onSecondary = TextWhite,
    error = AlertRed,
    onError = TextWhite,
    background = BackgroundCharcoal,
    onBackground = TextWhite,
    surface = SurfaceCardDark,
    onSurface = TextWhite
)

@Composable
fun MedVoiceTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
