# MMPPlayer - macOS Video Player Development Guidelines

## Project Overview

**MMPPlayer** is a native macOS desktop video player application built using **Swift**, **SwiftUI**, and **AVFoundation**.

> [!IMPORTANT]
> **Strict Command-Line (CLI) Only Policy**:
> - **No Xcode GUI**: Do NOT use or suggest using the Xcode application GUI. The entire project lifecycle (creating files, compiling, testing, running, and bundling) is executed strictly via the terminal using the `swift` command-line tools.
> - **No `.xcodeproj` Dependency**: Manage all dependencies, targets, and compilation via **Swift Package Manager (`Package.swift`)**.

---

## Command-Line Toolchain & Commands

Always use the Swift CLI toolchain for building, running, and managing the project:

| Task | Command | Description |
| :--- | :--- | :--- |
| **Run App** | `swift run` | Compiles and immediately launches the player window for testing |
| **Build Debug** | `swift build` | Compiles debug binaries |
| **Build Release** | `swift build -c release` | Compiles optimized release binaries |
| **Run Tests** | `swift test` | Executes unit and integration test suites |
| **Clean Build** | `swift package clean` | Cleans build artifacts (`.build/` directory) |
| **Resolve Dependencies** | `swift package resolve` | Resolves SPM packages listed in `Package.swift` |
| **Update Dependencies** | `swift package update` | Updates packages to the latest compatible versions |

### Creating a Standalone macOS `.app` Bundle (CLI Only)
To launch with full macOS app permissions (Dock icon, menu bar, drag-and-drop, sandbox/file access) without Xcode GUI, assemble a `.app` bundle from the CLI:

```bash
# Example bundling steps via terminal
mkdir -p build/MMPPlayer.app/Contents/{MacOS,Resources}
cp $(swift build -c release --show-bin-path)/MMPPlayer build/MMPPlayer.app/Contents/MacOS/
cp Resources/Info.plist build/MMPPlayer.app/Contents/
```

---

## Technical Stack & Architecture

### Core Technologies
- **Language**: Swift (Swift 6 concurrency model)
- **UI Framework**: SwiftUI (macOS 14+ / 15+) with AppKit interop (`NSViewRepresentable`, `NSOpenPanel`) where necessary
- **Playback Engine**: `AVFoundation` (`AVPlayer`, `AVPlayerItem`, `AVAsset`) & `AVKit` (`AVPlayerView`)
- **State Management**: Swift Observation framework (`@Observable`) or modern Combine/AsyncStreams

### Architecture (MVVM + Engine Service)
Keep UI components completely decoupled from AVFoundation playback logic:

```
Sources/MMPPlayer/
├── App/
│   ├── MMPPlayerApp.swift       # Application entry point (@main, WindowGroup)
│   └── AppDelegate.swift        # NSApplicationDelegate (menu items, file drop handlers)
├── Player/
│   ├── PlayerEngine.swift       # AVPlayer wrapper (play, pause, seek, rate, volume)
│   ├── TimeObserver.swift       # Periodic CMTime observation for timeline updates
│   └── MediaItem.swift          # Media metadata, video URL, duration, resolution
├── ViewModels/
│   └── PlayerViewModel.swift    # @Observable presentation logic, control states, UI binding
├── Views/
│   ├── MainWindowView.swift     # Root player layout
│   ├── VideoSurfaceView.swift   # NSViewRepresentable wrapping AVPlayerView or AVPlayerLayer
│   ├── Controls/
│   │   ├── ControlOverlayView.swift  # Auto-hiding HUD overlay
│   │   ├── ScrubberBarView.swift     # Custom timeline slider with preview/hover
│   │   ├── PlaybackButtons.swift     # Play/Pause, Step, Next/Previous
│   │   └── VolumeSliderView.swift    # Volume & Mute control
│   └── DropZoneView.swift       # Drag-and-drop visual indicator
└── Utilities/
    ├── TimeFormatting.swift     # Formats CMTime / Seconds to HH:MM:SS
    └── KeyboardShortcuts.swift  # Key event handlers
```

