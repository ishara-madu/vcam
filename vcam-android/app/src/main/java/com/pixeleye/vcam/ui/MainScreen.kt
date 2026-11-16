package com.pixeleye.vcam.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pixeleye.vcam.ui.components.BottomControlPanel
import com.pixeleye.vcam.ui.components.PreviewArea
import com.pixeleye.vcam.ui.components.SettingsSheet
import com.pixeleye.vcam.ui.components.TopStatusBar
import com.pixeleye.vcam.ui.state.CameraFacing
import com.pixeleye.vcam.ui.state.ConnectionMode
import com.pixeleye.vcam.ui.state.ConnectionStatus
import com.pixeleye.vcam.ui.state.LensType
import com.pixeleye.vcam.ui.state.VcamUiState
import com.pixeleye.vcam.ui.state.VideoResolution
import com.pixeleye.vcam.ui.theme.DeepBackground
import com.pixeleye.vcam.ui.theme.PureBlack
import com.pixeleye.vcam.ui.theme.VcamTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier
) {
    var uiState by remember {
        mutableStateOf(
            VcamUiState(
                isStreaming = false,
                isOledSaverActive = false,
                isTorchActive = false,
                cameraFacing = CameraFacing.BACK,
                selectedLens = LensType.WIDE,
                connectionMode = ConnectionMode.USB,
                connectionStatus = ConnectionStatus.WAITING_FOR_MAC,
                ipAddress = "192.168.1.15",
                port = 8088,
                resolution = VideoResolution.FHD_1080P,
                fps = 60,
                bitrateMbps = 12,
                isSettingsOpen = false
            )
        )
    }

    val backgroundColor = if (uiState.isOledSaverActive) PureBlack else DeepBackground

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor),
        containerColor = backgroundColor
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top Status Bar (Only visible or bright when not in full OLED saver)
                if (!uiState.isOledSaverActive) {
                    TopStatusBar(
                        uiState = uiState,
                        onToggleConnectionMode = {
                            uiState = uiState.copy(
                                connectionMode = if (uiState.connectionMode == ConnectionMode.USB) {
                                    ConnectionMode.WIFI
                                } else {
                                    ConnectionMode.USB
                                },
                                connectionStatus = if (uiState.connectionMode == ConnectionMode.USB) {
                                    ConnectionStatus.CONNECTED_WIFI
                                } else {
                                    ConnectionStatus.CONNECTED_USB
                                }
                            )
                        }
                    )
                }

                // Middle Viewfinder & OLED Battery Saver Area
                PreviewArea(
                    uiState = uiState,
                    onToggleOledSaver = {
                        uiState = uiState.copy(isOledSaverActive = !uiState.isOledSaverActive)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = if (uiState.isOledSaverActive) 0.dp else 4.dp)
                )

                // Bottom Control Panel
                if (!uiState.isOledSaverActive) {
                    Spacer(modifier = Modifier.height(8.dp))
                    BottomControlPanel(
                        uiState = uiState,
                        onToggleStream = {
                            val newStreaming = !uiState.isStreaming
                            uiState = uiState.copy(
                                isStreaming = newStreaming,
                                connectionStatus = if (newStreaming) {
                                    if (uiState.connectionMode == ConnectionMode.USB) {
                                        ConnectionStatus.CONNECTED_USB
                                    } else {
                                        ConnectionStatus.CONNECTED_WIFI
                                    }
                                } else {
                                    ConnectionStatus.WAITING_FOR_MAC
                                }
                            )
                        },
                        onToggleTorch = {
                            uiState = uiState.copy(isTorchActive = !uiState.isTorchActive)
                        },
                        onSwitchCamera = {
                            uiState = uiState.copy(
                                cameraFacing = if (uiState.cameraFacing == CameraFacing.BACK) {
                                    CameraFacing.FRONT
                                } else {
                                    CameraFacing.BACK
                                }
                            )
                        },
                        onSelectLens = { lens ->
                            uiState = uiState.copy(selectedLens = lens)
                        },
                        onOpenSettings = {
                            uiState = uiState.copy(isSettingsOpen = true)
                        }
                    )
                }
            }

            // Settings Modal Bottom Sheet
            if (uiState.isSettingsOpen) {
                SettingsSheet(
                    uiState = uiState,
                    onDismiss = {
                        uiState = uiState.copy(isSettingsOpen = false)
                    },
                    onSelectResolution = { res ->
                        uiState = uiState.copy(resolution = res)
                    },
                    onSelectFps = { fps ->
                        uiState = uiState.copy(fps = fps)
                    },
                    onSelectBitrate = { bitrate ->
                        uiState = uiState.copy(bitrateMbps = bitrate)
                    },
                    onSelectConnectionMode = { mode ->
                        uiState = uiState.copy(
                            connectionMode = mode,
                            connectionStatus = if (mode == ConnectionMode.USB) {
                                ConnectionStatus.CONNECTED_USB
                            } else {
                                ConnectionStatus.CONNECTED_WIFI
                            }
                        )
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF090A0C)
@Composable
fun MainScreenPreview() {
    VcamTheme {
        MainScreen()
    }
}
