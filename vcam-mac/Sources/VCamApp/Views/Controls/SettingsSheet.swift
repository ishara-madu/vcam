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
                Text("Settings")
                    .font(.system(size: 15, weight: .semibold))
                Spacer()
                Button(action: onDismiss) {
                    Image(systemName: "xmark")
                        .font(.system(size: 11, weight: .medium))
                        .foregroundColor(.secondary)
                        .frame(width: 20, height: 20)
                        .background(Color.white.opacity(0.06))
                        .clipShape(Circle())
                }
                .buttonStyle(.plain)
            }

            VStack(spacing: 16) {
                // Section 1: Virtual Camera System
                VStack(alignment: .leading, spacing: 8) {
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Virtual Camera")
                                .font(.system(size: 12, weight: .medium))
                            Text("Publishes stream to FaceTime, Zoom, Meet, Teams, OBS")
                                .font(.system(size: 10))
                                .foregroundColor(.secondary)
                        }
                        Spacer()
                        Toggle("", isOn: $isVirtualCameraActive)
                            .toggleStyle(.switch)
                            .labelsHidden()
                    }
                }
                .padding(12)
                .background(Color.white.opacity(0.03))
                .clipShape(RoundedRectangle(cornerRadius: 10))
                .overlay(
                    RoundedRectangle(cornerRadius: 10).stroke(Color.white.opacity(0.06), lineWidth: 0.75)
                )

                // Section 2: Connection Transport
                VStack(alignment: .leading, spacing: 10) {
                    Text("TRANSPORT")
                        .font(.system(size: 10, weight: .semibold))
                        .foregroundColor(.secondary)

                    Picker("Transport", selection: $isUSBMode) {
                        Text("USB (ADB Port Forward)").tag(true)
                        Text("Wi-Fi (Direct LAN)").tag(false)
                    }
                    .pickerStyle(.segmented)
                    .labelsHidden()

                    if isUSBMode {
                        HStack {
                            Text("Endpoint: **127.0.0.1:\(port)**")
                                .font(.system(size: 11))
                                .foregroundColor(.secondary)
                            Spacer()
                            Text("Ultra-low latency")
                                .font(.system(size: 10, design: .monospaced))
                                .foregroundColor(Color(red: 0.2, green: 0.85, blue: 0.4))
                        }
                        .padding(.top, 2)
                    } else {
                        HStack(spacing: 8) {
                            Text("Phone IP:")
                                .font(.system(size: 11))
                                .foregroundColor(.secondary)
                            TextField("192.168.1.x", text: $host)
                                .textFieldStyle(.plain)
                                .font(.system(size: 11, design: .monospaced))
                                .padding(6)
                                .background(Color.white.opacity(0.04))
                                .clipShape(RoundedRectangle(cornerRadius: 6))

                            Text("Port:")
                                .font(.system(size: 11))
                                .foregroundColor(.secondary)
                            TextField("8088", value: $port, format: .number)
                                .textFieldStyle(.plain)
                                .font(.system(size: 11, design: .monospaced))
                                .frame(width: 50)
                                .padding(6)
                                .background(Color.white.opacity(0.04))
                                .clipShape(RoundedRectangle(cornerRadius: 6))
                        }
                    }
                }
                .padding(12)
                .background(Color.white.opacity(0.03))
                .clipShape(RoundedRectangle(cornerRadius: 10))
                .overlay(
                    RoundedRectangle(cornerRadius: 10).stroke(Color.white.opacity(0.06), lineWidth: 0.75)
                )

                // Section 3: Hardware Decoding Readout
                HStack {
                    Text("Decoder")
                        .font(.system(size: 11, weight: .medium))
                    Spacer()
                    Text("Apple VideoToolbox (Hardware NV12)")
                        .font(.system(size: 10, design: .monospaced))
                        .foregroundColor(.secondary)
                }
                .padding(.horizontal, 4)
            }

            Spacer()

            Button("Done") {
                onDismiss()
            }
            .buttonStyle(.borderedProminent)
            .controlSize(.regular)
            .keyboardShortcut(.defaultAction)
            .frame(maxWidth: .infinity)
        }
        .padding(20)
        .frame(width: 380, height: 350)
    }
}
