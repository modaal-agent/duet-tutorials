// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import FoyerKit
import XCTest

/// The account feature's recordings replayed across the framework, one method
/// per recording.
final class AccountBoundaryReplayTests: XCTestCase {

  private func replay(_ fixture: String) throws {
    let steps = try BoundaryReplayHarness.replay(fixture: fixture) { initialState in
      try FoyerBoundary.shared.makeSession(feature: "account", initialStateJson: initialState)
    }
    XCTAssertGreaterThan(steps, 0, "the recording must contain steps to replay")
  }

  func testEditNameSavesAndClimbs() throws {
    try replay("account.edit-name-saves-and-climbs")
  }

  func testEditNameCancels() throws {
    try replay("account.edit-name-cancels")
  }

  func testSignOutRequestsOnce() throws {
    try replay("account.sign-out-requests-once")
  }

  func testCloseClimbs() throws {
    try replay("account.close-climbs")
  }
}
