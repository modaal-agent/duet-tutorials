// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import FoyerKit
import XCTest

/// The progress feature's recordings replayed across the framework, one method
/// per recording.
final class ProgressBoundaryReplayTests: XCTestCase {

  private func replay(_ fixture: String) throws {
    let steps = try BoundaryReplayHarness.replay(fixture: fixture) { initialState in
      try FoyerBoundary.shared.makeSession(feature: "progress", initialStateJson: initialState)
    }
    XCTAssertGreaterThan(steps, 0, "the recording must contain steps to replay")
  }

  func testAppearanceObservesOnce() throws {
    try replay("progress.appearance-observes-once")
  }

  func testPageProjectsDown() throws {
    try replay("progress.page-projects-down")
  }

  func testReadinessArrivesLaterally() throws {
    try replay("progress.readiness-arrives-laterally")
  }
}
