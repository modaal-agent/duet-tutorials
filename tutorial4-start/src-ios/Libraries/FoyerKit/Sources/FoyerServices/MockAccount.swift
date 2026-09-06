// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import FoyerKit
import Foundation

/// The account port as a mock service: the saved name is held in memory.
public final class MockAccount: NSObject, AccountPort {
  public private(set) var displayName: String?

  public override init() {}

  public func saveDisplayName(name: String, onSaved: @escaping () -> Void) {
    displayName = name
    onSaved()
  }
}
