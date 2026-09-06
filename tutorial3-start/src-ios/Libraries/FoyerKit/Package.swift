// swift-tools-version:5.9

// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import PackageDescription

// The iOS app's consumer package: the binary target over the Kotlin core's
// XCFramework at the path scripts/assemble_kit.sh publishes, the bridge every
// shell shares, and one shell target per feature. This package's `swift test`
// is the Swift shells lane; it runs after the framework is assembled, which is
// why the parity manifest's `swift:` paths never point here.
//
// Complete concurrency checking under the Swift 5 language mode, in every
// target: `Sendable` conformances and actor isolation are compiler-checked.
let strictConcurrency: [SwiftSetting] = [
  .enableExperimentalFeature("StrictConcurrency")
]

let package = Package(
  name: "FoyerKit",
  platforms: [
    // iOS for the app; macOS so the shells lane runs as plain `swift test`.
    .iOS(.v17),
    .macOS(.v14),
  ],
  products: [
    .library(name: "FoyerBridge", targets: ["FoyerBridge", "FoyerKit"]),
    .library(name: "SplashShell", targets: ["SplashShell"]),
  ],
  dependencies: [
    // The Duet framework, pinned exactly: a newer release is a deliberate
    // re-pin. The Kotlin half pins the same release in
    // src-kmp/gradle/libs.versions.toml.
    .package(url: "https://github.com/modaal-agent/duet.git", exact: "0.7.0")
  ],
  targets: [
    // The Kotlin core, prebuilt. scripts/assemble_kit.sh is the only writer
    // of this path.
    .binaryTarget(
      name: "FoyerKit",
      path: "../../../src-kmp/apple-umbrella/build/XCFrameworks/app/FoyerKit.xcframework"
    ),
    // The store mirror, its own target so every shell and the app share one
    // copy. It lives here and not in the framework because the bridged
    // `SkieSwiftStateFlow` type is generated per framework.
    .target(
      name: "FoyerBridge",
      dependencies: [
        .target(name: "FoyerKit"),
        .product(name: "DuetShells", package: "duet"),
      ],
      swiftSettings: strictConcurrency,
      linkerSettings: [
        // FoyerKit is a static Kotlin/Native framework; its runtime needs
        // libc++ symbols Swift does not autolink.
        .linkedLibrary("c++"),
        .linkedFramework("Foundation"),
      ]
    ),
    .target(
      name: "SplashShell",
      dependencies: [
        "FoyerBridge",
        .target(name: "FoyerKit"),
        .product(name: "DuetShells", package: "duet"),
      ],
      swiftSettings: strictConcurrency
    ),
    .testTarget(
      name: "SplashShellTests",
      dependencies: ["SplashShell"],
      swiftSettings: strictConcurrency
    ),
  ]
)
