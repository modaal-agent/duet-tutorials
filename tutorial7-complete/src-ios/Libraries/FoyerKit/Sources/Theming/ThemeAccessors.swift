// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

#if os(iOS)

import DuetTheming
import SwiftUI
import UIKit

// The call-site surface over the generated vocabularies: a view names the
// token, the theme in effect names the value, and parity/design-tokens.yaml
// names what the value is.

public extension ThemeProviding {
  /// A colour token as a SwiftUI `Color`. Under the system appearance the
  /// colour is dynamic, so a view holding it follows a light/dark switch on
  /// its own.
  func color(_ semanticColor: SemanticColor) -> Color {
    Color(color(for: semanticColor, preferredAppearance: nil, on: nil))
  }

  /// A type token resolved for rendering: the face, the line height and the
  /// tracking together. `.font(theme.font(.cardTitle))` applies all three.
  func font(_ semanticFont: SemanticFont) -> ThemedFont {
    let resolved = font(for: semanticFont, preferredAppearance: nil, on: nil)
    return ThemedFont(face: resolved.font, metrics: resolved.metrics)
  }
}

/// One type token, resolved: the face plus the two metrics a `Font` cannot
/// carry. Dynamic Type has already scaled the face by the time a token
/// arrives here, so both metrics are scaled by the same factor.
public struct ThemedFont {
  public let font: Font
  public let lineSpacing: CGFloat
  public let tracking: CGFloat

  fileprivate init(face: UIFont, metrics: FontMetrics) {
    let scale = metrics.pointSize > 0 ? face.pointSize / metrics.pointSize : 1
    let lineHeight = (metrics.lineHeight ?? face.lineHeight) * scale
    self.font = Font(face)
    self.lineSpacing = max(0, lineHeight - face.lineHeight)
    self.tracking = metrics.letterSpacing.toPoints(face.pointSize)
  }
}

public extension View {
  /// Applies a resolved type token: the face, its leading and its tracking.
  /// This overloads SwiftUI's `font(_:)` on the argument type, so the call a
  /// view would write anyway carries the whole token.
  func font(_ themedFont: ThemedFont) -> some View {
    font(themedFont.font)
      .lineSpacing(themedFont.lineSpacing)
      .tracking(themedFont.tracking)
  }
}

#endif
