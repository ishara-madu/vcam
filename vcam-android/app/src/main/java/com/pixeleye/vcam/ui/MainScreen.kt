package com.pixeleye.vcam.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import com.pixeleye.vcam.ui.theme.PureBlack
import com.pixeleye.vcam.ui.theme.VcamTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    initialIp: String = "192.168.1.15",
    onStartStreamRequested: (VideoResolution, Int, Int, CameraFacing, LensType, Int) -> Unit = { _, _, _, _, _, _ -> },
    onStopStreamRequested: () -> Unit = {},
    onSwitchCameraRequested: (CameraFacing) -> Unit = {},
    onLensChanged: (LensType) -> Unit = {},
    onTorchToggled: (Boolean) -> Unit = {},
    onBitrateChanged: (Int) -> Unit = {},
    registerClientCallbacks: (((String) -> Unit), (() -> Unit), ((String) -> Unit)) -> Unit = { _, _, _ -> },
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
                ipAddress = initialIp,
                port = 8088,
                resolution = VideoResolution.FHD_1080P,
                fps = 60,
                bitrateMbps = 12,
                isSettingsOpen = false
            )
        )
    }

    LaunchedEffect(Unit) {
        registerClientCallbacks(
            { clientIp ->
                uiState = uiState.copy(
                    connectionStatus = if (uiState.connectionMode == ConnectionMode.USB) {
                        ConnectionStatus.CONNECTED_USB
                    } else {
                        ConnectionStatus.CONNECTED_WIFI
                    }
                )
            },
            {
                uiState = uiState.copy(
                    connectionStatus = if (uiState.isStreaming) {
                        ConnectionStatus.WAITING_FOR_MAC
                    } else {
                        ConnectionStatus.DISCONNECTED
                    }
                )
            },
            { freshIp ->
                if (freshIp.isNotEmpty() && freshIp != "127.0.0.1") {
                    uiState = uiState.copy(ipAddress = freshIp)
                }
            }
        )
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack),
        containerColor = PureBlack
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Layer 1: Edge-to-Edge Camera Viewfinder
            PreviewArea(
                uiState = uiState,
                onToggleOledSaver = {
                    uiState = uiState.copy(isOledSaverActive = !uiState.isOledSaverActive)
                },
                modifier = Modifier.fillMaxSize()
            )

            // Layer 2: Floating Minimal Top Status Bar
            if (!uiState.isOledSaverActive) {
                TopStatusBar(
                    uiState = uiState,
                    onToggleConnectionMode = {
                        val newMode = if (uiState.connectionMode == ConnectionMode.USB) ConnectionMode.WIFI else ConnectionMode.USB
                        uiState = uiState.copy(
                            connectionMode = newMode,
                            connectionStatus = if (uiState.isStreaming) {
                                if (newMode == ConnectionMode.USB) ConnectionStatus.CONNECTED_USB else ConnectionStatus.CONNECTED_WIFI
                            } else {
                                ConnectionStatus.WAITING_FOR_MAC
                            }
                        )
                    },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                )
            }

            // Layer 3: Floating Minimal Bottom Controls
            if (!uiState.isOledSaverActive) {
                BottomControlPanel(
                    uiState = uiState,
                    onToggleStream = {
                        val willStream = !uiState.isStreaming
                        uiState = uiState.copy(
                            isStreaming = willStream,
                            connectionStatus = if (willStream) ConnectionStatus.WAITING_FOR_MAC else ConnectionStatus.DISCONNECTED
                        )

                        if (willStream) {
                            onStartStreamRequested(
                                uiState.resolution,
                                uiState.fps,
                                uiState.bitrateMbps,
                                uiState.cameraFacing,
                                uiState.selectedLens,
                                uiState.port
                            )
                        } else {
                            onStopStreamRequested()
                        }
                    },
                    onToggleTorch = {
                        val newTorch = !uiState.isTorchActive
                        uiState = uiState.copy(isTorchActive = newTorch)
                        onTorchToggled(newTorch)
                    },
                    onSwitchCamera = {
                        val newFacing = if (uiState.cameraFacing == CameraFacing.BACK) CameraFacing.FRONT else CameraFacing.BACK
                        uiState = uiState.copy(cameraFacing = newFacing)
                        onSwitchCameraRequested(newFacing)
                    },
                    onSelectLens = { lens ->
                        uiState = uiState.copy(selectedLens = lens)
                        onLensChanged(lens)
                    },
                    onOpenSettings = {
                        uiState = uiState.copy(isSettingsOpen = true)
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                )
            }

            // Modal Bottom Sheet for Settings
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
                        onBitrateChanged(bitrate)
                    },
                    onSelectConnectionMode = { mode ->
                        uiState = uiState.copy(connectionMode = mode)
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun MainScreenPreview() {
    VcamTheme {
        MainScreen()
    }
}
