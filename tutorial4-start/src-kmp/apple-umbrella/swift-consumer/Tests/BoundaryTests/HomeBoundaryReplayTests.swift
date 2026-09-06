// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import FoyerKit
import XCTest

/// The home feature's recordings replayed across the framework, one method
/// per recording.
final class HomeBoundaryReplayTests: XCTestCase {

  private func replay(_ fixture: String) throws {
    let steps = try BoundaryReplayHarness.replay(fixture: fixture) { initialState in
      try FoyerBoundary.shared.makeSession(feature: "home", initialStateJson: initialState)
    }
    XCTAssertGreaterThan(steps, 0, "the recording must contain steps to replay")
  }

  func testLoadsOnce() throws {
    try replay("home.loads-once")
  }

  func testReappearKeepsItems() throws {
    try replay("home.reappear-keeps-items")
  }
}
