// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import FoyerKit
import XCTest

/// The main feature's recordings replayed across the framework, one method
/// per recording.
final class MainBoundaryReplayTests: XCTestCase {

  private func replay(_ fixture: String) throws {
    let steps = try BoundaryReplayHarness.replay(fixture: fixture) { initialState in
      try FoyerBoundary.shared.makeSession(feature: "main", initialStateJson: initialState)
    }
    XCTAssertGreaterThan(steps, 0, "the recording must contain steps to replay")
  }

  func testTabSwitches() throws {
    try replay("main.tab-switches")
  }

  func testSignOutRelays() throws {
    try replay("main.sign-out-relays")
  }
}
