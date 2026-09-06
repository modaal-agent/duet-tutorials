// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import FoyerBridge
import FoyerKit
import Foundation

/// The live environment, implemented in Swift against the bridged Kotlin
/// interface. The framework exports the interface as an Objective-C protocol,
/// so an `NSObject` subclass conforms and the Kotlin effect handler calls
/// back into these members.
final class LiveSplashEnvironment: NSObject, SplashEnvironment {
  private let onDelegate: (SplashDelegateEvent) -> Void

  init(onDelegate: @escaping (SplashDelegateEvent) -> Void) {
    self.onDelegate = onDelegate
  }

  /// The wall clock, the Kotlin object. Under `runTest` the Kotlin lane
  /// swaps in virtual time; the app runs on this.
  var clock: KernelClock { LiveClock.shared }

  func notifyHost(event: SplashDelegateEvent) {
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

/// Builds one splash mount: scope, Kotlin store, Swift mirror, shell. The
/// same wiring every bridged feature repeats.
public final class SplashBuilder {
  public init() {}

  @MainActor
  public func buildSplash(
    onDelegate: @escaping (SplashDelegateEvent) -> Void
  ) -> SplashChild {
    let scope = mainImmediateStoreScope()
    let store = makeSplashStore(
      environment: LiveSplashEnvironment(onDelegate: onDelegate),
      scope: scope)
    let bridged = SplashKitStore(
      state: splashStateFlow(store: store),
      send: { store.send(action: $0) },
      teardown: {
        store.teardown()
        cancelStoreScope(scope: scope)
      })
    return SplashChild(shell: SplashViewShell(store: bridged))
  }
}
