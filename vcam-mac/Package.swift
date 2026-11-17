// swift-tools-version: 6.0
import PackageDescription

let package = Package(
    name: "vcam-mac",
    platforms: [
        .macOS(.v14)
    ],
    products: [
        .executable(name: "vcam", targets: ["VCamApp"]),
        .library(name: "VCamCore", targets: ["VCamCore"])
    ],
    dependencies: [],
    targets: [
        .target(
            name: "VCamCore",
            dependencies: [],
            path: "Sources/VCamCore"
        ),
        .executableTarget(
            name: "VCamApp",
            dependencies: ["VCamCore"],
            path: "Sources/VCamApp"
        )
    ]
)
