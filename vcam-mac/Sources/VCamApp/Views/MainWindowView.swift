import SwiftUI
import VCamCore

public struct MainWindowView: View {
    @State private var viewModel = PlayerViewModel()

    public init() {}

    public var body: some View {
        ZStack {
            // Full-bleed Video Surface
            VideoSurfaceView(
                pixelBuffer: viewModel.currentPixelBuffer,
                isConnected: viewModel.connectionState == .connected
            )
            .contentShape(Rectangle())
            .onContinuousHover { phase in
                switch phase {
                case .active:
                    viewModel.triggerMouseMove()
                case .ended:
                    break
                }
            }

            // Top Status & Floating HUD Elements
            VStack {
                HStack(alignment: .top) {
                    // Top-Left: Telemetry & System Virtual Camera Stats
                    if viewModel.showStatsOverlay && viewModel.isHudVisible {
                        StatsOverlayView(
                            fps: viewModel.fps,
                            latencyMs: viewModel.latencyMs,
                            bitrateMbps: viewModel.bitrateMbps,
                            resolution: viewModel.resolutionText,
                            virtualCameraActive: viewModel.isVirtualCameraActive
                        )
                        .transition(.opacity.combined(with: .scale(scale: 0.96)))
                    }

                    Spacer()

                    // Top-Right: Connection Mode / State Pill
                    if viewModel.isHudVisible {
                        HStack(spacing: 8) {
                            Circle()
                                .fill(connectionIndicatorColor)
                                .frame(width: 8, height: 8)

                            Text(viewModel.connectionState.rawValue)
                                .font(.system(size: 11, weight: .semibold))
                                .foregroundColor(.white)

                            if viewModel.connectionState == .connected {
                                Text(viewModel.isUSBMode ? "USB" : "Wi-Fi")
                                    .font(.system(size: 10, weight: .bold, design: .monospaced))
                                    .padding(.horizontal, 6)
                                    .padding(.vertical, 2)
                                    .background(Color.white.opacity(0.15))
                                    .clipShape(Capsule())
                                    .foregroundColor(.cyan)
                            }
                        }
                        .padding(.horizontal, 12)
                        .padding(.vertical, 6)
                        .background(.ultraThinMaterial)
                        .clipShape(Capsule())
                        .overlay(
                            Capsule().stroke(Color.white.opacity(0.12), lineWidth: 1)
                        )
                        .transition(.opacity)
                    }
                }
                .padding(20)

                Spacer()

                // Bottom Floating Control HUD
                if viewModel.isHudVisible {
                    ControlOverlayView(
                        isPaused: viewModel.isPaused,
                        currentLens: viewModel.currentLens,
                        showStats: viewModel.showStatsOverlay,
                        connectionState: viewModel.connectionState,
                        onPlayPause: { viewModel.togglePlayPause() },
                        onReconnect: { viewModel.connect() },
                        onCycleLens: { viewModel.cycleCameraLens() },
                        onToggleStats: {
                            withAnimation(.easeInOut(duration: 0.2)) {
                                viewModel.showStatsOverlay.toggle()
                            }
                        },
                        onOpenSettings: { viewModel.showSettingsSheet = true },
                        onToggleFullscreen: { toggleWindowFullscreen() }
                    )
                    .padding(.bottom, 24)
                    .transition(.opacity.combined(with: .move(edge: .bottom)))
                }
            }
            .animation(.easeInOut(duration: 0.25), value: viewModel.isHudVisible)
        }
        .sheet(isPresented: $viewModel.showSettingsSheet) {
            SettingsSheet(
                isUSBMode: $viewModel.isUSBMode,
                host: $viewModel.connectionHost,
                port: $viewModel.connectionPort,
                isVirtualCameraActive: $viewModel.isVirtualCameraActive,
                onDismiss: { viewModel.showSettingsSheet = false }
            )
        }
        .onAppear {
            viewModel.connect()
            viewModel.triggerMouseMove()
        }
        // Keyboard Shortcuts
        .background(
            KeyHandlerView(
                onSpace: { viewModel.togglePlayPause() },
                onR: { viewModel.connect() },
                onC: { viewModel.cycleCameraLens() },
                onI: {
                    withAnimation {
                        viewModel.showStatsOverlay.toggle()
                    }
                },
                onF: { toggleWindowFullscreen() }
            )
        )
    }

    private var connectionIndicatorColor: Color {
        switch viewModel.connectionState {
        case .connected:
            return .green
        case .connecting:
            return .orange
        case .disconnected:
            return .gray
        case .failed:
            return .red
        }
    }

    private func toggleWindowFullscreen() {
        if let window = NSApplication.shared.windows.first {
            window.toggleFullScreen(nil)
        }
    }
}

// Transparent view catching key events
private struct KeyHandlerView: NSViewRepresentable {
    let onSpace: () -> Void
    let onR: () -> Void
    let onC: () -> Void
    let onI: () -> Void
    let onF: () -> Void

    func makeNSView(context: Context) -> KeyView {
        let view = KeyView()
        view.onSpace = onSpace
        view.onR = onR
        view.onC = onC
        view.onI = onI
        view.onF = onF
        return view
    }

    func updateNSView(_ nsView: KeyView, context: Context) {
        nsView.onSpace = onSpace
        nsView.onR = onR
        nsView.onC = onC
        nsView.onI = onI
        nsView.onF = onF
    }

    final class KeyView: NSView {
        var onSpace: (() -> Void)?
        var onR: (() -> Void)?
        var onC: (() -> Void)?
        var onI: (() -> Void)?
        var onF: (() -> Void)?

        override var acceptsFirstResponder: Bool { true }

        override func keyDown(with event: NSEvent) {
            switch event.charactersIgnoringModifiers?.lowercased() {
            case " ":
                onSpace?()
            case "r":
                onR?()
            case "c":
                onC?()
            case "i":
                onI?()
            case "f":
                onF?()
            default:
                super.keyDown(with: event)
            }
        }
    }
}
