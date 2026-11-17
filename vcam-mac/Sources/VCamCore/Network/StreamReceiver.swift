import Foundation
import Network

public enum StreamConnectionState: String, Sendable {
    case disconnected = "Disconnected"
    case connecting = "Connecting..."
    case connected = "Connected (Streaming)"
    case failed = "Connection Failed"
}

public final class StreamReceiver: @unchecked Sendable {
    private var connection: NWConnection?
    private let parser = PacketParser()
    private let decoder = VideoDecoder()
    private let queue = DispatchQueue(label: "com.pixeleye.vcam.receiver", qos: .userInteractive)

    public var onStateChange: (@Sendable (StreamConnectionState) -> Void)?
    public var onFrameReceived: (@Sendable (VideoFrame) -> Void)?
    public var onStatsUpdate: (@Sendable (Double, Double, Int) -> Void)? // fps, latencyMs, bytesPerSec

    private var frameCount: Int = 0
    private var byteCount: Int = 0
    private var lastStatsTime: TimeInterval = Date().timeIntervalSinceReferenceDate

    public init() {
        decoder.onFrameDecoded = { [weak self] frame in
            guard let self = self else { return }
            self.onFrameReceived?(frame)
            VirtualCameraManager.shared.publishFrame(frame.pixelBuffer, pts: frame.pts)

            self.frameCount += 1
            let now = Date().timeIntervalSinceReferenceDate
            let elapsed = now - self.lastStatsTime
            if elapsed >= 1.0 {
                let fps = Double(self.frameCount) / elapsed
                let bytesSec = Int(Double(self.byteCount) / elapsed)
                self.onStatsUpdate?(fps, frame.latencyMs, bytesSec)
                self.frameCount = 0
                self.byteCount = 0
                self.lastStatsTime = now
            }
        }
    }

    public func connect(host: String = "127.0.0.1", port: UInt16 = 8088) {
        disconnect()

        onStateChange?(.connecting)

        let tcpOptions = NWProtocolTCP.Options()
        tcpOptions.noDelay = true // Disable Nagle's algorithm for zero latency

        let params = NWParameters(tls: nil, tcp: tcpOptions)
        params.prohibitedInterfaceTypes = []

        let nwEndpoint = NWEndpoint.hostPort(
            host: NWEndpoint.Host(host),
            port: NWEndpoint.Port(rawValue: port)!
        )

        let conn = NWConnection(to: nwEndpoint, using: params)
        self.connection = conn

        conn.stateUpdateHandler = { [weak self] state in
            guard let self = self else { return }
            switch state {
            case .ready:
                self.onStateChange?(.connected)
                VirtualCameraManager.shared.startVirtualCamera()
                self.receiveNextChunk()
            case .failed:
                self.onStateChange?(.failed)
            case .cancelled:
                self.onStateChange?(.disconnected)
            default:
                break
            }
        }

        conn.start(queue: queue)
    }

    public func disconnect() {
        if let conn = connection {
            conn.cancel()
            connection = nil
        }
        decoder.invalidate()
        VirtualCameraManager.shared.stopVirtualCamera()
        onStateChange?(.disconnected)
    }

    private func receiveNextChunk() {
        connection?.receive(minimumIncompleteLength: 1, maximumLength: 65536) { [weak self] content, _, isComplete, error in
            guard let self = self else { return }

            if let data = content, !data.isEmpty {
                self.byteCount += data.count
                let packets = self.parser.parse(incomingData: data)
                for packet in packets {
                    self.handlePacket(packet)
                }
            }

            if isComplete || error != nil {
                self.disconnect()
            } else {
                self.receiveNextChunk()
            }
        }
    }

    private func handlePacket(_ packet: VCamPacket) {
        switch packet.type {
        case .config:
            // SPS / PPS configuration packet (Annex B or length prefixed)
            if packet.payload.count > 4 {
                self.decoder.configureWith(sps: packet.payload, pps: packet.payload)
            }
        case .frameKey, .frameDelta:
            self.decoder.decode(naluData: packet.payload, ptsUs: packet.ptsUs)
        default:
            break
        }
    }
}
