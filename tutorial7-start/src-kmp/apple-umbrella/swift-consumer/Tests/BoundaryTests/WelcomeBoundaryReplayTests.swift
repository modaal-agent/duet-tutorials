// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import FoyerKit
import XCTest

/// The welcome feature's recordings replayed across the framework, one method
/// per recording.
final class WelcomeBoundaryReplayTests: XCTestCase {

  private func replay(_ fixture: String) throws {
    let steps = try BoundaryReplayHarness.replay(fixture: fixture) { initialState in
      try FoyerBoundary.shared.makeSession(feature: "welcome", initialStateJson: initialState)
    }
    XCTAssertGreaterThan(steps, 0, "the recording must contain steps to replay")
  }

  func testAppearancePublishesReadiness() throws {
    try replay("welcome.appearance-publishes-readiness")
  }

  func testContinueClimbs() throws {
    try replay("welcome.continue-climbs")
  }
}
