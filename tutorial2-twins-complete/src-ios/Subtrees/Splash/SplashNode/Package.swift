// swift-tools-version:5.9

// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import PackageDescription

// The app-facing half of the splash subtree: the shell, the builder and the
// SwiftUI view over the Swift twin in ../SplashFeature. The iPhone app links
// this package. Its tests are the shells lane, on the macOS host.
let strictConcurrency: [SwiftSetting] = [
  .enableExperimentalFeature("StrictConcurrency")
]

let package = Package(
  name: "SplashNode",
  platforms: [
    // iOS for the app; macOS so the shells lane runs as plain `swift test`.
    .iOS(.v17),
    .macOS(.v14),
  ],
  products: [
    .library(name: "SplashNode", targets: ["SplashNode"])
  ],
  dependencies: [
    .package(url: "https://github.com/modaal-agent/duet.git", exact: "0.7.0"),
    .package(path: "../SplashFeature"),
  ],
  targets: [
    .target(
      name: "SplashNode",
      dependencies: [
        .product(name: "Duet", package: "duet"),
        .product(name: "DuetShells", package: "duet"),
        .product(name: "SplashFeature", package: "SplashFeature"),
      ],
      swiftSettings: strictConcurrency
    ),
    .testTarget(
      name: "SplashNodeTests",
      dependencies: [
        "SplashNode",
        .product(name: "DuetTesting", package: "duet"),
      ],
      swiftSettings: strictConcurrency
    ),
  ]
)
