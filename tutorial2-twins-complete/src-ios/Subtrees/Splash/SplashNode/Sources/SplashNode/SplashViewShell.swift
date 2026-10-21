// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import Combine
import Duet
import DuetShells
import SplashFeature

// The Swift shell over the splash's Swift twin. The store is Duet's Swift
// `Store`: it reduces synchronously on the main actor and publishes its state,
// so the shell observes it directly.

/// The store the shell hosts.
public typealias SplashStore = Store<SplashState, SplashAction, SplashEffectPayload>

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
  public let store: SplashStore

  public init(store: SplashStore) {
    self.store = store
    super.init()
  }

  override public func bind() {
    // Registered first so it unwinds last: the projection below stops before
    // the store's teardown cancels its effects.
    host.host(store)

    // State to view state. `StateTransitions` replays the current value on
    // subscription, so this adoption is also the initial projection.
    host.adopt(
      StateTransitions(state: store.$state) { [weak self] _, state in
        self?.apply(state)
      })
  }

  // MARK: - Intents (the view calls these)

  public func appeared() { store.send(.appeared) }
  public func ceremonyFinished() { store.send(.ceremonyFinished) }

  // MARK: - State to view state

  private func apply(_ state: SplashState) {
    viewState.isArmed = state.isArmed
  }
}
