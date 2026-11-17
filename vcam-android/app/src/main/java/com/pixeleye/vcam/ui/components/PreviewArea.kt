package com.pixeleye.vcam.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixeleye.vcam.ui.state.CameraFacing
import com.pixeleye.vcam.ui.state.VcamUiState
import com.pixeleye.vcam.ui.theme.DarkSurface
import com.pixeleye.vcam.ui.theme.GlassBorder
import com.pixeleye.vcam.ui.theme.PureBlack
import com.pixeleye.vcam.ui.theme.StatusRed
import com.pixeleye.vcam.ui.theme.TextPrimary
import com.pixeleye.vcam.ui.theme.TextSecondary

@Composable
fun PreviewArea(
    uiState: VcamUiState,
    onToggleOledSaver: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulseLive")
    val liveGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "liveAlpha"
    )

    Crossfade(
        targetState = uiState.isOledSaverActive,
        label = "oledCrossfade",
        modifier = modifier
    ) { isOledActive ->
        if (isOledActive) {
            // Pure Black OLED Saver Canvas
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PureBlack)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onToggleOledSaver() },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "OLED Saver Active",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Tap to wake viewfinder",
                        color = Color.White.copy(alpha = 0.2f),
                        fontSize = 10.sp
                    )
                }
            }
        } else {
            // Edge-to-Edge Minimal Viewfinder
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0A0B0E))
            ) {
                // Subtle Center Crosshair / Viewfinder Tick
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .border(0.75.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                    )
                }

                // Top Minimal Overlays
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // LIVE / STANDBY Indicator
                    if (uiState.isStreaming) {
                        Row(
                            modifier = Modifier
                                .clip(CapsuleShape)
                                .background(DarkSurface.copy(alpha = 0.75f))
                                .border(0.75.dp, GlassBorder, CapsuleShape)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .alpha(liveGlowAlpha)
                                    .clip(CircleShape)
                                    .background(StatusRed)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "REC",
                                color = StatusRed,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.5.sp
                            )
                        }
                    } else {
                        Text(
                            text = "READY",
                            color = TextSecondary.copy(alpha = 0.6f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier
                                .clip(CapsuleShape)
                                .background(DarkSurface.copy(alpha = 0.6f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // OLED Dim Mode Trigger
                    Row(
                        modifier = Modifier
                            .clip(CapsuleShape)
                            .background(DarkSurface.copy(alpha = 0.75f))
                            .border(0.75.dp, GlassBorder, CapsuleShape)
                            .clickable { onToggleOledSaver() }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.VisibilityOff,
                            contentDescription = "OLED",
                            tint = TextSecondary,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "OLED Dim",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Bottom Viewfinder Telemetry Pill
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clip(CapsuleShape)
                            .background(DarkSurface.copy(alpha = 0.8f))
                            .border(0.75.dp, GlassBorder, CapsuleShape)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${uiState.resolution.label} ${uiState.fps}fps",
                            color = TextPrimary.copy(alpha = 0.85f),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = " • ",
                            color = TextSecondary.copy(alpha = 0.4f),
                            fontSize = 10.sp
                        )
                        Text(
                            text = "${uiState.bitrateMbps} Mb/s",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

private val CapsuleShape = RoundedCornerShape(16.dp)
