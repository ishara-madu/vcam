package com.pixeleye.vcam.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixeleye.vcam.ui.state.CameraFacing
import com.pixeleye.vcam.ui.state.LensType
import com.pixeleye.vcam.ui.state.VcamUiState
import com.pixeleye.vcam.ui.theme.DarkSurface
import com.pixeleye.vcam.ui.theme.GlassBorder
import com.pixeleye.vcam.ui.theme.PureBlack
import com.pixeleye.vcam.ui.theme.StatusRed
import com.pixeleye.vcam.ui.theme.StatusYellow
import com.pixeleye.vcam.ui.theme.TextPrimary
import com.pixeleye.vcam.ui.theme.TextSecondary

@Composable
fun BottomControlPanel(
    uiState: VcamUiState,
    onToggleStream: () -> Unit,
    onToggleTorch: () -> Unit,
    onSwitchCamera: () -> Unit,
    onSelectLens: (LensType) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Floating Minimal Lens Pills (.5, 1x, 2x)
        if (uiState.cameraFacing == CameraFacing.BACK) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(DarkSurface.copy(alpha = 0.75f))
                    .border(0.75.dp, GlassBorder, RoundedCornerShape(20.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                LensType.entries.forEach { lens ->
                    val isSelected = uiState.selectedLens == lens
                    val animatedBg by animateColorAsState(
                        targetValue = if (isSelected) Color.White else Color.Transparent,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "lensBg"
                    )
                    val animatedTextColor by animateColorAsState(
                        targetValue = if (isSelected) PureBlack else TextSecondary,
                        label = "lensText"
                    )

                    Box(
                        modifier = Modifier
                            .size(width = 36.dp, height = 26.dp)
                            .clip(CircleShape)
                            .background(animatedBg)
                            .clickable { onSelectLens(lens) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = lens.factor.replace("x", ""),
                            color = animatedTextColor,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Main Minimal Shutter Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Torch Button (Translucent Glass Circle)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(DarkSurface.copy(alpha = 0.8f))
                    .border(
                        0.75.dp,
                        if (uiState.isTorchActive) StatusYellow else GlassBorder,
                        CircleShape
                    )
                    .clickable { onToggleTorch() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (uiState.isTorchActive) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                    contentDescription = "Torch",
                    tint = if (uiState.isTorchActive) StatusYellow else TextSecondary,
                    modifier = Modifier.size(19.dp)
                )
            }

            // Central Camera Shutter Button (Apple/Leica style)
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color.White.copy(alpha = 0.85f), CircleShape)
                    .clickable { onToggleStream() }
                    .padding(5.dp),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.isStreaming) {
                    // Recording Red Square Stop Button
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(StatusRed)
                    )
                } else {
                    // Solid White Shutter Button
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
            }

            // Camera Switch Button
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(DarkSurface.copy(alpha = 0.8f))
                    .border(0.75.dp, GlassBorder, CircleShape)
                    .clickable { onSwitchCamera() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Cameraswitch,
                    contentDescription = "Switch",
                    tint = TextPrimary.copy(alpha = 0.85f),
                    modifier = Modifier.size(19.dp)
                )
            }

            // Settings Button
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(DarkSurface.copy(alpha = 0.8f))
                    .border(0.75.dp, GlassBorder, CircleShape)
                    .clickable { onOpenSettings() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Settings",
                    tint = TextPrimary.copy(alpha = 0.85f),
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}
