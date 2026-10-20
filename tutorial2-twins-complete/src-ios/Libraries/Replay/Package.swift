// swift-tools-version:5.9

// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import PackageDescription

// The Swift replay runner: the app's replay registry and the replay-protocol
// stdio server `tools/duet protocol-run` drives (declared to the CLI by the
// manifest's `replayRunner:` key). It links each feature's Swift twin and no
// app code. The Kotlin twin's runner is src-android/replay-runner.
let strictConcurrency: [SwiftSetting] = [
  .enableExperimentalFeature("StrictConcurrency")
]

let package = Package(
  name: "Replay",
  platforms: [
    .iOS(.v17),
    .macOS(.v14),
  ],
  products: [
    .library(name: "ReplayAdapter", targets: ["ReplayAdapter"])
  ],
  dependencies: [
    // The Duet framework, pinned exactly, at the release every other manifest
    // in this tree names.
    .package(url: "https://github.com/modaal-agent/duet.git", exact: "0.7.0"),
    .package(path: "../../Subtrees/Splash/SplashFeature"),
  ],
  targets: [
    .target(
      name: "ReplayAdapter",
      dependencies: [
        .product(name: "Duet", package: "duet"),
        .product(name: "DuetReplay", package: "duet"),
        .product(name: "SplashFeature", package: "SplashFeature"),
      ],
      swiftSettings: strictConcurrency
    ),
    .executableTarget(
      name: "replay-runner",
      dependencies: [
        "ReplayAdapter",
        .product(name: "DuetReplay", package: "duet"),
      ],
      swiftSettings: strictConcurrency
    ),
  ]
)
