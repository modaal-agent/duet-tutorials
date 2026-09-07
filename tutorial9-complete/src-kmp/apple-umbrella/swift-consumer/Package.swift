// swift-tools-version:6.0

// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import PackageDescription

// The boundary replay suite: link the assembled FoyerKit.xcframework and
// replay the committed fixtures across it. Test-only; the package builds only
// after scripts/assemble_kit.sh has published the framework, which is why the
// manifest's `swift:` paths never point here and the lane script owns the
// ordering.
let package = Package(
  name: "FoyerKitConsumer",
  platforms: [.macOS(.v13)],
  targets: [
    // The Kotlin core, prebuilt. scripts/assemble_kit.sh is the only writer
    // of this path.
    .binaryTarget(
      name: "FoyerKit",
      path: "../build/XCFrameworks/app/FoyerKit.xcframework"
    ),
    .testTarget(
      name: "BoundaryTests",
      dependencies: ["FoyerKit"],
      swiftSettings: [.enableExperimentalFeature("StrictConcurrency")],
      linkerSettings: [
        // FoyerKit is a static Kotlin/Native framework; its runtime needs
        // libc++ symbols Swift does not autolink.
        .linkedLibrary("c++"),
        .linkedFramework("Foundation"),
      ]
    ),
  ]
)
