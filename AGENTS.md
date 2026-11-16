# vcam — Android to Mac Virtual Camera Development Guidelines

## Project Overview

**vcam** is an ultra-low-latency (sub-30ms), high-performance video streaming system that turns an Android device into a high-definition native macOS **Virtual Camera** (`CMIOExtension`). The virtual camera appears system-wide in macOS applications such as FaceTime, Zoom, Google Meet, Microsoft Teams, OBS Studio, QuickTime, and web browsers.

The project consists of two tightly coupled native components:
1. **`vcam-android`**: Native Android app written in **Kotlin** utilizing **Camera2 / CameraX** and hardware-accelerated **MediaCodec** (H.264/HEVC) for zero-copy, real-time video encoding over USB (ADB tunnel) or high-speed Wi-Fi LAN.
2. **`vcam-mac`**: Native macOS client & Virtual Camera extension written in **Swift** and **SwiftUI**, utilizing **VideoToolbox** for hardware decompression and **CoreMediaIO (`CMIOExtension`)** to publish a virtual webcam stream to macOS.

> [!IMPORTANT]
> **Strict Command-Line (CLI) Only Policy**:
> - **No Xcode GUI**: Do NOT use or suggest using the Xcode application GUI. The entire macOS lifecycle (creating files, compiling, testing, running, and extension packaging) is executed strictly via the terminal using the `swift` command-line tools.
> - **No Android Studio GUI**: Manage Android builds, dependency resolutions, and device installations strictly via `./gradlew` and `adb`.
> - **No `.xcodeproj` Dependency**: Manage all macOS dependencies, targets, and compilation via **Swift Package Manager (`Package.swift`)**.

---

## Command-Line Toolchain & Commands

### macOS Client (`vcam-mac`) Toolchain
All commands must be run from the `vcam-mac` directory:

| Task | Command | Description |
| :--- | :--- | :--- |
| **Run App** | `swift run` | Compiles and immediately launches the macOS preview & control window |
| **Build Debug** | `swift build` | Compiles debug binaries for client app & core modules |
| **Build Release** | `swift build -c release` | Compiles optimized release binaries |
| **Run Tests** | `swift test` | Executes unit and pipeline test suites |
| **Clean Build** | `swift package clean` | Cleans build artifacts (`.build/` directory) |
| **Resolve Dependencies** | `swift package resolve` | Resolves SPM packages listed in `Package.swift` |
| **Update Dependencies** | `swift package update` | Updates packages to the latest compatible versions |

#### Creating a Standalone macOS `.app` Bundle (CLI Only)
To launch with full macOS app permissions (camera entitlements, extension hosting, network access) without Xcode GUI:

```bash
# Assemble standalone macOS application bundle
mkdir -p build/vcam.app/Contents/{MacOS,Resources,Library/SystemExtensions}
cp $(swift build -c release --show-bin-path)/vcam build/vcam.app/Contents/MacOS/
cp Resources/Info.plist build/vcam.app/Contents/
```

---

### Android Client (`vcam-android`) Toolchain
All commands must be run from the `vcam-android` directory:

| Task | Command | Description |
| :--- | :--- | :--- |
| **Build Debug APK** | `./gradlew assembleDebug` | Compiles debug APK |
| **Install on Device** | `./gradlew installDebug` | Builds and installs APK onto connected Android device |
| **Build Release APK** | `./gradlew assembleRelease` | Compiles release APK |
| **Run App on Device** | `adb shell am start -n com.pixeleye.vcam/.MainActivity` | Launches app on device via ADB |
| **View Real-time Logs**| `adb logcat -s VCam VCamEncoder VCamStreamer` | Streams app-specific logcat output |
| **Clean Build** | `./gradlew clean` | Cleans Android Gradle build caches |

---

### Ultra-Fast Connection (USB Port Forwarding via ADB)
For the absolute lowest latency and zero packet loss, route streaming traffic over USB via ADB:

