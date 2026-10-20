// swift-tools-version:5.9

// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import PackageDescription

// The splash feature: the reducer, its types, the runtime half and their
// tests. This package is the Swift lane's root — `tools/duet verify` runs
// `swift test` here on the macOS host, no simulator — and it depends on the
// Duet framework and nothing else, so the lane resolves the family only.
// App-side code (the shell, the builder, the view) goes in a sibling package,
// so nothing here resolves an app dependency.
//
// Complete concurrency checking under the Swift 5 language mode, in every
// target: `Sendable` conformances and actor isolation are compiler-checked.
let strictConcurrency: [SwiftSetting] = [
  .enableExperimentalFeature("StrictConcurrency")
]

let package = Package(
  name: "SplashFeature",
  platforms: [
    // iOS for the app; macOS so the Swift lane runs as plain `swift test`.
    .iOS(.v17),
    .macOS(.v14),
  ],
  products: [
    .library(name: "SplashFeature", targets: ["SplashFeature"])
  ],
  dependencies: [
    // The Duet framework, pinned exactly: a newer release is a deliberate
    // re-pin, made in every manifest of the tree at once.
    .package(url: "https://github.com/modaal-agent/duet.git", exact: "0.7.0")
  ],
  targets: [
    .target(
      name: "SplashFeature",
      dependencies: [
        .product(name: "Duet", package: "duet")
      ],
      swiftSettings: strictConcurrency
    ),
    .testTarget(
      name: "SplashFeatureTests",
      dependencies: [
        "SplashFeature",
        .product(name: "DuetTesting", package: "duet"),
      ],
      swiftSettings: strictConcurrency
    ),
  ]
)
