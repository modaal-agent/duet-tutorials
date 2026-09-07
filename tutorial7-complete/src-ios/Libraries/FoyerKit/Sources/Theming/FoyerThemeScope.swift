// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

#if os(iOS)

import DuetTheming
import SwiftUI

/// Publishes the theme the system's settings select to the SwiftUI tree
/// below: the main theme, or the high-contrast theme while Increase Contrast
/// is on. A change of the setting publishes the other provider, and every
/// view that reads the theme re-renders.
public struct FoyerThemeScope<Content: View>: View {
  @Environment(\.colorSchemeContrast) private var contrast
  private let content: Content

  public init(@ViewBuilder content: () -> Content) {
    self.content = content()
  }

  public var body: some View {
    ThemeScope(contrast == .increased ? FoyerThemes.highContrast : FoyerThemes.main) { content }
  }
}

/// The app's two providers, built once. The theme follows the system
/// setting, so nothing is persisted: `Unpersisted` answers every read with
/// nil, and neither provider is ever asked to change its theme.
@MainActor
enum FoyerThemes {
  static let main = provider(for: .mainTheme)
  static let highContrast = provider(for: .highContrast)

  private static func provider(for theme: Theme) -> ThemeProvider {
    ThemeProvider(persistentStorage: Unpersisted(), defaultTheme: theme, defaultPreferredAppearance: .system)
  }
}

private struct Unpersisted: ThemeProviderPersisting {
  func get<T: Codable>(_ type: T.Type, key: String) -> T? { nil }
  func set<T: Codable>(_ value: T, key: String) {}
}

#endif
