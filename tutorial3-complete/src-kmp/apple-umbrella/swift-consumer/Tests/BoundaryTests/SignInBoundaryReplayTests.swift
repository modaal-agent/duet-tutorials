// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import FoyerKit
import XCTest

/// The signin feature's recordings replayed across the framework, one method
/// per recording.
final class SignInBoundaryReplayTests: XCTestCase {

  private func replay(_ fixture: String) throws {
    let steps = try BoundaryReplayHarness.replay(fixture: fixture) { initialState in
      try FoyerBoundary.shared.makeSession(feature: "signin", initialStateJson: initialState)
    }
    XCTAssertGreaterThan(steps, 0, "the recording must contain steps to replay")
  }

  func testEmailSignsIn() throws {
    try replay("signin.email-signs-in")
  }

  func testGuestSignsIn() throws {
    try replay("signin.guest-signs-in")
  }

  func testEmptyAddressFails() throws {
    try replay("signin.empty-address-fails")
  }

  func testPortFailureLands() throws {
    try replay("signin.port-failure-lands")
  }
}
