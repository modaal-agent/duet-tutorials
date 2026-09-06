// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import XCTest

/// Poll the delivery an assertion depends on, in 20 ms slices against a
/// ceiling. Real time, deliberately: no test dispatcher crosses the
/// boundary. A passing row returns on the first satisfied poll.
@MainActor
func settle(
  until reached: @autoclosure @MainActor () -> Bool,
  _ what: @autoclosure () -> String,
  withinSeconds seconds: Double = 5,
  file: StaticString = #filePath,
  line: UInt = #line
) async {
  for _ in 0..<Int(seconds * 50) {
    if reached() { return }
    try? await Task.sleep(nanoseconds: 20_000_000)
  }
  XCTFail("settle: \(what()) never happened within \(seconds)s", file: file, line: line)
}
