// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import FoyerKit
import Foundation
import XCTest

/// The boundary's error channel: a Kotlin failure arrives in Swift as a
/// catchable error that still carries the Kotlin message. Without `@Throws`
/// on the Kotlin side the process would terminate instead, and this test
/// could not be written.
final class BoundaryErrorChannelTests: XCTestCase {

  func testUnregisteredFeatureSurfacesAsSwiftError() {
    XCTAssertThrowsError(
      try FoyerBoundary.shared.makeSession(feature: "no-such-feature", initialStateJson: "{}")
    ) { error in
      XCTAssertTrue(
        (error as NSError).localizedDescription.contains("no-such-feature"),
        "the Kotlin failure detail must survive the crossing; got: \(error)")
    }
  }

  /// The canonical writer reaches Swift: the expected side of every replay
  /// comparison goes through this call.
  func testCanonicalizeReachesSwift() throws {
    let canonical = try FoyerBoundary.shared.canonicalize(rawJson: #"{"b":2,"a":1}"#)
    XCTAssertEqual(canonical, #"{"a":1,"b":2}"#, "the core's writer orders keys")
  }
}
