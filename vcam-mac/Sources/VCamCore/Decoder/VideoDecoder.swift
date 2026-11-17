import Foundation
import VideoToolbox
import CoreMedia
import CoreVideo

public final class VideoDecoder: @unchecked Sendable {
    private var decompressionSession: VTDecompressionSession?
    private var formatDescription: CMVideoFormatDescription?
    public var onFrameDecoded: (@Sendable (VideoFrame) -> Void)?

    public init() {}

    deinit {
        invalidate()
    }

    public func invalidate() {
        if let session = decompressionSession {
            VTDecompressionSessionInvalidate(session)
            decompressionSession = nil
        }
        formatDescription = nil
    }

    /// Configures the decoder using SPS and PPS NAL units from PACKET_CONFIG
    @discardableResult
    public func configureWith(sps: Data, pps: Data) -> Bool {
        invalidate()

        var status: OSStatus = noErr
        sps.withUnsafeBytes { spsPtr in
            pps.withUnsafeBytes { ppsPtr in
                let parameterSetPointers = [
                    spsPtr.baseAddress!.assumingMemoryBound(to: UInt8.self),
                    ppsPtr.baseAddress!.assumingMemoryBound(to: UInt8.self)
                ]
                let parameterSetSizes = [sps.count, pps.count]

                status = CMVideoFormatDescriptionCreateFromH264ParameterSets(
                    allocator: kCFAllocatorDefault,
                    parameterSetCount: 2,
                    parameterSetPointers: parameterSetPointers,
                    parameterSetSizes: parameterSetSizes,
                    nalUnitHeaderLength: 4,
                    formatDescriptionOut: &formatDescription
                )
            }
        }

        guard status == noErr, let format = formatDescription else {
            return false
        }

        return createDecompressionSession(with: format)
    }

    private func createDecompressionSession(with format: CMVideoFormatDescription) -> Bool {
        let destinationPixelBufferAttributes: [String: Any] = [
            kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_420YpCbCr8BiPlanarVideoRange,
            kCVPixelBufferMetalCompatibilityKey as String: true,
            kCVPixelBufferOpenGLCompatibilityKey as String: true
        ]

        var outputCallback = VTDecompressionOutputCallbackRecord(
            decompressionOutputCallback: decompressionOutputCallback,
            decompressionOutputRefCon: Unmanaged.passUnretained(self).toOpaque()
        )

        let status = VTDecompressionSessionCreate(
            allocator: kCFAllocatorDefault,
            formatDescription: format,
            decoderSpecification: nil,
            imageBufferAttributes: destinationPixelBufferAttributes as CFDictionary,
            outputCallback: &outputCallback,
            decompressionSessionOut: &decompressionSession
        )

        guard status == noErr, let session = decompressionSession else {
            return false
        }

        // Enable real-time decode mode (no frame delay)
        VTSessionSetProperty(session, key: kVTDecompressionPropertyKey_RealTime, value: kCFBooleanTrue)
        return true
    }

    /// Decodes an H.264 NAL unit
    public func decode(naluData: Data, ptsUs: Int64) {
        guard let session = decompressionSession, let format = formatDescription else { return }

        // Convert NAL unit to CMSampleBuffer
        var blockBuffer: CMBlockBuffer?
        let blockStatus = CMBlockBufferCreateWithMemoryBlock(
            allocator: kCFAllocatorDefault,
            memoryBlock: nil,
            blockLength: naluData.count,
            blockAllocator: kCFAllocatorDefault,
            customBlockSource: nil,
            offsetToData: 0,
            dataLength: naluData.count,
            flags: 0,
            blockBufferOut: &blockBuffer
        )

        guard blockStatus == kCMBlockBufferNoErr, let block = blockBuffer else { return }

        _ = naluData.withUnsafeBytes { rawPtr in
            CMBlockBufferReplaceDataBytes(
                with: rawPtr.baseAddress!,
                blockBuffer: block,
                offsetIntoDestination: 0,
                dataLength: naluData.count
            )
        }

        var sampleBuffer: CMSampleBuffer?
        let sampleSizeArray = [naluData.count]
        var timingInfo = CMSampleTimingInfo(
            duration: CMTime.invalid,
            presentationTimeStamp: CMTime(value: ptsUs, timescale: 1_000_000),
            decodeTimeStamp: .invalid
        )

        let sampleStatus = CMSampleBufferCreateReady(
            allocator: kCFAllocatorDefault,
            dataBuffer: block,
            formatDescription: format,
            sampleCount: 1,
            sampleTimingEntryCount: 1,
            sampleTimingArray: &timingInfo,
            sampleSizeEntryCount: 1,
            sampleSizeArray: sampleSizeArray,
            sampleBufferOut: &sampleBuffer
        )

        guard sampleStatus == noErr, let sample = sampleBuffer else { return }

        var flags: VTDecodeInfoFlags = []
        let contextPtr = UnsafeMutableRawPointer(bitPattern: Int(ptsUs))

        VTDecompressionSessionDecodeFrame(
            session,
            sampleBuffer: sample,
            flags: [._EnableAsynchronousDecompression],
            frameRefcon: contextPtr,
            infoFlagsOut: &flags
        )
    }
}

private func decompressionOutputCallback(
    decompressionOutputRefCon: UnsafeMutableRawPointer?,
    sourceFrameRefCon: UnsafeMutableRawPointer?,
    status: OSStatus,
    infoFlags: VTDecodeInfoFlags,
    imageBuffer: CVImageBuffer?,
    presentationTimeStamp: CMTime,
    presentationDuration: CMTime
) {
    guard status == noErr, let imageBuffer = imageBuffer, let refCon = decompressionOutputRefCon else {
        return
    }

    let decoder = Unmanaged<VideoDecoder>.fromOpaque(refCon).takeUnretainedValue()
    let capturePts = Int64(Int(bitPattern: sourceFrameRefCon))
    let width = CVPixelBufferGetWidth(imageBuffer)
    let height = CVPixelBufferGetHeight(imageBuffer)

    let frame = VideoFrame(
        pixelBuffer: imageBuffer,
        pts: presentationTimeStamp,
        width: width,
        height: height,
        captureTimestampUs: capturePts
    )

    decoder.onFrameDecoded?(frame)
}
