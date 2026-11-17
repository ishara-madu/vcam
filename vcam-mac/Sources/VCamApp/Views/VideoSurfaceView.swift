import SwiftUI
import CoreVideo
import CoreImage

public struct VideoSurfaceView: View {
    public let pixelBuffer: CVPixelBuffer?
    public let isConnected: Bool

    @State private var copied: Bool = false

    public init(pixelBuffer: CVPixelBuffer?, isConnected: Bool) {
        self.pixelBuffer = pixelBuffer
        self.isConnected = isConnected
    }

    public var body: some View {
        GeometryReader { geometry in
            ZStack {
                Color.black.ignoresSafeArea()

                if let buffer = pixelBuffer, let image = makeNSImage(from: buffer) {
                    Image(nsImage: image)
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .frame(width: geometry.size.width, height: geometry.size.height)
                } else {
                    // Minimalist Standby Canvas
                    VStack(spacing: 16) {
                        Image(systemName: "camera")
                            .font(.system(size: 32, weight: .thin))
                            .foregroundColor(.white.opacity(0.3))

                        VStack(spacing: 4) {
                            Text("Waiting for Android Camera")
                                .font(.system(size: 13, weight: .medium))
                                .foregroundColor(.white.opacity(0.75))

                            Text("Connect via USB or ensure both devices are on the same Wi-Fi")
                                .font(.system(size: 11))
                                .foregroundColor(.white.opacity(0.35))
                        }

                        // Minimalist Copy Command Pill
                        Button {
                            NSPasteboard.general.clearContents()
                            NSPasteboard.general.setString("adb forward tcp:8088 tcp:8088", forType: .string)
                            withAnimation(.easeInOut(duration: 0.2)) {
                                copied = true
                            }
                            DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
                                withAnimation { copied = false }
                            }
                        } label: {
                            HStack(spacing: 8) {
                                Text("adb forward tcp:8088 tcp:8088")
                                    .font(.system(size: 11, weight: .medium, design: .monospaced))
                                    .foregroundColor(.white.opacity(0.65))

                                Image(systemName: copied ? "checkmark" : "doc.on.doc")
                                    .font(.system(size: 10, weight: .semibold))
                                    .foregroundColor(copied ? .green : .white.opacity(0.4))
                            }
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(Color.white.opacity(0.04))
                            .clipShape(Capsule())
                            .overlay(
                                Capsule().stroke(Color.white.opacity(0.08), lineWidth: 0.75)
                            )
                        }
                        .buttonStyle(.plain)
                        .help("Copy ADB USB forward command")
                    }
                    .padding(32)
                }
            }
        }
    }

    private func makeNSImage(from buffer: CVPixelBuffer) -> NSImage? {
        let ciImage = CIImage(cvPixelBuffer: buffer)
        let rep = NSCIImageRep(ciImage: ciImage)
        let nsImage = NSImage(size: rep.size)
        nsImage.addRepresentation(rep)
        return nsImage
    }
}