---

## Desktop Video Player Requirements & Best Practices

### 1. Playback Engine (`AVFoundation`)
- Handle asynchronous asset loading (`load(.duration, .tracks)` using Swift concurrency `await`).
- Support precise timeline seeking (`seek(to:toleranceBefore:toleranceAfter:)`).
- Observe playback status (`AVPlayerItem.Status`), buffering progress, and end-of-playback notifications.
- Ensure audio session and hardware acceleration are utilized properly.

### 2. Modern Native macOS UX
- **Auto-Hiding Controls**: Controls HUD must fade out after 2–3 seconds of cursor inactivity during playback and appear instantly on mouse movement.
- **Keyboard Shortcuts**:
  - `Space`: Toggle Play / Pause
  - `Left Arrow` / `Right Arrow`: Seek backward / forward (e.g., 5 seconds)
  - `Up Arrow` / `Down Arrow`: Volume up / down
  - `F`: Toggle Fullscreen
  - `M`: Toggle Mute
  - `[` / `]`: Decrease / increase playback speed (0.5x, 1.0x, 1.25x, 1.5x, 2.0x)
- **Drag & Drop**: Allow dragging video files (`.mp4`, `.mov`, `.m4v`, `.mkv`, etc.) directly into the player window to start playing.
- **Aspect Ratio & Window Resizing**: Automatically adjust or preserve aspect ratio according to video tracks.

### 3. Strict Minimalist UI & Native macOS Aesthetics Policy
> [!IMPORTANT]
> **Video-First & Minimalist Design Guidelines**:
> - **Video-First Presentation**: The video content must be the absolute focal point. Avoid heavy window frames, thick title bars, cards, sidebars, or bloated chrome.
> - **Floating Translucent HUD**:
>   - Controls must float over the video using native macOS materials (`.ultraThinMaterial` / subtle dark blur) with smooth rounded corners.
>   - Strictly avoid opaque gray backgrounds, heavy borders, or stark dropshadows.
> - **Auto-Hiding Controls & Cursor**:
>   - Controls HUD must smoothly fade out after 2–3 seconds of mouse inactivity during playback.
>   - Hide the macOS cursor (`NSCursor.hide()`) when controls fade out; restore cursor and HUD immediately on mouse movement or pause.
> - **Native Iconography (SF Symbols Only)**:
>   - Exclusively use native Apple **SF Symbols** (`play.fill`, `pause.fill`, `speaker.wave.2.fill`, `arrow.up.left.and.arrow.down.right`, etc.) with consistent sizing and weight (`.medium` or `.semibold`).
> - **Clean Scrubber & Typography**:
>   - Use a sleek, thin timeline scrubber bar that expands slightly on hover, avoiding bulky thumb knobs.
>   - Use monospaced digits (`.monospacedDigit()`) for all time labels (`00:00 / 00:00`) to eliminate jitter and layout shifts during playback.
> - **No Visual Clutter**:
>   - Strictly forbid decorative badges, pills with heavy colored backgrounds, unnecessary cards, or generic gradient backgrounds. Keep interactions crisp, subtle, and native to macOS.

### 4. Swift Concurrency & Safety
- Isolate all UI state changes and ViewModel interactions to `@MainActor`.
- Use `Task` and `AsyncStream` or Swift Concurrency for observing `AVPlayer` time and status changes.
- Handle file access bookmarks (`URL.startAccessingSecurityScopedResource()`) properly when opening files via dialog or drag-and-drop on macOS.

---

## Agent Guidelines for Code Modification

1. **Verification**: Always run `swift build` after modifying or creating Swift source files to ensure no compiler warnings or syntax errors.
2. **No Xcode Recommendations**: Never prompt the user to open Xcode or configure schemes in Xcode. Provide exact terminal commands.
3. **Native Apple APIs**: Prioritize built-in Apple frameworks (`SwiftUI`, `AVFoundation`, `AppKit`, `UniformTypeIdentifiers`). Avoid adding unnecessary third-party SPM packages unless explicitly requested.
