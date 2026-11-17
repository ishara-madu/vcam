import SwiftUI
import VCamCore

public struct SettingsSheet: View {
    @Binding public var isUSBMode: Bool
    @Binding public var host: String
    @Binding public var port: UInt16
    @Binding public var isVirtualCameraActive: Bool
    public let onDismiss: () -> Void

    public init(
        isUSBMode: Binding<Bool>,
        host: Binding<String>,
        port: Binding<UInt16>,
        isVirtualCameraActive: Binding<Bool>,
        onDismiss: @escaping () -> Void
    ) {
        self._isUSBMode = isUSBMode
        self._host = host
        self._port = port
        self._isVirtualCameraActive = isVirtualCameraActive
        self.onDismiss = onDismiss
    }

    public var body: some View {
        VStack(spacing: 20) {
            // Header
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("vcam Settings")
                        .font(.system(size: 18, weight: .bold))
                    Text("macOS Virtual Camera & Streaming Configuration")
                        .font(.system(size: 12))
                        .foregroundColor(.secondary)
                }
                Spacer()
                Button(action: onDismiss) {
                    Image(systemName: "xmark.circle.fill")
                        .font(.system(size: 20))
                        .foregroundColor(.secondary)
                }
                .buttonStyle(.plain)
            }

            Divider()

            // Virtual Camera Configuration Box
            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    Image(systemName: "camera.viewfinder")
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(.cyan)
                    Text("macOS System Virtual Camera")
                        .font(.system(size: 13, weight: .semibold))
                    Spacer()
                    Toggle("", isOn: $isVirtualCameraActive)
                        .toggleStyle(.switch)
                }

                Text("When enabled, **vcam Virtual Camera** appears as a native webcam in FaceTime, Zoom, Google Meet, Teams, OBS Studio, and Web Browsers.")
                    .font(.system(size: 11))
                    .foregroundColor(.secondary)
                    .lineSpacing(2)

                HStack(spacing: 6) {
                    Text("System Extension:")
                        .font(.system(size: 10, weight: .semibold))
                        .foregroundColor(.secondary)
                    Text("CoreMediaIO CMIOExtension")
                        .font(.system(size: 10, weight: .semibold, design: .monospaced))
                        .foregroundColor(.cyan)
                }
                .padding(.horizontal, 8)
                .padding(.vertical, 4)
                .background(Color.white.opacity(0.06))
                .clipShape(RoundedRectangle(cornerRadius: 6))
            }
            .padding(14)
            .background(Color.white.opacity(0.04))
            .clipShape(RoundedRectangle(cornerRadius: 12))

            // Transport Options
            VStack(alignment: .leading, spacing: 12) {
                Text("TRANSPORT & PROTOCOL")
                    .font(.system(size: 10, weight: .bold))
                    .foregroundColor(.secondary)

                Picker("Connection Mode", selection: $isUSBMode) {
                    Text("USB (ADB Tunnel - Zero Latency)").tag(true)
                    Text("Wi-Fi (Direct LAN Socket)").tag(false)
                }
                .pickerStyle(.segmented)

                if isUSBMode {
                    HStack {
                        Image(systemName: "cable.connector")
                            .foregroundColor(.green)
                        Text("Port Forwarding: **127.0.0.1:\(port)**")
                            .font(.system(size: 12))
                        Spacer()
                        Text("< 2ms USB overhead")
                            .font(.system(size: 11, design: .monospaced))
                            .foregroundColor(.green)
                    }
                    .padding(10)
                    .background(Color.white.opacity(0.04))
                    .clipShape(RoundedRectangle(cornerRadius: 8))
                } else {
                    HStack(spacing: 10) {
                        Text("Phone IP:")
                            .font(.system(size: 12, weight: .medium))
                        TextField("192.168.1.x", text: $host)
                            .textFieldStyle(.roundedBorder)
                            .frame(width: 140)

                        Text("Port:")
                            .font(.system(size: 12, weight: .medium))
                        TextField("8088", value: $port, format: .number)
                            .textFieldStyle(.roundedBorder)
                            .frame(width: 70)
                    }
                }
            }
            .padding(14)
            .background(Color.white.opacity(0.04))
            .clipShape(RoundedRectangle(cornerRadius: 12))

            // Hardware Decoder Box
            HStack(spacing: 10) {
                Image(systemName: "bolt.badge.clock.fill")
                    .font(.system(size: 16))
                    .foregroundColor(.green)
                VStack(alignment: .leading, spacing: 2) {
                    Text("VideoToolbox Real-Time Hardware Decompression")
                        .font(.system(size: 12, weight: .semibold))
                    Text("Zero CPU overhead • Decodes directly to NV12 CVPixelBuffer")
                        .font(.system(size: 10))
                        .foregroundColor(.secondary)
                }
                Spacer()
            }
            .padding(12)
            .background(Color.white.opacity(0.04))
            .clipShape(RoundedRectangle(cornerRadius: 10))

            Spacer()

            Button("Done") {
                onDismiss()
            }
            .keyboardShortcut(.defaultAction)
            .frame(maxWidth: .infinity)
        }
        .padding(24)
        .frame(width: 480, height: 490)
    }
}
