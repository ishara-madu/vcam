package com.pixeleye.vcam.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VCamDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = DeepBackground,
    primaryContainer = ElectricBlue,
    onPrimaryContainer = Color.White,
    secondary = ElectricBlue,
    onSecondary = Color.White,
    tertiary = LiveGreen,
    onTertiary = DeepBackground,
    background = DeepBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle
)

@Composable
fun VcamTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = VCamDarkColorScheme,
        typography = Typography,
        content = content
    )
}
