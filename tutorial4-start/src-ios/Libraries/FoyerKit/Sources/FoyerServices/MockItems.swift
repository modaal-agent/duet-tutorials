// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import FoyerKit
import Foundation

/// The items port as a mock service: a fixed list of twelve rows. The Kotlin
/// mock carries the same twelve; the two lists are duplicated on purpose for
/// the mocks' short life.
public final class MockItems: NSObject, ItemsPort {
  public override init() {}

  public func items(onItems: @escaping ([Item]) -> Void) {
    onItems(Self.canned)
  }

  private static let canned: [Item] = [
    "Welcome note",
    "Getting started",
    "Your first week",
    "Reading list",
    "Saved for later",
    "Shared with you",
    "Recently viewed",
    "Drafts",
    "Archive",
    "Highlights",
    "Notes to self",
    "Everything else",
  ].enumerated().map { index, title in Item(id: String(index + 1), title: title) }
}
