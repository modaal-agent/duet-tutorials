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
    .library(name: "FoyerServices", targets: ["FoyerServices"]),
    .library(name: "SignInShell", targets: ["SignInShell"]),
    .library(name: "HomeShell", targets: ["HomeShell"]),
    .library(name: "EditNameShell", targets: ["EditNameShell"]),
    .library(name: "AccountShell", targets: ["AccountShell"]),
    .library(name: "ProfileShell", targets: ["ProfileShell"]),
    .library(name: "MainShell", targets: ["MainShell"]),
    .library(name: "RootShell", targets: ["RootShell"]),
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
    // The four mock services behind the ports, product sources until
    // Tutorial 4 replaces them with the on-device backend.
    .target(
      name: "FoyerServices",
      dependencies: [.target(name: "FoyerKit")],
      swiftSettings: strictConcurrency
    ),
    // One shell target per level of the tree. A parent's target depends on
    // its children's: the Builder constructs them, the shell mounts them.
    .target(
      name: "SignInShell",
      dependencies: [
        "FoyerBridge",
        .target(name: "FoyerKit"),
        .product(name: "DuetShells", package: "duet"),
      ],
      swiftSettings: strictConcurrency
    ),
    .testTarget(
      name: "SignInShellTests",
      dependencies: ["SignInShell", "FoyerServices"],
      swiftSettings: strictConcurrency
    ),
    .target(
      name: "HomeShell",
      dependencies: [
        "FoyerBridge",
        .target(name: "FoyerKit"),
        .product(name: "DuetShells", package: "duet"),
      ],
      swiftSettings: strictConcurrency
    ),
    .testTarget(
      name: "HomeShellTests",
      dependencies: ["HomeShell", "FoyerServices"],
      swiftSettings: strictConcurrency
    ),
    .target(
      name: "EditNameShell",
      dependencies: [
        "FoyerBridge",
        .target(name: "FoyerKit"),
        .product(name: "DuetShells", package: "duet"),
      ],
      swiftSettings: strictConcurrency
    ),
    .testTarget(
      name: "EditNameShellTests",
      dependencies: ["EditNameShell", "FoyerServices"],
      swiftSettings: strictConcurrency
    ),
    .target(
      name: "AccountShell",
      dependencies: [
        "FoyerBridge",
        .target(name: "FoyerKit"),
        .product(name: "DuetShells", package: "duet"),
        "EditNameShell",
      ],
      swiftSettings: strictConcurrency
    ),
    .testTarget(
      name: "AccountShellTests",
      dependencies: ["AccountShell", "FoyerServices"],
      swiftSettings: strictConcurrency
    ),
    .target(
      name: "ProfileShell",
      dependencies: [
        "FoyerBridge",
        .target(name: "FoyerKit"),
        .product(name: "DuetShells", package: "duet"),
        "AccountShell",
      ],
      swiftSettings: strictConcurrency
    ),
    .testTarget(
      name: "ProfileShellTests",
      dependencies: ["ProfileShell", "FoyerServices"],
      swiftSettings: strictConcurrency
    ),
    .target(
      name: "MainShell",
      dependencies: [
        "FoyerBridge",
        .target(name: "FoyerKit"),
        .product(name: "DuetShells", package: "duet"),
        "HomeShell",
        "ProfileShell",
      ],
      swiftSettings: strictConcurrency
    ),
    .testTarget(
      name: "MainShellTests",
      dependencies: ["MainShell", "FoyerServices"],
      swiftSettings: strictConcurrency
    ),
    .target(
      name: "RootShell",
      dependencies: [
        "FoyerBridge",
        .target(name: "FoyerKit"),
        .product(name: "DuetShells", package: "duet"),
        "SplashShell",
        "SignInShell",
        "MainShell",
        "FoyerServices",
      ],
      swiftSettings: strictConcurrency
    ),
    .testTarget(
      name: "RootShellTests",
      dependencies: ["RootShell", "FoyerServices"],
      swiftSettings: strictConcurrency
    ),
  ]
)
