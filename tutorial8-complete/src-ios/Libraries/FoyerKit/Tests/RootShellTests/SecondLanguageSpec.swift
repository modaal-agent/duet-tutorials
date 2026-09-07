// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import Foundation
import XCTest

/// The second language is complete in every catalog: each key has a German
/// entry marked translated, every plural category of the source language is
/// covered, and the format specifiers match the source language's. The
/// catalogs are read from the package tree through `#filePath`, so the test
/// sees the files the generator reads, not a compiled bundle. Xcode's
/// editor marks a missing translation; nothing else does, which is why a
/// test holds the catalogs to it.
final class SecondLanguageSpec: XCTestCase {
  private static let sources = URL(fileURLWithPath: #filePath)
    .deletingLastPathComponent()  // this file
    .deletingLastPathComponent()  // RootShellTests
    .deletingLastPathComponent()  // Tests
    .appendingPathComponent("Sources")

  func testEveryKeyInEveryCatalogHasATranslatedGermanEntry() throws {
    let catalogs = try FileManager.default
      .contentsOfDirectory(at: Self.sources, includingPropertiesForKeys: nil)
      .map { $0.appendingPathComponent("Localizable.xcstrings") }
      .filter { FileManager.default.fileExists(atPath: $0.path) }
    XCTAssertEqual(catalogs.count, 9, "one catalog per shell target")

    for catalog in catalogs {
      let name = catalog.deletingLastPathComponent().lastPathComponent
      let document = try JSONSerialization.jsonObject(with: Data(contentsOf: catalog)) as! [String: Any]
      let strings = document["strings"] as! [String: [String: Any]]
      for (key, entry) in strings {
        let localizations = entry["localizations"] as! [String: [String: Any]]
        let source = try XCTUnwrap(localizations["en"], "\(name) \(key): no source entry")
        let german = try XCTUnwrap(localizations["de"], "\(name) \(key): no German entry")
        for (category, unit) in units(of: source) {
          let translated = try XCTUnwrap(units(of: german)[category], "\(name) \(key): no German \(category)")
          XCTAssertEqual(translated["state"] as? String, "translated", "\(name) \(key) \(category)")
          XCTAssertEqual(
            try specifiers(unit["value"] as! String), try specifiers(translated["value"] as! String),
            "\(name) \(key) \(category): format specifiers")
        }
      }
    }
  }

  /// The string units of one localization: one for a plain string, one per
  /// plural category for a plural, keyed by the category.
  private func units(of localization: [String: Any]) -> [String: [String: Any]] {
    if let unit = localization["stringUnit"] as? [String: Any] { return ["string": unit] }
    let plural = (localization["variations"] as! [String: Any])["plural"] as! [String: [String: Any]]
    return plural.mapValues { $0["stringUnit"] as! [String: Any] }
  }

  /// The format specifiers of one value, in order: `%lld`, `%@`.
  private func specifiers(_ value: String) throws -> [String] {
    try value.ranges(of: Regex("%(lld|@)")).map { String(value[$0]) }
  }
}
