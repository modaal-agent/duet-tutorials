// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import FoyerKit
import Foundation
import XCTest

/// Replays one fixture file through a boundary session and compares each
/// step's state and effects with the file, byte for byte. The expected side
/// goes through the core's canonical writer too, so the comparison is between
/// two outputs of the same writer and never a formatting test.
enum BoundaryReplayHarness {

  /// Drives one fixture through `makeSession`, returning the step count.
  static func replay(
    fixture: String,
    session makeSession: (String) throws -> ReplaySession,
    file: StaticString = #filePath,
    line: UInt = #line
  ) throws -> Int {
    let url = fixturesDirectory.appendingPathComponent("\(fixture).fixture.json")
    let document =
      try JSONSerialization.jsonObject(with: Data(contentsOf: url)) as! [String: Any]
    let session = try makeSession(try compactString(document["initialState"]!))

    var replayed = 0
    for rawStep in document["steps"] as! [[String: Any]] {
      let label = rawStep["label"] as? String ?? "step \(replayed)"
      let result = session.step(actionJson: try compactString(rawStep["action"]!))
      let expectedState = try FoyerBoundary.shared.canonicalize(
        rawJson: try compactString(rawStep["expectedState"]!))
      let expectedEffects = try FoyerBoundary.shared.canonicalize(
        rawJson: try compactString(rawStep["expectedEffects"]!))
      XCTAssertEqual(
        result.stateCanonical, expectedState, "\(fixture)/\(label): state diverged",
        file: file, line: line)
      XCTAssertEqual(
        result.effectsCanonical, expectedEffects, "\(fixture)/\(label): effects diverged",
        file: file, line: line)
      replayed += 1
    }
    return replayed
  }

  static func compactString(_ tree: Any) throws -> String {
    String(
      decoding: try JSONSerialization.data(withJSONObject: tree, options: [.fragmentsAllowed]),
      as: UTF8.self)
  }

  /// `parity/fixtures/` at the repository root: the one corpus both lanes replay.
  static var fixturesDirectory: URL {
    URL(fileURLWithPath: #filePath)
      .deletingLastPathComponent()  // BoundaryTests/
      .deletingLastPathComponent()  // Tests/
      .deletingLastPathComponent()  // swift-consumer/
      .deletingLastPathComponent()  // apple-umbrella/
      .deletingLastPathComponent()  // src-kmp/
      .deletingLastPathComponent()  // the repository root
      .appendingPathComponent("parity/fixtures")
  }
}
