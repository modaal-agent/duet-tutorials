// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import FoyerKit
import XCTest

/// The name feature's recordings replayed across the framework, one method
/// per recording.
final class NameBoundaryReplayTests: XCTestCase {

  private func replay(_ fixture: String) throws {
    let steps = try BoundaryReplayHarness.replay(fixture: fixture) { initialState in
      try FoyerBoundary.shared.makeSession(feature: "name", initialStateJson: initialState)
    }
    XCTAssertGreaterThan(steps, 0, "the recording must contain steps to replay")
  }

  func testValidDraftPublishesReadiness() throws {
    try replay("name.valid-draft-publishes-readiness")
  }

  func testEmptyNameRejected() throws {
    try replay("name.empty-name-rejected")
  }

  func testContinueClimbs() throws {
    try replay("name.continue-climbs")
  }
}
