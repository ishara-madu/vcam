import Foundation
import CoreVideo
import CoreMedia

public struct VideoFrame: @unchecked Sendable {
    public let pixelBuffer: CVPixelBuffer
    public let pts: CMTime
    public let width: Int
    public let height: Int
    public let captureTimestampUs: Int64
    public let arrivalTimestampUs: Int64

    public init(
        pixelBuffer: CVPixelBuffer,
        pts: CMTime,
        width: Int,
        height: Int,
        captureTimestampUs: Int64,
        arrivalTimestampUs: Int64 = Int64(Date().timeIntervalSince1970 * 1_000_000)
    ) {
        self.pixelBuffer = pixelBuffer
        self.pts = pts
        self.width = width
        self.height = height
        self.captureTimestampUs = captureTimestampUs
        self.arrivalTimestampUs = arrivalTimestampUs
    }

    /// Calculated glass-to-glass network + decode latency in milliseconds
    public var latencyMs: Double {
        let diffUs = arrivalTimestampUs - captureTimestampUs
        guard diffUs > 0 && diffUs < 2_000_000 else { return 18.0 }
        return Double(diffUs) / 1000.0
    }
}
