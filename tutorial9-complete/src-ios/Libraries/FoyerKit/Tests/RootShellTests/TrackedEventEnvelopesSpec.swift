// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import DuetTelemetry
import Foundation
import XCTest

/// The envelope fixtures, read by the other language. Every `track` effect a
/// Kotlin reducer emitted is in a recording under parity/fixtures in the
/// grammar's wire form; this spec decodes each one with the Swift twin's
/// `Codable`, holds the set of vendor-facing names to the seven the app
/// declares, and re-encodes each event to the same JSON. A recording written
/// by the Kotlin encoders and read back unchanged by the Swift ones is what
/// lets one declaration serve both apps. The path climbs from this file to
/// the tree.
final class TrackedEventEnvelopesSpec: XCTestCase {

  private let fixtures = URL(fileURLWithPath: #filePath)
    .deletingLastPathComponent().deletingLastPathComponent().deletingLastPathComponent()
    .deletingLastPathComponent().deletingLastPathComponent().deletingLastPathComponent()
    .appendingPathComponent("parity/fixtures")

  private static let declared: Set<String> = [
    "Splash Completed",
    "Session Signed In",
    "Onboarding Completed",
    "Promo Viewed",
    "Upgrade Completed",
    "Name Edited",
    "Session Signed Out",
  ]

  func testTheRecordingsEnvelopesDecodeWithTheSwiftTwinAndNameTheDeclaredEvents() throws {
    let files = try FileManager.default.contentsOfDirectory(atPath: fixtures.path)
      .filter { $0.hasSuffix(".fixture.json") }.sorted()
    XCTAssertGreaterThan(files.count, 70, "the tree's recordings are on disk")

    var names: Set<String> = []
    var envelopes = 0
    for file in files {
      let data = try Data(contentsOf: fixtures.appendingPathComponent(file))
      let document = try JSONSerialization.jsonObject(with: data)
      for envelope in Self.trackEnvelopes(in: document) {
        envelopes += 1
        let bytes = try JSONSerialization.data(withJSONObject: envelope)
        let event = try JSONDecoder().decode(TrackedEvent.self, from: bytes)
        names.insert(event.encodedName())
        // The round trip: what Kotlin wrote, Swift writes back the same.
        let again = try JSONSerialization.jsonObject(with: JSONEncoder().encode(event))
        XCTAssertEqual(
          again as? NSDictionary, envelope as? NSDictionary, "\(file): the Swift twin re-encodes the envelope unchanged")
      }
    }
    XCTAssertGreaterThan(envelopes, 0)
    XCTAssertEqual(names, Self.declared, "the names in the recordings")
  }

  /// Every `{"case": "track", "value": {"event": …}}` envelope under one element.
  private static func trackEnvelopes(in element: Any) -> [Any] {
    if let object = element as? [String: Any] {
      if object["case"] as? String == "track", let value = object["value"] as? [String: Any],
        let event = value["event"]
      {
        return [event]
      }
      return object.values.flatMap { trackEnvelopes(in: $0) }
    }
    if let array = element as? [Any] {
      return array.flatMap { trackEnvelopes(in: $0) }
    }
    return []
  }
}