```bash
# Forward Mac port 8088 to Android port 8088 over USB cable
adb forward tcp:8088 tcp:8088
```

---

## Technical Stack & Architecture

### High-Level Architecture

```
┌────────────────────────────────┐         USB (ADB Port Forward)         ┌────────────────────────────────┐
│      vcam-android (Phone)      │       or Low-Latency Wi-Fi (TCP)       │        vcam-mac (Mac)          │
│                                │ ──────────────────────────────────────► │                                │
│ Camera2 / CameraX (Surface)    │          Binary Stream (NALUs)         │ Network Receiver (NWConnection)│
│       │                        │                                        │       │                        │
│ MediaCodec (HW H.264 Encoder)  │                                        │ VideoToolbox (HW Decoder)      │
│       │                        │                                        │       ├──────────────────────┐ │
│ Socket Server (Port 8088)      │                                        │       ▼                      ▼ │
└────────────────────────────────┘                                        │ SwiftUI Preview    CMIOExtension│
                                                                          │ (HUD Window)     (System WebCam)│
                                                                          └────────────────────────────────┘
```

### Core Technologies

#### macOS (`vcam-mac`)
- **Language**: Swift (Swift 6 concurrency model)
- **UI Framework**: SwiftUI (macOS 14+ / 15+) with AppKit interop (`NSViewRepresentable`, `NSWindow`)
- **Video Decoding Engine**: `VideoToolbox` (`VTDecompressionSession`), `CoreMedia`, `CoreVideo` (`CVPixelBuffer`)
- **Virtual Camera**: `CoreMediaIO` (`CMIOExtension`, `CMIOExtensionProvider`, `CMIOExtensionStream`)
- **Network Engine**: Apple `Network.framework` (`NWConnection`, `NWListener`) for zero-overhead socket I/O
- **State Management**: Swift Observation framework (`@Observable`)

#### Android (`vcam-android`)
- **Language**: Kotlin 2.x (Coroutines, Flow)
- **UI Framework**: Jetpack Compose + Material 3
- **Camera Pipeline**: Camera2 / CameraX with direct `Surface` attachment
- **Video Encoding Engine**: `MediaCodec` (H.264 / AVC hardware encoder, baseline profile, 0-latency mode)
- **Network Engine**: Java/Kotlin NIO Non-blocking Sockets or Coroutine Socket Engine
- **Background Persistence**: Foreground Service with wake lock and black-screen battery saver mode

---

## Project Structure

