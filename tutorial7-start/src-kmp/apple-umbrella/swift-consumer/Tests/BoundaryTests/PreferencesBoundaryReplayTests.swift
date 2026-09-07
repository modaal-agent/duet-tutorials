// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import FoyerKit
import XCTest

/// The preferences feature's recordings replayed across the framework, one method
/// per recording.
final class PreferencesBoundaryReplayTests: XCTestCase {

  private func replay(_ fixture: String) throws {
    let steps = try BoundaryReplayHarness.replay(fixture: fixture) { initialState in
      try FoyerBoundary.shared.makeSession(feature: "preferences", initialStateJson: initialState)
    }
    XCTAssertGreaterThan(steps, 0, "the recording must contain steps to replay")
  }

  func testTogglePublishesReadiness() throws {
    try replay("preferences.toggle-publishes-readiness")
  }

  func testContinueNeedsOne() throws {
    try replay("preferences.continue-needs-one")
  }

  func testContinueClimbs() throws {
    try replay("preferences.continue-climbs")
  }
}
