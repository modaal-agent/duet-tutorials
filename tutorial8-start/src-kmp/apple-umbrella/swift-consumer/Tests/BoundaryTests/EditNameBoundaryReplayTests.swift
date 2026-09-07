// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import FoyerKit
import XCTest

/// The editname feature's recordings replayed across the framework, one method
/// per recording.
final class EditNameBoundaryReplayTests: XCTestCase {

  private func replay(_ fixture: String) throws {
    let steps = try BoundaryReplayHarness.replay(fixture: fixture) { initialState in
      try FoyerBoundary.shared.makeSession(feature: "editname", initialStateJson: initialState)
    }
    XCTAssertGreaterThan(steps, 0, "the recording must contain steps to replay")
  }

  func testValidNameSaves() throws {
    try replay("editname.valid-name-saves")
  }

  func testEmptyNameRejected() throws {
    try replay("editname.empty-name-rejected")
  }

  func testLongNameRejected() throws {
    try replay("editname.long-name-rejected")
  }

  func testCancelCloses() throws {
    try replay("editname.cancel-closes")
  }
}
