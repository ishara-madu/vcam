package com.pixeleye.vcam.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixeleye.vcam.ui.state.ConnectionMode
import com.pixeleye.vcam.ui.state.ConnectionStatus
import com.pixeleye.vcam.ui.state.VcamUiState
import com.pixeleye.vcam.ui.theme.DarkSurface
import com.pixeleye.vcam.ui.theme.GlassBorder
import com.pixeleye.vcam.ui.theme.StatusAmber
import com.pixeleye.vcam.ui.theme.StatusGreen
import com.pixeleye.vcam.ui.theme.StatusRed
import com.pixeleye.vcam.ui.theme.TextPrimary
import com.pixeleye.vcam.ui.theme.TextSecondary

@Composable
fun TopStatusBar(
    uiState: VcamUiState,
    onToggleConnectionMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val statusDotColor = when (uiState.connectionStatus) {
        ConnectionStatus.CONNECTED_USB, ConnectionStatus.CONNECTED_WIFI -> StatusGreen
        ConnectionStatus.WAITING_FOR_MAC -> StatusAmber
        ConnectionStatus.DISCONNECTED -> StatusRed
    }

    val statusText = when (uiState.connectionStatus) {
        ConnectionStatus.CONNECTED_USB -> "USB Connected"
        ConnectionStatus.CONNECTED_WIFI -> "Wi-Fi Connected"
        ConnectionStatus.WAITING_FOR_MAC -> "Waiting for Mac"
        ConnectionStatus.DISCONNECTED -> "Offline"
    }

    val fullAddress = "${uiState.ipAddress}:${uiState.port}"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Floating Ultra-Thin Translucent Pill
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(DarkSurface.copy(alpha = 0.85f))
                .border(0.75.dp, GlassBorder, RoundedCornerShape(24.dp))
                .padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Status Dot
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .alpha(if (uiState.connectionStatus == ConnectionStatus.WAITING_FOR_MAC) pulseAlpha else 1f)
                    .clip(CircleShape)
                    .background(statusDotColor)
            )

            Spacer(modifier = Modifier.width(7.dp))

            // Status label
            Text(
                text = statusText,
                color = TextPrimary.copy(alpha = 0.9f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Hairline vertical divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(11.dp)
                    .background(Color.White.copy(alpha = 0.15f))
            )

            Spacer(modifier = Modifier.width(10.dp))

            // IP Address:Port (Tap to copy)
            Row(
                modifier = Modifier
                    .clickable {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("vcam IP", fullAddress)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Copied $fullAddress", Toast.LENGTH_SHORT).show()
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = fullAddress,
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.width(5.dp))

                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy",
                    tint = TextSecondary,
                    modifier = Modifier.size(11.dp)
                )
            }
        }

        // Minimalist Transport Mode Selector
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurface.copy(alpha = 0.5f))
                .clickable { onToggleConnectionMode() }
                .padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (uiState.connectionMode == ConnectionMode.USB) Icons.Default.Cable else Icons.Default.Wifi,
                contentDescription = "Transport",
                tint = TextSecondary,
                modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = if (uiState.connectionMode == ConnectionMode.USB) "USB (adb forward :${uiState.port})" else "Wi-Fi (Direct LAN)",
                color = TextSecondary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
