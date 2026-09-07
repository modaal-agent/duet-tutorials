// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import FoyerKit
import XCTest

/// The upgrade feature's recordings replayed across the framework, one method
/// per recording.
final class UpgradeBoundaryReplayTests: XCTestCase {

  private func replay(_ fixture: String) throws {
    let steps = try BoundaryReplayHarness.replay(fixture: fixture) { initialState in
      try FoyerBoundary.shared.makeSession(feature: "upgrade", initialStateJson: initialState)
    }
    XCTAssertGreaterThan(steps, 0, "the recording must contain steps to replay")
  }

  func testPlansLoadOnce() throws {
    try replay("upgrade.plans-load-once")
  }

  func testSelectConfirmPurchaseDone() throws {
    try replay("upgrade.select-confirm-purchase-done")
  }

  func testBackWalksTheSteps() throws {
    try replay("upgrade.back-walks-the-steps")
  }

  func testPurchaseFailureLands() throws {
    try replay("upgrade.purchase-failure-lands")
  }

  func testBackOnDoneCompletes() throws {
    try replay("upgrade.back-on-done-completes")
  }

  func testDismissFromOutside() throws {
    try replay("upgrade.dismiss-from-outside")
  }
}
