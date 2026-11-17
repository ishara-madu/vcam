import SwiftUI

public struct StatsOverlayView: View {
    public let fps: Double
    public let latencyMs: Double
    public let bitrateMbps: Double
    public let resolution: String
    public let virtualCameraActive: Bool

    public init(
        fps: Double,
        latencyMs: Double,
        bitrateMbps: Double,
        resolution: String,
        virtualCameraActive: Bool
    ) {
        self.fps = fps
        self.latencyMs = latencyMs
        self.bitrateMbps = bitrateMbps
        self.resolution = resolution
        self.virtualCameraActive = virtualCameraActive
    }

    public var body: some View {
        HStack(spacing: 14) {
            // Virtual Camera Status Indicator Dot
            HStack(spacing: 6) {
                Circle()
                    .fill(virtualCameraActive ? Color(red: 0.2, green: 0.85, blue: 0.4) : Color.orange)
                    .frame(width: 7, height: 7)

                Text(virtualCameraActive ? "LIVE" : "STANDBY")
                    .font(.system(size: 10, weight: .bold, design: .monospaced))
                    .foregroundColor(virtualCameraActive ? Color(red: 0.2, green: 0.85, blue: 0.4) : .orange)
            }

            Rectangle()
                .fill(Color.white.opacity(0.12))
                .frame(width: 1, height: 12)

            // Latency
            HStack(spacing: 3) {
                Text(String(format: "%.1f", latencyMs))
                    .font(.system(size: 11, weight: .semibold, design: .monospaced))
                    .foregroundColor(.white)
                    .monospacedDigit()
                Text("ms")
                    .font(.system(size: 9, weight: .medium))
                    .foregroundColor(.white.opacity(0.45))
            }

            // FPS
            HStack(spacing: 3) {
                Text(String(format: "%.0f", fps))
                    .font(.system(size: 11, weight: .semibold, design: .monospaced))
                    .foregroundColor(.white)
                    .monospacedDigit()
                Text("fps")
                    .font(.system(size: 9, weight: .medium))
                    .foregroundColor(.white.opacity(0.45))
            }

            // Bitrate
            HStack(spacing: 3) {
                Text(String(format: "%.1f", bitrateMbps))
                    .font(.system(size: 11, weight: .semibold, design: .monospaced))
                    .foregroundColor(.white)
                    .monospacedDigit()
                Text("Mb/s")
                    .font(.system(size: 9, weight: .medium))
                    .foregroundColor(.white.opacity(0.45))
            }

            Rectangle()
                .fill(Color.white.opacity(0.12))
                .frame(width: 1, height: 12)

            // Resolution
            Text(resolution)
                .font(.system(size: 10, weight: .medium, design: .monospaced))
                .foregroundColor(.white.opacity(0.65))
                .monospacedDigit()
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 7)
        .background(.ultraThinMaterial)
        .clipShape(Capsule())
        .overlay(
            Capsule()
                .stroke(Color.white.opacity(0.1), lineWidth: 0.75)
        )
        .shadow(color: .black.opacity(0.2), radius: 8, y: 3)
    }
}
