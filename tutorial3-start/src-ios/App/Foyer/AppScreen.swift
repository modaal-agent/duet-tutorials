// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import FoyerKit
import SplashShell
import SwiftUI

/// The render layer: one screen chosen from the host's phase.
struct AppScreen: View {
  @ObservedObject var app: AppHost

  var body: some View {
    switch app.phase {
    case .splash:
      if let splash = app.splash {
        SplashView(viewState: splash.shell.viewState, shell: splash.shell)
      }
    case .placeholder(let completedBy):
      PlaceholderView(completedBy: completedBy)
    }
  }
}

/// The screen after the splash. Tutorial 3 replaces it with the sign-in gate.
private struct PlaceholderView: View {
  let completedBy: SplashCompletionPath

  private var path: String {
    switch onEnum(of: completedBy) {
    case .ceremony: "the ceremony"
    case .safetyNet: "the safety net"
    }
  }

  var body: some View {
    VStack(spacing: 12) {
      Text("Foyer")
        .font(.largeTitle)
      Text("Splash completed by \(path).")
        .font(.body)
      Text("Tutorial 3 puts the sign-in gate here.")
        .font(.callout)
        .foregroundStyle(.secondary)
        .multilineTextAlignment(.center)
    }
    .padding(32)
    .frame(maxWidth: .infinity, maxHeight: .infinity)
  }
}
