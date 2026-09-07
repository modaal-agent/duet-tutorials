// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

#if os(iOS)

import DuetTheming

/// The registry: a theme is a key, and `themed()` names the catalog behind
/// it. Both conformances resolve by cast at the first lookup, so they live
/// together in this one file.
extension Theme: @retroactive Themeable {
  public static let mainTheme = Theme(key: "mainTheme")
  public static let highContrast = Theme(key: "highContrast")

  public func themed() -> Themed {
    switch self {
    case .mainTheme:
      return MainTheme()
    case .highContrast:
      return HighContrastTheme()
    default:
      fatalError("no theme is registered under the key \(key)")
    }
  }
}

/// The theme a tree renders with when no scope published one: Xcode previews
/// and unhosted trees.
extension ThemeDefaults: @retroactive ThemeDefaulting {
  public func defaultTheme() -> Theme {
    .mainTheme
  }
}

#endif
