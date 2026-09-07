// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import FoyerKit
import XCTest

/// The root feature's recordings replayed across the framework, one method
/// per recording.
final class RootBoundaryReplayTests: XCTestCase {

  private func replay(_ fixture: String) throws {
    let steps = try BoundaryReplayHarness.replay(fixture: fixture) { initialState in
      try FoyerBoundary.shared.makeSession(feature: "root", initialStateJson: initialState)
    }
    XCTAssertGreaterThan(steps, 0, "the recording must contain steps to replay")
  }

  func testSplashBeforeAuthHolds() throws {
    try replay("root.splash-before-auth-holds")
  }

  func testGateAfterSplash() throws {
    try replay("root.gate-after-splash")
  }

  func testGatePicksOnboarding() throws {
    try replay("root.gate-picks-onboarding")
  }

  func testOnboardingCompletesIntoMain() throws {
    try replay("root.onboarding-completes-into-main")
  }

  func testLateSplashInert() throws {
    try replay("root.late-splash-inert")
  }

  func testSignedInSkipsGate() throws {
    try replay("root.signed-in-skips-gate")
  }

  func testSignOutReturnsToGate() throws {
    try replay("root.sign-out-returns-to-gate")
  }

  func testSessionExpiryReturnsToGate() throws {
    try replay("root.session-expiry-returns-to-gate")
  }

  func testEntitlementChanges() throws {
    try replay("root.entitlement-changes")
  }

  func testLinkForwardsUnderMain() throws {
    try replay("root.link-forwards-under-main")
  }

  func testLinkWaitsForMain() throws {
    try replay("root.link-waits-for-main")
  }
}
