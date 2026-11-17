import SwiftUI
import VCamCore

public struct ControlOverlayView: View {
    public let isPaused: Bool
    public let currentLens: String
    public let showStats: Bool
    public let connectionState: StreamConnectionState
    public let onPlayPause: () -> Void
    public let onReconnect: () -> Void
    public let onCycleLens: () -> Void
    public let onToggleStats: () -> Void
    public let onOpenSettings: () -> Void
    public let onToggleFullscreen: () -> Void

    public init(
        isPaused: Bool,
        currentLens: String,
        showStats: Bool,
        connectionState: StreamConnectionState,
        onPlayPause: @escaping () -> Void,
        onReconnect: @escaping () -> Void,
        onCycleLens: @escaping () -> Void,
        onToggleStats: @escaping () -> Void,
        onOpenSettings: @escaping () -> Void,
        onToggleFullscreen: @escaping () -> Void
    ) {
        self.isPaused = isPaused
        self.currentLens = currentLens
        self.showStats = showStats
        self.connectionState = connectionState
        self.onPlayPause = onPlayPause
        self.onReconnect = onReconnect
        self.onCycleLens = onCycleLens
        self.onToggleStats = onToggleStats
        self.onOpenSettings = onOpenSettings
        self.onToggleFullscreen = onToggleFullscreen
    }

    public var body: some View {
        HStack(spacing: 12) {
            // Play / Pause Stream
            Button(action: onPlayPause) {
                Image(systemName: isPaused ? "play.fill" : "pause.fill")
                    .font(.system(size: 15, weight: .semibold))
                    .frame(width: 36, height: 36)
                    .foregroundColor(.white)
            }
            .buttonStyle(.plain)
            .help(isPaused ? "Resume Feed (Space)" : "Pause Feed (Space)")

            // Reconnect
            Button(action: onReconnect) {
                Image(systemName: "arrow.triangle.2.circlepath")
                    .font(.system(size: 14, weight: .semibold))
                    .frame(width: 36, height: 36)
                    .foregroundColor(.white)
            }
            .buttonStyle(.plain)
            .help("Reconnect Stream (R)")

            Divider()
                .frame(height: 20)
                .background(Color.white.opacity(0.2))

            // Lens Switcher Button with text
            Button(action: onCycleLens) {
                HStack(spacing: 6) {
                    Image(systemName: "camera.fill")
                        .font(.system(size: 13, weight: .semibold))
                    Text(currentLens)
                        .font(.system(size: 12, weight: .medium))
                }
                .padding(.horizontal, 10)
                .frame(height: 36)
                .foregroundColor(.white)
            }
            .buttonStyle(.plain)
            .help("Cycle Camera Lens (C)")

            Divider()
                .frame(height: 20)
                .background(Color.white.opacity(0.2))

            // Toggle Telemetry Stats
            Button(action: onToggleStats) {
                Image(systemName: showStats ? "chart.bar.xaxis" : "chart.bar")
                    .font(.system(size: 14, weight: .semibold))
                    .frame(width: 36, height: 36)
                    .foregroundColor(showStats ? .cyan : .white)
            }
            .buttonStyle(.plain)
            .help("Toggle Stats HUD (I)")

            // Settings Sheet
            Button(action: onOpenSettings) {
                Image(systemName: "gearshape.fill")
                    .font(.system(size: 14, weight: .semibold))
                    .frame(width: 36, height: 36)
                    .foregroundColor(.white)
            }
            .buttonStyle(.plain)
            .help("Stream & Virtual Camera Settings")

            // Fullscreen
            Button(action: onToggleFullscreen) {
                Image(systemName: "arrow.up.left.and.arrow.down.right")
                    .font(.system(size: 13, weight: .semibold))
                    .frame(width: 36, height: 36)
                    .foregroundColor(.white)
            }
            .buttonStyle(.plain)
            .help("Toggle Fullscreen (F)")
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
        .background(.ultraThinMaterial)
        .clipShape(Capsule())
        .overlay(
            Capsule()
                .stroke(Color.white.opacity(0.12), lineWidth: 1)
        )
        .shadow(color: .black.opacity(0.4), radius: 12, y: 6)
    }
}
