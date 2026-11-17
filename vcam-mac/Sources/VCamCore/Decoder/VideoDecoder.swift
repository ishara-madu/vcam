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

    /// Configures the decoder by extracting SPS and PPS from Annex B config payload
    @discardableResult
    public func configureWith(configPayload: Data) -> Bool {
        let nalus = extractAnnexBNalus(from: configPayload)
        // SPS NAL unit type is 7, PPS is 8
        guard let sps = nalus.first(where: { ($0.first ?? 0) & 0x1F == 7 }),
              let pps = nalus.first(where: { ($0.first ?? 0) & 0x1F == 8 }) else {
            // If unable to distinguish by type, try using first two NALUs if available
            if nalus.count >= 2 {
                return configureWith(sps: nalus[0], pps: nalus[1])
            }
            return false
        }
        return configureWith(sps: sps, pps: pps)
    }

    /// Configures the decoder using pure SPS and PPS NAL units (without start codes)
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

        // Convert Annex B start codes (00 00 00 01) to 4-byte AVCC length prefix
        let avccData = convertAnnexBToAVCC(naluData)
        guard !avccData.isEmpty else { return }

        var blockBuffer: CMBlockBuffer?
        let blockStatus = CMBlockBufferCreateWithMemoryBlock(
            allocator: kCFAllocatorDefault,
            memoryBlock: nil,
            blockLength: avccData.count,
            blockAllocator: kCFAllocatorDefault,
            customBlockSource: nil,
            offsetToData: 0,
            dataLength: avccData.count,
            flags: 0,
            blockBufferOut: &blockBuffer
        )

        guard blockStatus == kCMBlockBufferNoErr, let block = blockBuffer else { return }

        _ = avccData.withUnsafeBytes { rawPtr in
            CMBlockBufferReplaceDataBytes(
                with: rawPtr.baseAddress!,
                blockBuffer: block,
                offsetIntoDestination: 0,
                dataLength: avccData.count
            )
        }

        var sampleBuffer: CMSampleBuffer?
        let sampleSizeArray = [avccData.count]
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

    /// Converts Annex B NAL unit(s) into AVCC (4-byte length prefixed) buffer
    private func convertAnnexBToAVCC(_ data: Data) -> Data {
        let nalus = extractAnnexBNalus(from: data)
        guard !nalus.isEmpty else { return data }

        var avccBuffer = Data()
        for nalu in nalus {
            var length = UInt32(nalu.count).bigEndian
            withUnsafeBytes(of: &length) { avccBuffer.append(contentsOf: $0) }
            avccBuffer.append(nalu)
        }
        return avccBuffer
    }

    /// Splits an Annex B byte sequence into individual NAL units (without start codes)
    private func extractAnnexBNalus(from data: Data) -> [Data] {
        var nalus: [Data] = []
        let count = data.count
        guard count >= 4 else { return [] }

        data.withUnsafeBytes { rawBuffer in
            let bytes = rawBuffer.bindMemory(to: UInt8.self).baseAddress!
            var startIndices: [Int] = []

            var i = 0
            while i < count - 3 {
                if bytes[i] == 0 && bytes[i + 1] == 0 {
                    if bytes[i + 2] == 1 {
                        startIndices.append(i + 3)
                        i += 3
                        continue
                    } else if bytes[i + 2] == 0 && i < count - 4 && bytes[i + 3] == 1 {
                        startIndices.append(i + 4)
                        i += 4
                        continue
                    }
                }
                i += 1
            }

            for idx in 0..<startIndices.count {
                let start = startIndices[idx]
                let end: Int
                if idx + 1 < startIndices.count {
                    var nextStart = startIndices[idx + 1]
                    // Step back past the 00 00 01 or 00 00 00 01
                    if nextStart >= 4 && bytes[nextStart - 4] == 0 {
                        nextStart -= 4
                    } else if nextStart >= 3 {
                        nextStart -= 3
                    }
                    end = nextStart
                } else {
                    end = count
                }

                if end > start {
                    let naluData = Data(bytes: bytes.advanced(by: start), count: end - start)
                    nalus.append(naluData)
                }
            }
        }

        return nalus
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
