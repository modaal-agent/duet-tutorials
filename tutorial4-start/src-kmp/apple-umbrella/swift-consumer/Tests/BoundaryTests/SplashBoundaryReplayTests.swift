// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import FoyerKit
import XCTest

/// The splash feature's four recordings, the same files the Kotlin lane
/// replays, replayed across the framework. A green run shows that the
/// reducer reached Swift intact and that the module is exported from the
/// framework rather than only linked into it. One method per recording; a
/// recording without a row here is a recording this suite does not check.
final class SplashBoundaryReplayTests: XCTestCase {

  private func replay(_ fixture: String) throws {
    let steps = try BoundaryReplayHarness.replay(fixture: fixture) { initialState in
      try FoyerBoundary.shared.makeSession(feature: "splash", initialStateJson: initialState)
    }
    XCTAssertGreaterThan(steps, 0, "the recording must contain steps to replay")
  }

  func testCeremonyCompletes() throws {
    try replay("splash.ceremony-completes")
  }

  func testSafetyNetFires() throws {
    try replay("splash.safety-net-fires")
  }

  func testBothPathsNotifyTwice() throws {
    try replay("splash.both-paths-notify-twice")
  }

  func testRepeatAppearInert() throws {
    try replay("splash.repeat-appear-inert")
  }
}
