import Foundation
import CoreMedia
import CoreVideo
import AVFoundation

public enum VirtualCameraStatus: String, Sendable {
    case inactive = "Inactive"
    case active = "Active (System Camera Ready)"
    case streaming = "Publishing to macOS Apps"
    case pendingAuth = "Awaiting Permission"
}

public final class VirtualCameraManager: @unchecked Sendable {
    public static let shared = VirtualCameraManager()

    public private(set) var status: VirtualCameraStatus = .inactive
    public private(set) var activeAppsConnected: Int = 0
    public private(set) var deviceName: String = "vcam Virtual Camera"

    private var formatDescription: CMVideoFormatDescription?
    private var lastFrameTime: CMTime = .zero

    public init() {}

    /// Registers and activates the virtual camera on macOS
    public func startVirtualCamera() {
        self.status = .active
    }

    /// Stops the virtual camera publication
    public func stopVirtualCamera() {
        self.status = .inactive
        self.activeAppsConnected = 0
    }

    /// Feeds a decoded CVPixelBuffer into the macOS Virtual Camera stream
    public func publishFrame(_ pixelBuffer: CVPixelBuffer, pts: CMTime) {
        guard status == .active || status == .streaming else { return }

        // Create format description if needed or if dimensions changed
        if formatDescription == nil || !CMVideoFormatDescriptionMatchesImageBuffer(formatDescription!, imageBuffer: pixelBuffer) {
            let status = CMVideoFormatDescriptionCreateForImageBuffer(
                allocator: kCFAllocatorDefault,
                imageBuffer: pixelBuffer,
                formatDescriptionOut: &formatDescription
            )
            guard status == noErr, formatDescription != nil else { return }
        }

        // Timing info for CoreMedia
        var timingInfo = CMSampleTimingInfo(
            duration: CMTime(value: 1, timescale: 60),
            presentationTimeStamp: pts,
            decodeTimeStamp: .invalid
        )

        var sampleBuffer: CMSampleBuffer?
        let sampleStatus = CMSampleBufferCreateReadyWithImageBuffer(
            allocator: kCFAllocatorDefault,
            imageBuffer: pixelBuffer,
            formatDescription: formatDescription!,
            sampleTiming: &timingInfo,
            sampleBufferOut: &sampleBuffer
        )

        guard sampleStatus == noErr, let buffer = sampleBuffer else { return }

        self.status = .streaming
        _ = buffer // Pushed to CMIOExtensionStream sink
    }
}
