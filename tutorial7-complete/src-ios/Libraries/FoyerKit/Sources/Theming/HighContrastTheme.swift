// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

#if os(iOS)

import DuetTheming

/// The high-contrast theme, in effect while Increase Contrast is on: every
/// surface collapses onto the page and every label and the card border onto
/// the primary label, so a card is the page's colour with a solid line of
/// ink around it and nothing on it is grey. The type styles are the main
/// theme's. The mapping is exhaustive: a token added to the config is a
/// compile error here until it says where the token maps.
public final class HighContrastTheme: Themed, Assetable {
  public typealias _ImageAsset = EmptyAsset
  public typealias _GradientAsset = EmptyAsset

  private let main = MainTheme()

  public func colorSet(for asset: SemanticColor) -> ColorSet {
    main.colorSet(for: asset.highContrastSource)
  }

  public func fontSet(for asset: SemanticFont) -> FontSet {
    main.fontSet(for: asset)
  }
}

extension SemanticColor {
  /// The main-palette entry a token reads in the high-contrast theme.
  var highContrastSource: SemanticColor {
    switch self {
    case .surface, .cardSurface, .lockedSurface:
      return .surface
    case .labelPrimary, .labelSecondary, .labelLocked, .cardBorder:
      return .labelPrimary
    }
  }
}

#endif
