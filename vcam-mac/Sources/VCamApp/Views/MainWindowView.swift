import SwiftUI
import VCamCore

public struct MainWindowView: View {
    @State private var viewModel = PlayerViewModel()

    public init() {}

    public var body: some View {
        ZStack {
            // Full-bleed Edge-to-Edge Video Surface
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

            // Floating Minimalist Overlays
            VStack {
                // Top Floating Status Bar
                HStack(alignment: .top) {
                    if viewModel.showStatsOverlay && viewModel.isHudVisible {
                        StatsOverlayView(
                            fps: viewModel.fps,
                            latencyMs: viewModel.latencyMs,
                            bitrateMbps: viewModel.bitrateMbps,
                            resolution: viewModel.resolutionText,
                            virtualCameraActive: viewModel.isVirtualCameraActive
                        )
                        .transition(.opacity.combined(with: .scale(scale: 0.98)))
                    }

                    Spacer()

                    if viewModel.isHudVisible {
                        // Minimalist Connection Status Badge
                        HStack(spacing: 6) {
                            Circle()
                                .fill(connectionIndicatorColor)
                                .frame(width: 6, height: 6)

                            Text(viewModel.connectionState == .connected ? (viewModel.isUSBMode ? "USB" : "Wi-Fi") : viewModel.connectionState.rawValue)
                                .font(.system(size: 10, weight: .semibold, design: .monospaced))
                                .foregroundColor(.white.opacity(0.85))
                        }
                        .padding(.horizontal, 10)
                        .padding(.vertical, 5)
                        .background(.ultraThinMaterial)
                        .clipShape(Capsule())
                        .overlay(
                            Capsule().stroke(Color.white.opacity(0.1), lineWidth: 0.75)
                        )
                        .transition(.opacity)
                    }
                }
                .padding(16)

                Spacer()

                // Bottom Minimalist Floating Control Capsule
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
                    .padding(.bottom, 20)
                    .transition(.opacity.combined(with: .move(edge: .bottom)))
                }
            }
            .animation(.easeInOut(duration: 0.2), value: viewModel.isHudVisible)
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
        // Minimalist Key Handlers
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
            return Color(red: 0.2, green: 0.85, blue: 0.4)
        case .connecting:
            return .orange
        case .disconnected:
            return .gray.opacity(0.6)
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
