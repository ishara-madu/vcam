package com.pixeleye.vcam.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VCamMinimalDarkScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = PureBlack,
    primaryContainer = DarkSurfaceElevated,
    onPrimaryContainer = Color.White,
    secondary = TextSecondary,
    onSecondary = Color.White,
    tertiary = StatusGreen,
    onTertiary = PureBlack,
    background = PureBlack,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = GlassBorder
)

@Composable
fun VcamTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = VCamMinimalDarkScheme,
        typography = Typography,
        content = content
    )
}
