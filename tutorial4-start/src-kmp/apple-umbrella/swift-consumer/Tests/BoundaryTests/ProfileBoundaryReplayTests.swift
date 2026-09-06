// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import FoyerKit
import XCTest

/// The profile feature's recordings replayed across the framework, one method
/// per recording.
final class ProfileBoundaryReplayTests: XCTestCase {

  private func replay(_ fixture: String) throws {
    let steps = try BoundaryReplayHarness.replay(fixture: fixture) { initialState in
      try FoyerBoundary.shared.makeSession(feature: "profile", initialStateJson: initialState)
    }
    XCTAssertGreaterThan(steps, 0, "the recording must contain steps to replay")
  }

  func testAccountCloses() throws {
    try replay("profile.account-closes")
  }

  func testNameChangeUpdatesHeader() throws {
    try replay("profile.name-change-updates-header")
  }

  func testSignOutRelays() throws {
    try replay("profile.sign-out-relays")
  }
}
