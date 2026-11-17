import Foundation

public enum PacketType: UInt8, Sendable {
    case config = 0x01        // SPS / PPS
    case frameKey = 0x02      // H.264 IDR I-Frame
    case frameDelta = 0x03    // H.264 P-Frame
    case pingPong = 0x04      // Heartbeat
    case control = 0x05       // Client to server control
}

public struct VCamPacket: Sendable {
    public let type: PacketType
    public let ptsUs: Int64
    public let payload: Data
    public let receivedAtUs: Int64

    public init(type: PacketType, ptsUs: Int64, payload: Data, receivedAtUs: Int64 = Int64(Date().timeIntervalSince1970 * 1_000_000)) {
        self.type = type
        self.ptsUs = ptsUs
        self.payload = payload
        self.receivedAtUs = receivedAtUs
    }
}

public final class PacketParser: @unchecked Sendable {
    public static let magic: UInt32 = 0x5643414D // "VCAM"
    public static let headerSize = 17

    private var buffer = Data()

    public init() {}

    /// Appends incoming socket data and extracts complete framed packets
    public func parse(incomingData: Data) -> [VCamPacket] {
        buffer.append(incomingData)
        var packets: [VCamPacket] = []

        while buffer.count >= Self.headerSize {
            // Find Magic header
            guard let magicIndex = findMagic() else {
                // If buffer grows too big without magic, discard to avoid memory bloat
                if buffer.count > 65536 {
                    buffer.removeAll()
                }
                break
            }

            if magicIndex > 0 {
                buffer.removeSubrange(0..<magicIndex)
            }

            guard buffer.count >= Self.headerSize else { break }

            let rawType = buffer[4]
            guard let type = PacketType(rawValue: rawType) else {
                // Invalid packet type, advance past magic
                buffer.removeFirst(4)
                continue
            }

            // Read PTS (8 bytes, Big-Endian)
            let ptsUs = buffer.subdata(in: 5..<13).withUnsafeBytes { ptr -> Int64 in
                Int64(bigEndian: ptr.load(as: Int64.self))
            }

            // Read Payload Size (4 bytes, Big-Endian)
            let payloadSize = buffer.subdata(in: 13..<17).withUnsafeBytes { ptr -> UInt32 in
                UInt32(bigEndian: ptr.load(as: UInt32.self))
            }

            let totalPacketSize = Self.headerSize + Int(payloadSize)
            guard buffer.count >= totalPacketSize else {
                // Awaiting remaining payload bytes from socket
                break
            }

            let payloadData = buffer.subdata(in: Self.headerSize..<totalPacketSize)
            packets.append(VCamPacket(type: type, ptsUs: ptsUs, payload: payloadData))

            buffer.removeSubrange(0..<totalPacketSize)
        }

        return packets
    }

    private func findMagic() -> Int? {
        guard buffer.count >= 4 else { return nil }
        let target = Self.magic.bigEndian
        return buffer.withUnsafeBytes { rawBuffer -> Int? in
            let ptr = rawBuffer.bindMemory(to: UInt8.self)
            for i in 0...(buffer.count - 4) {
                let val = ptr.baseAddress!.advanced(by: i).withMemoryRebound(to: UInt32.self, capacity: 1) { $0.pointee }
                if val == target {
                    return i
                }
            }
            return nil
        }
    }
}
