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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixeleye.vcam.ui.state.CameraFacing
import com.pixeleye.vcam.ui.state.LensType
import com.pixeleye.vcam.ui.state.VcamUiState
import com.pixeleye.vcam.ui.theme.DarkSurfaceElevated
import com.pixeleye.vcam.ui.theme.DeepBackground
import com.pixeleye.vcam.ui.theme.ElectricBlue
import com.pixeleye.vcam.ui.theme.LiveRed
import com.pixeleye.vcam.ui.theme.NeonCyan
import com.pixeleye.vcam.ui.theme.TextPrimary
import com.pixeleye.vcam.ui.theme.TextSecondary
import com.pixeleye.vcam.ui.theme.TorchYellow

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
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Lens Zoom Selector (0.5x, 1.0x, 2.0x) - Visible for Back Camera
        if (uiState.cameraFacing == CameraFacing.BACK) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(DarkSurfaceElevated.copy(alpha = 0.9f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                LensType.entries.forEach { lens ->
                    val isSelected = uiState.selectedLens == lens
                    val animatedBg by animateColorAsState(
                        targetValue = if (isSelected) NeonCyan else Color.Transparent,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "lensBgAnimation"
                    )
                    val animatedText by animateColorAsState(
                        targetValue = if (isSelected) DeepBackground else TextSecondary,
                        label = "lensTextAnimation"
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(animatedBg)
                            .clickable { onSelectLens(lens) }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = lens.factor,
                            color = animatedText,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        } else {
            // Front Camera active indicator pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(DarkSurfaceElevated.copy(alpha = 0.9f))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Front Wide-Angle Active",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Main Controls Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Torch / Flashlight Button
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        if (uiState.isTorchActive) TorchYellow.copy(alpha = 0.2f)
                        else DarkSurfaceElevated
                    )
                    .border(
                        1.dp,
                        if (uiState.isTorchActive) TorchYellow else Color.White.copy(alpha = 0.1f),
                        CircleShape
                    )
                    .clickable { onToggleTorch() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (uiState.isTorchActive) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                    contentDescription = "Torch",
                    tint = if (uiState.isTorchActive) TorchYellow else TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Camera Switch Button (Front / Back)
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceElevated)
                    .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                    .clickable { onSwitchCamera() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Cameraswitch,
                    contentDescription = "Switch Camera",
                    tint = TextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Primary Start / Stop Stream Hero Button
            val streamButtonBg = if (uiState.isStreaming) {
                Brush.horizontalGradient(listOf(LiveRed, Color(0xFFD50000)))
            } else {
                Brush.horizontalGradient(listOf(ElectricBlue, NeonCyan))
            }

            Row(
                modifier = Modifier
                    .height(56.dp)
                    .weight(1f)
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .shadow(
                        elevation = if (uiState.isStreaming) 12.dp else 6.dp,
                        shape = RoundedCornerShape(28.dp),
                        ambientColor = if (uiState.isStreaming) LiveRed else NeonCyan,
                        spotColor = if (uiState.isStreaming) LiveRed else NeonCyan
                    )
                    .background(streamButtonBg)
                    .clickable { onToggleStream() }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (uiState.isStreaming) Icons.Default.Stop else Icons.Default.Videocam,
                    contentDescription = if (uiState.isStreaming) "Stop" else "Start",
                    tint = if (uiState.isStreaming) Color.White else DeepBackground,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (uiState.isStreaming) "STOP STREAM" else "START STREAM",
                    color = if (uiState.isStreaming) Color.White else DeepBackground,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            // Settings Button
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceElevated)
                    .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                    .clickable { onOpenSettings() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = TextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
