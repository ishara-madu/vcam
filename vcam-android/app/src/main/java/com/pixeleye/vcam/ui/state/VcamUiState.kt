package com.pixeleye.vcam.ui.state

enum class ConnectionMode {
    USB,
    WIFI
}

enum class ConnectionStatus(val label: String) {
    DISCONNECTED("Disconnected"),
    WAITING_FOR_MAC("Waiting for Mac..."),
    CONNECTED_USB("Connected via USB"),
    CONNECTED_WIFI("Connected via Wi-Fi")
}

enum class CameraFacing(val label: String) {
    BACK("Back Camera"),
    FRONT("Front Camera")
}

enum class LensType(val label: String, val factor: String) {
    ULTRAWIDE("Ultra-wide", "0.5x"),
    WIDE("Wide (Main)", "1.0x"),
    TELEPHOTO("Telephoto", "2.0x")
}

enum class VideoResolution(val label: String, val width: Int, val height: Int) {
    HD_720P("720p", 1280, 720),
    FHD_1080P("1080p", 1920, 1080),
    UHD_4K("4K", 3840, 2160)
}

data class VcamUiState(
    val isStreaming: Boolean = false,
    val isOledSaverActive: Boolean = false,
    val isTorchActive: Boolean = false,
    val cameraFacing: CameraFacing = CameraFacing.BACK,
    val selectedLens: LensType = LensType.WIDE,
    val connectionMode: ConnectionMode = ConnectionMode.USB,
    val connectionStatus: ConnectionStatus = ConnectionStatus.WAITING_FOR_MAC,
    val ipAddress: String = "192.168.1.15",
    val port: Int = 8088,
    val resolution: VideoResolution = VideoResolution.FHD_1080P,
    val fps: Int = 60,
    val bitrateMbps: Int = 12,
    val isSettingsOpen: Boolean = false
)