```
vcam/
├── AGENTS.md                    # Agent development instructions and rules
├── docs/
│   └── protocol.md              # Binary framing & packet specification
├── vcam-android/                # Android Application
│   ├── app/
│   │   ├── build.gradle.kts
│   │   └── src/main/
│   │       ├── AndroidManifest.xml
│   │       └── java/com/pixeleye/vcam/
│   │           ├── MainActivity.kt          # Compose UI entry point
│   │           ├── camera/
│   │           │   ├── CameraManager.kt     # CameraX/Camera2 controller (FPS, exposure, lens)
│   │           │   └── CameraConfig.kt      # Resolution, FPS, facing mode
│   │           ├── encoder/
│   │           │   ├── VideoEncoder.kt      # MediaCodec H.264 hardware encoder
│   │           │   └── EncoderConfig.kt     # Bitrate, keyframe interval, latency flags
│   │           ├── net/
│   │           │   ├── StreamServer.kt      # TCP server streaming H.264 NALUs
│   │           │   └── PacketFraming.kt     # Binary protocol packetizer
│   │           ├── service/
│   │           │   └── VCamService.kt       # Foreground service for persistent streaming
│   │           └── ui/
│   │               ├── MainScreen.kt        # Camera preview & streaming controls
│   │               └── theme/               # Color, Theme, Typography
│   ├── build.gradle.kts
│   └── settings.gradle.kts
└── vcam-mac/                    # macOS Client & CMIO Virtual Camera
    ├── Package.swift            # SPM Package definition
    └── Sources/
        ├── VCamApp/             # Native macOS Application
        │   ├── VCamApp.swift    # @main SwiftUI Entry Point
        │   ├── AppDelegate.swift# NSApplicationDelegate
        │   ├── ViewModels/
        │   │   └── PlayerViewModel.swift    # UI state, latency counter, connection status
        │   └── Views/
        │       ├── MainWindowView.swift     # Root layout with floating HUD
        │       ├── VideoSurfaceView.swift   # Metal / CVPixelBuffer rendering surface
        │       └── Controls/
        │           ├── ControlOverlayView.swift # Translucent floating HUD
        │           ├── StatsOverlayView.swift   # Monospaced latency & FPS stats
        │           └── SettingsSheet.swift      # Resolution, bitrate, and source selection
        ├── VCamCore/            # Core decoding & network libraries
        │   ├── Network/
        │   │   ├── StreamReceiver.kt/swift  # NWConnection socket client
        │   │   └── PacketParser.swift       # Demuxes packet framing & NALUs
        │   ├── Decoder/
        │   │   └── VideoDecoder.swift       # VideoToolbox VTDecompressionSession
        │   └── Models/
        │       └── VideoFrame.swift         # CVPixelBuffer, PTS, frame dimensions
        └── VCamExtension/       # macOS CoreMediaIO Virtual Camera Extension
            ├── ExtensionProvider.swift      # CMIOExtensionProvider entry point
            ├── ExtensionDevice.swift        # Virtual Camera device descriptor
            └── ExtensionStream.swift        # CMIOExtensionStream pushing CMSampleBuffers
```

---

## Low-Latency Pipeline Guidelines ("උපරිම ෆාස්ට්" / Zero-Lag Principles)

To achieve the fastest possible streaming with sub-30ms glass-to-glass latency:

### 1. Android Zero-Copy Camera-to-Encoder Pipeline
- **Surface Input**: Do NOT read preview byte buffers or YUV image arrays into JVM memory. Attach the `MediaCodec` input surface (`encoder.createInputSurface()`) directly to the Camera2 / CameraX output.
- **Hardware Encoder Tuning**:
  - Codec: H.264 (`MediaFormat.MIMETYPE_VIDEO_AVC`).
  - Profile: Baseline Profile (`AVCProfileBaseline`) or Main Profile for fastest decoding without B-frames.
  - Latency Mode: Set `KEY_LATENCY = 0` and `KEY_PRIORITY = 0` (Realtime).
  - Rate Control: `BITRATE_MODE_CBR` (Constant Bitrate) with dynamic bitrate adaptation.
  - Keyframe Interval: 1 second (`KEY_I_FRAME_INTERVAL = 1`) or use periodic intra-refresh (`KEY_INTRA_REFRESH_PERIOD`) to ensure instant recovery without transmission lag.

### 2. Transport Protocol (`docs/protocol.md`)
- **Binary Header Framing**:
  - `Magic` (4 bytes): `0x5643414D` (`VCAM`)
  - `PacketType` (1 byte): `0x01` (Config: SPS/PPS), `0x02` (Video NALU: I/P-frame), `0x03` (Ping/Pong), `0x04` (Command/Control)
  - `PTS` (8 bytes): Presentation Timestamp in microseconds (`Long`)
  - `PayloadSize` (4 bytes): Length of payload in bytes (`UInt32`)
  - `Payload`: Raw Annex-B or AVCC NAL units.
- **Minimal Buffering**: Socket send/receive buffer sizes must be tuned to allow immediate flushing (`TCP_NODELAY = true`). Late frames must be dropped rather than queued.

