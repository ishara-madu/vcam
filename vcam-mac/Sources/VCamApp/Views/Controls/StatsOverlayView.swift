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
        VStack(alignment: .leading, spacing: 10) {
            // Virtual Camera System Status Header
            HStack(spacing: 8) {
                Circle()
                    .fill(virtualCameraActive ? Color.green : Color.orange)
                    .frame(width: 8, height: 8)

                Text(virtualCameraActive ? "Virtual Camera System-Wide Active" : "Virtual Camera Standby")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundColor(.white.opacity(0.9))

                Spacer()

                Text("CoreMediaIO")
                    .font(.system(size: 10, weight: .semibold, design: .monospaced))
                    .padding(.horizontal, 6)
                    .padding(.vertical, 2)
                    .background(Color.white.opacity(0.1))
                    .clipShape(Capsule())
                    .foregroundColor(.cyan)
            }

            Divider()
                .background(Color.white.opacity(0.15))

            // Metrics Grid
            HStack(spacing: 16) {
                // Latency
                VStack(alignment: .leading, spacing: 2) {
                    Text("LATENCY")
                        .font(.system(size: 9, weight: .bold))
                        .foregroundColor(.white.opacity(0.45))
                    HStack(alignment: .firstTextBaseline, spacing: 2) {
                        Text(String(format: "%.1f", latencyMs))
                            .font(.system(size: 14, weight: .bold, design: .monospaced))
                            .foregroundColor(.green)
                            .monospacedDigit()
                        Text("ms")
                            .font(.system(size: 10, weight: .medium))
                            .foregroundColor(.white.opacity(0.6))
                    }
                }

                // FPS
                VStack(alignment: .leading, spacing: 2) {
                    Text("FRAMERATE")
                        .font(.system(size: 9, weight: .bold))
                        .foregroundColor(.white.opacity(0.45))
                    HStack(alignment: .firstTextBaseline, spacing: 2) {
                        Text(String(format: "%.0f", fps))
                            .font(.system(size: 14, weight: .bold, design: .monospaced))
                            .foregroundColor(.cyan)
                            .monospacedDigit()
                        Text("FPS")
                            .font(.system(size: 10, weight: .medium))
                            .foregroundColor(.white.opacity(0.6))
                    }
                }

                // Bitrate
                VStack(alignment: .leading, spacing: 2) {
                    Text("BITRATE")
                        .font(.system(size: 9, weight: .bold))
                        .foregroundColor(.white.opacity(0.45))
                    HStack(alignment: .firstTextBaseline, spacing: 2) {
                        Text(String(format: "%.1f", bitrateMbps))
                            .font(.system(size: 14, weight: .bold, design: .monospaced))
                            .foregroundColor(.white)
                            .monospacedDigit()
                        Text("Mbps")
                            .font(.system(size: 10, weight: .medium))
                            .foregroundColor(.white.opacity(0.6))
                    }
                }

                // Resolution
                VStack(alignment: .leading, spacing: 2) {
                    Text("RESOLUTION")
                        .font(.system(size: 9, weight: .bold))
                        .foregroundColor(.white.opacity(0.45))
                    Text(resolution)
                        .font(.system(size: 12, weight: .bold, design: .monospaced))
                        .foregroundColor(.white.opacity(0.85))
                        .monospacedDigit()
                }
            }
        }
        .padding(14)
        .background(.ultraThinMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(Color.white.opacity(0.12), lineWidth: 1)
        )
        .shadow(color: .black.opacity(0.35), radius: 10, y: 5)
    }
}
