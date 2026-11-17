import Foundation
import SwiftUI
import Observation
import CoreVideo
import VCamCore

@Observable
@MainActor
public final class PlayerViewModel {
    public var connectionState: StreamConnectionState = .disconnected
    public var connectionHost: String = "127.0.0.1"
    public var connectionPort: UInt16 = 8088
    public var isUSBMode: Bool = true

    // Telemetry stats
    public var fps: Double = 60.0
    public var latencyMs: Double = 18.5
    public var bitrateMbps: Double = 12.0
    public var resolutionText: String = "1080p (1920×1080)"
    public var droppedFrames: Int = 0

    // Virtual Camera state
    public var isVirtualCameraActive: Bool = true
    public var virtualCameraStatus: VirtualCameraStatus = .active

    // HUD & Auto-hide
    public var isHudVisible: Bool = true
    public var showStatsOverlay: Bool = true
    public var showSettingsSheet: Bool = false
    public var isPaused: Bool = false
    public var currentLens: String = "1.0x (Wide)"

    // Decoded frame for rendering
    public var currentPixelBuffer: CVPixelBuffer?

    private var autoHideTask: Task<Void, Never>?
    private let receiver = StreamReceiver()

    public init() {
        setupReceiver()
    }

    private func setupReceiver() {
        receiver.onStateChange = { [weak self] state in
            Task { @MainActor in
                self?.connectionState = state
                if state == .connected {
                    self?.virtualCameraStatus = .streaming
                } else {
                    self?.virtualCameraStatus = .active
                }
            }
        }

        receiver.onFrameReceived = { [weak self] frame in
            Task { @MainActor in
                guard let self = self, !self.isPaused else { return }
                self.currentPixelBuffer = frame.pixelBuffer
                self.resolutionText = "\(frame.width)×\(frame.height)"
            }
        }

        receiver.onStatsUpdate = { [weak self] fps, latency, bytesSec in
            Task { @MainActor in
                guard let self = self else { return }
                self.fps = fps
                self.latencyMs = latency
                self.bitrateMbps = (Double(bytesSec) * 8.0) / 1_000_000.0
            }
        }
    }

    public func connect() {
        let host = isUSBMode ? "127.0.0.1" : connectionHost
        receiver.connect(host: host, port: connectionPort)
    }

    public func disconnect() {
        receiver.disconnect()
    }

    public func togglePlayPause() {
        isPaused.toggle()
    }

    public func cycleCameraLens() {
        let lenses = ["0.5x (Ultra-wide)", "1.0x (Wide)", "2.0x (Telephoto)", "Front Camera"]
        if let idx = lenses.firstIndex(of: currentLens) {
            currentLens = lenses[(idx + 1) % lenses.count]
        } else {
            currentLens = "1.0x (Wide)"
        }
    }

    public func triggerMouseMove() {
        isHudVisible = true
        NSCursor.unhide()

        autoHideTask?.cancel()
        autoHideTask = Task {
            try? await Task.sleep(nanoseconds: 3_000_000_000) // 3 seconds
            if !Task.isCancelled {
                self.isHudVisible = false
                NSCursor.setHiddenUntilMouseMoves(true)
            }
        }
    }
}