### 3. macOS Hardware Decoding (`VideoToolbox`)
- Feed NALUs directly into a `VTDecompressionSession` configured for real-time operation (`kVTDecompressionPropertyKey_RealTime = true`).
- Decode directly to `kCVPixelFormatType_420YpCbCr8BiPlanarVideoRange` or `kCVPixelFormatType_32BGRA` for zero-conversion pass-through to `CMIOExtension` and Metal views.

### 4. Virtual Camera Extension (`CMIOExtension`)
- Wrap decoded `CVPixelBuffer`s into `CMSampleBuffer`s using `CMVideoFormatDescription`.
- Feed the buffers directly to the `CMIOExtensionStream` so any macOS app (Zoom, Meet, FaceTime) sees the virtual camera in native real-time.

---

## Strict Minimalist UI & Native macOS Aesthetics Policy

> [!IMPORTANT]
> **Video-First & Minimalist Design Guidelines for `vcam-mac`**:
> - **Video-First Presentation**: The camera stream must be the absolute focal point. Avoid heavy window frames, thick title bars, cards, sidebars, or bloated chrome.
> - **Floating Translucent HUD**:
>   - Controls must float over the video using native macOS materials (`.ultraThinMaterial` / subtle dark blur) with smooth rounded corners.
>   - Strictly avoid opaque gray backgrounds, heavy borders, or stark dropshadows.
> - **Auto-Hiding Controls & Cursor**:
>   - Controls HUD must smoothly fade out after 2–3 seconds of mouse inactivity during playback.
>   - Hide the macOS cursor (`NSCursor.hide()`) when controls fade out; restore cursor and HUD immediately on mouse movement.
> - **Native Iconography (SF Symbols Only)**:
>   - Exclusively use native Apple **SF Symbols** (`camera.fill`, `video.fill`, `bolt.horizontal.fill`, `antenna.radiowaves.left.and.right`, `gearshape.fill`, `arrow.triangle.2.circlepath`, etc.) with consistent sizing and weight (`.medium` or `.semibold`).
> - **Clean Scrubber & Typography**:
>   - Use monospaced digits (`.monospacedDigit()`) for all latency (e.g., `18 ms`), FPS (e.g., `60 FPS`), bitrate (e.g., `12.5 Mbps`), and resolution (`1080p`) readouts to eliminate jitter and layout shifts.
> - **No Visual Clutter**:
>   - Strictly forbid decorative badges, pills with heavy colored backgrounds, unnecessary cards, or generic gradient backgrounds. Keep interactions crisp, subtle, and native to macOS.

---

## Keyboard Shortcuts (macOS Client)

| Key | Action |
| :--- | :--- |
| `Space` | Pause / Resume streaming feed |
| `F` | Toggle Fullscreen |
| `M` | Mute / Unmute microphone stream (if audio enabled) |
| `C` | Cycle Android camera lens (Back / Ultra-wide / Front) |
| `R` | Reconnect / Restart stream |
| `I` | Toggle detailed latency & stream stats HUD |

---

## Agent Guidelines for Code Modification

1. **Verification**:
   - For macOS changes: Run `swift build` in `vcam-mac` to verify there are no compiler warnings or syntax errors.
   - For Android changes: Run `./gradlew assembleDebug` in `vcam-android` to ensure clean builds.
2. **No Xcode or Android Studio GUI Recommendations**: Never prompt the user to open Xcode or Android Studio. Always provide exact CLI commands (`swift`, `./gradlew`, `adb`).
3. **Native APIs First**:
   - On macOS: Prioritize built-in Apple frameworks (`SwiftUI`, `VideoToolbox`, `CoreMediaIO`, `Network`, `Metal`).
   - On Android: Prioritize built-in Android APIs (`Camera2`, `CameraX`, `MediaCodec`, `kotlinx.coroutines`). Avoid bulky third-party streaming libraries unless explicitly requested.
4. **Swift Concurrency**: Isolate all UI state changes to `@MainActor`. Use Swift 6 async/await, `Task`, and `AsyncStream` patterns.
