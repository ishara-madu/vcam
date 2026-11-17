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
        HStack(spacing: 8) {
            // Play / Pause
            IconButton(
                systemName: isPaused ? "play.fill" : "pause.fill",
                helpText: isPaused ? "Resume (Space)" : "Pause (Space)",
                isActive: isPaused,
                action: onPlayPause
            )

            // Reconnect
            IconButton(
                systemName: "arrow.triangle.2.circlepath",
                helpText: "Reconnect Stream (R)",
                action: onReconnect
            )

            HairlineDivider()

            // Lens Switcher Button
            Button(action: onCycleLens) {
                HStack(spacing: 5) {
                    Image(systemName: "camera")
                        .font(.system(size: 11, weight: .semibold))
                    Text(currentLens)
                        .font(.system(size: 11, weight: .medium, design: .rounded))
                }
                .foregroundColor(.white.opacity(0.85))
                .padding(.horizontal, 10)
                .frame(height: 28)
                .background(Color.white.opacity(0.06))
                .clipShape(Capsule())
            }
            .buttonStyle(.plain)
            .help("Switch Lens (C)")

            HairlineDivider()

            // Toggle Telemetry Stats
            IconButton(
                systemName: "gauge.with.needle",
                helpText: "Toggle Stats (I)",
                isActive: showStats,
                action: onToggleStats
            )

            // Settings Sheet
            IconButton(
                systemName: "slider.horizontal.3",
                helpText: "Settings",
                action: onOpenSettings
            )

            // Fullscreen
            IconButton(
                systemName: "arrow.up.left.and.arrow.down.right",
                helpText: "Fullscreen (F)",
                action: onToggleFullscreen
            )
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 6)
        .background(.ultraThinMaterial)
        .clipShape(Capsule())
        .overlay(
            Capsule()
                .stroke(Color.white.opacity(0.1), lineWidth: 0.75)
        )
        .shadow(color: .black.opacity(0.25), radius: 10, y: 4)
    }
}

private struct IconButton: View {
    let systemName: String
    let helpText: String
    var isActive: Bool = false
    let action: () -> Void

    @State private var isHovered = false

    var body: some View {
        Button(action: action) {
            Image(systemName: systemName)
                .font(.system(size: 12, weight: .medium))
                .foregroundColor(isActive ? .cyan : (isHovered ? .white : .white.opacity(0.75)))
                .frame(width: 28, height: 28)
                .background(isHovered ? Color.white.opacity(0.08) : Color.clear)
                .clipShape(Circle())
        }
        .buttonStyle(.plain)
        .help(helpText)
        .onHover { isHovered = $0 }
    }
}

private struct HairlineDivider: View {
    var body: some View {
        Rectangle()
            .fill(Color.white.opacity(0.12))
            .frame(width: 1, height: 14)
    }
}
