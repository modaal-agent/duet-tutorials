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

  func testHomeOpensTheUpgradeFlow() throws {
    try replay("main.home-opens-the-upgrade-flow")
  }

  func testProfileOpensTheUpgradeFlow() throws {
    try replay("main.profile-opens-the-upgrade-flow")
  }

  func testLinkOpensTheUpgradeFlow() throws {
    try replay("main.link-opens-the-upgrade-flow")
  }

  func testLinkTravelsToTheProfileTab() throws {
    try replay("main.link-travels-to-the-profile-tab")
  }
}
