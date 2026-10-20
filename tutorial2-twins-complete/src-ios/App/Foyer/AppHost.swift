// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import Foundation
import SplashFeature
import SplashNode

/// Which screen the app shows.
enum AppPhase {
  case splash
  case placeholder(completedBy: SplashCompletionPath)
}

/// The app's host: it mounts the splash, receives the splash's delegate
/// event, tears the splash down and switches to the placeholder.
@MainActor
final class AppHost: ObservableObject {
  @Published private(set) var phase: AppPhase = .splash

  /// The splash mount: built here, torn down when the splash completes.
  private(set) var splash: SplashChild?

  init() {
    splash = SplashBuilder().buildSplash { [weak self] event in
      self?.splashCompleted(event)
    }
  }

  /// The mount's bracket opens once the window is on screen.
  func activate() {
    splash?.shell.activate()
  }

  /// Logical destruction: the scene disconnected.
  func teardown() {
    splash?.shell.deactivate()
    splash = nil
  }

  private func splashCompleted(_ event: SplashDelegateEvent) {
    // Both completion paths notify every time; the first notification moves
    // the app on and the rest arrive after the splash is gone.
    guard case .splash = phase,
      case .completed(let path) = event
    else { return }
    splash?.shell.deactivate()
    splash = nil
    phase = .placeholder(completedBy: path)
  }
}
