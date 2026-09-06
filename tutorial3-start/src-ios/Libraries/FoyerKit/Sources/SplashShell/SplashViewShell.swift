// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import Combine
import DuetShells
import FoyerBridge
import FoyerKit
import Foundation

// The Swift shell over the splash's Kotlin core. The reducer, the effect loop
// and the environment dispatch run inside the linked framework; what is Swift
// here is shell duty only: intents in, view state out, and the bracket.

/// The hosted store's Swift face. State and Action are the bridged Kotlin
/// types; the runtime behind `send` is the Kotlin kernel's.
public typealias SplashKitStore = BridgedStore<SplashState, any SplashAction>

// MARK: - View state (the render projection)

@MainActor
public final class SplashViewState: ObservableObject {
  /// Whether the safety net is armed. The view shows a progress line while it is.
  @Published public internal(set) var isArmed = false

  public init() {}
}

// MARK: - Shell

/// Three duties and no fourth: turn intents into actions, project state into
/// view state, and own nothing the host should own.
public final class SplashViewShell: ViewShell {
  public let viewState = SplashViewState()
  public let store: SplashKitStore

  public init(store: SplashKitStore) {
    self.store = store
    super.init()
  }

  override public func bind() {
    // Registered first so it unwinds last: whatever this shell adopts on top
    // of the store stops before the store's `cancel()` ends its effects.
    // Adopting the mirror is also what makes `deactivate()` stop the Kotlin
    // runtime: its `cancel()` is the store's teardown plus its scope's.
    host.adopt(store)

    // State to view state. `StateTransitions` replays the current value on
    // subscription, so this adoption is also the initial projection.
    host.adopt(
      StateTransitions(state: store.$state) { [weak self] _, state in
        self?.apply(state)
      })
  }

  // MARK: - Intents (the view calls these)

  public func appeared() { store.send(SplashActionAppeared.shared) }
  public func ceremonyFinished() { store.send(SplashActionCeremonyFinished.shared) }

  // MARK: - State to view state

  private func apply(_ state: SplashState) {
    viewState.isArmed = state.isArmed
  }
}
