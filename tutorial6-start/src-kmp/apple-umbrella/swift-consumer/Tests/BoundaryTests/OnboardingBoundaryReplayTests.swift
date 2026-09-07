// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import FoyerKit
import XCTest

/// The onboarding feature's recordings replayed across the framework, one method
/// per recording.
final class OnboardingBoundaryReplayTests: XCTestCase {

  private func replay(_ fixture: String) throws {
    let steps = try BoundaryReplayHarness.replay(fixture: fixture) { initialState in
      try FoyerBoundary.shared.makeSession(feature: "onboarding", initialStateJson: initialState)
    }
    XCTAssertGreaterThan(steps, 0, "the recording must contain steps to replay")
  }

  func testStepsAdvanceToCompletion() throws {
    try replay("onboarding.steps-advance-to-completion")
  }

  func testBackWalksThePages() throws {
    try replay("onboarding.back-walks-the-pages")
  }

  func testBackOnWelcomeInert() throws {
    try replay("onboarding.back-on-welcome-inert")
  }
}
