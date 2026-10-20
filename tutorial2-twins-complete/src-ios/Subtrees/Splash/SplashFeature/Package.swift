// swift-tools-version:5.9

// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import PackageDescription

// The Swift twin of the splash: the reducer, its types, the runtime half and
// their tests. This package is the Swift lane's root — `tools/duet verify`
// runs `swift test` here on the macOS host, no simulator — and it depends on
// the Duet framework and nothing else, so the lane resolves the family only.
// The shell, the builder and the view live in the sibling package
// ../SplashNode, which the iOS app links.
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
    // re-pin. The Kotlin twin pins the same release in its Gradle version
    // catalog; the two move together.
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
