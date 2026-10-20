// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import Duet
import SplashFeature

/// The live environment: the wall clock and a closure to the host.
struct LiveSplashEnvironment: SplashEnvironment {
  let onDelegate: (SplashDelegateEvent) -> Void

  var clock: any KernelClock { LiveClock() }

  func notifyHost(_ event: SplashDelegateEvent) {
    onDelegate(event)
  }
}

// MARK: - Builder

/// What a mount owns. The shell owns the store's lifetime, and the mount's
/// bracket is `shell.activate()` / `shell.deactivate()`.
public final class SplashChild {
  public let shell: SplashViewShell

  init(shell: SplashViewShell) {
    self.shell = shell
  }
}

/// Builds one splash mount: store and shell.
public final class SplashBuilder {
  public init() {}

  @MainActor
  public func buildSplash(
    onDelegate: @escaping (SplashDelegateEvent) -> Void
  ) -> SplashChild {
    let store = makeSplashStore(environment: LiveSplashEnvironment(onDelegate: onDelegate))
    return SplashChild(shell: SplashViewShell(store: store))
  }
}
