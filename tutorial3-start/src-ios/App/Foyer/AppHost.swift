// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import FoyerKit
import Foundation
import SplashShell

/// Which screen the app shows. Tutorial 3 replaces this with the root feature's state.
enum AppPhase {
  case splash
  case placeholder(completedBy: SplashCompletionPath)
}

/// The app's host at this step: it mounts the splash, receives the splash's
/// delegate event, tears the splash down and switches to the placeholder.
/// Tutorial 3 replaces it with the root feature and its Builder.
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
      case .completed(let completed) = onEnum(of: event)
    else { return }
    splash?.shell.deactivate()
    splash = nil
    phase = .placeholder(completedBy: completed.path)
  }
}
