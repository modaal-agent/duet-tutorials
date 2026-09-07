// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

#if os(iOS)

import DuetTheming
import UIKit

/// The main theme: every token at the value the config states for it. The
/// vocabularies and the value table are generated from
/// parity/design-tokens.yaml into `Generated/`, as extensions of this class;
/// this file holds what generation cannot decide: the theme type itself, the
/// two asset families the app does not use, and how a type token becomes a
/// face.
public class MainTheme: Themed, Assetable {
  public typealias _ImageAsset = EmptyAsset
  public typealias _GradientAsset = EmptyAsset
}

// MARK: - The type resolver

extension MainTheme {
  /// The generated `fontToken(for:)` states what a token is: family, weight,
  /// size, line height, tracking and the Dynamic Type style it scales
  /// against. Turning that into a face is the app's, because font files are
  /// app resources; this app uses the system face at the token's weight and
  /// design, so it ships no font binaries.
  public func fontSet(for asset: _FontAsset) -> FontSet {
    let token = fontToken(for: asset)
    let scaled = UIFontMetrics(forTextStyle: token.textStyle).scaledFont(for: token.face)
    return FontSet(
      .static(scaled),
      fontMetrics: FontMetrics(
        pointSize: token.size,
        lineHeight: token.lineHeight,
        // The config states tracking as a fraction of the em; `.pct` is a
        // percentage of the point size, the same quantity times 100.
        letterSpacing: .pct(token.trackingEm * 100)
      )
    )
  }
}

extension MainTheme.FontToken {
  /// The unscaled face the token names.
  fileprivate var face: UIFont {
    let system = UIFont.systemFont(ofSize: size, weight: uiWeight)
    guard let design = fontDesign,
      let descriptor = system.fontDescriptor.withDesign(design)
    else {
      return system
    }
    return UIFont(descriptor: descriptor, size: size)
  }

  /// `nil` is the system's default design.
  private var fontDesign: UIFontDescriptor.SystemDesign? {
    switch family {
    case .sans: return nil
    case .serif: return .serif
    case .mono: return .monospaced
    }
  }

  /// The config's weights are the CSS scale (100…900); UIKit names nine
  /// steps over the same range.
  private var uiWeight: UIFont.Weight {
    switch weight {
    case ..<200: return .ultraLight
    case ..<300: return .thin
    case ..<400: return .light
    case ..<500: return .regular
    case ..<600: return .medium
    case ..<700: return .semibold
    case ..<800: return .bold
    case ..<900: return .heavy
    default: return .black
    }
  }
}

#endif
