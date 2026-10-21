// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import SplashFeature
import SplashNode
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

/// The screen after the splash.
private struct PlaceholderView: View {
  let completedBy: SplashCompletionPath

  private var path: String {
    switch completedBy {
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
    }
    .padding(32)
    .frame(maxWidth: .infinity, maxHeight: .infinity)
  }
}
