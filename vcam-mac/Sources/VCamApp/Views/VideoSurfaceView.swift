import SwiftUI
import CoreVideo
import CoreImage

public struct VideoSurfaceView: View {
    public let pixelBuffer: CVPixelBuffer?
    public let isConnected: Bool

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
                    // Modern Minimalist Camera Standby Surface
                    VStack(spacing: 20) {
                        ZStack {
                            Circle()
                                .stroke(Color.cyan.opacity(0.15), lineWidth: 1)
                                .frame(width: 140, height: 140)

                            Circle()
                                .stroke(Color.cyan.opacity(0.35), lineWidth: 1.5)
                                .frame(width: 90, height: 90)

                            Image(systemName: "video.fill")
                                .font(.system(size: 32, weight: .semibold))
                                .foregroundStyle(
                                    LinearGradient(
                                        colors: [.cyan, .blue],
                                        startPoint: .topLeading,
                                        endPoint: .bottomTrailing
                                    )
                                )
                        }

                        VStack(spacing: 6) {
                            Text("Waiting for Android Camera Feed")
                                .font(.system(size: 16, weight: .semibold))
                                .foregroundColor(.white.opacity(0.9))

                            Text("Connect your phone via USB and run:")
                                .font(.system(size: 13, weight: .regular))
                                .foregroundColor(.white.opacity(0.55))

                            HStack(spacing: 8) {
                                Text("adb forward tcp:8088 tcp:8088")
                                    .font(.system(size: 12, weight: .semibold, design: .monospaced))
                                    .foregroundColor(.cyan)

                                Button {
                                    NSPasteboard.general.clearContents()
                                    NSPasteboard.general.setString("adb forward tcp:8088 tcp:8088", forType: .string)
                                } label: {
                                    Image(systemName: "doc.on.doc.fill")
                                        .font(.system(size: 11))
                                        .foregroundColor(.cyan.opacity(0.8))
                                }
                                .buttonStyle(.plain)
                                .help("Copy command to clipboard")
                            }
                            .padding(.horizontal, 14)
                            .padding(.vertical, 8)
                            .background(Color.white.opacity(0.06))
                            .clipShape(RoundedRectangle(cornerRadius: 10))
                            .overlay(
                                RoundedRectangle(cornerRadius: 10)
                                    .stroke(Color.cyan.opacity(0.25), lineWidth: 1)
                            )
                        }
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
