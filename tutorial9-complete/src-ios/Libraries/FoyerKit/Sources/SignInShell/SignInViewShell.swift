// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import Combine
import DuetShells
import FoyerBridge
import FoyerKit
import Foundation

/// The hosted store's Swift face: bridged Kotlin State and Action types.
public typealias SignInKitStore = BridgedStore<SignInState, any SignInAction>

// MARK: - View state (the render projection)

@MainActor
public final class SignInViewState: ObservableObject {
  @Published public internal(set) var isSigningIn = false
  @Published public internal(set) var failure: String?

  public init() {}
}

// MARK: - Shell

/// Three duties and no fourth: intents in, view state out, the bracket.
public final class SignInViewShell: ViewShell {
  public let viewState = SignInViewState()
  public let store: SignInKitStore

  public init(store: SignInKitStore) {
    self.store = store
    super.init()
  }

  override public func bind() {
    host.adopt(store)
    host.adopt(
      StateTransitions(state: store.$state) { [weak self] _, state in
        self?.apply(state)
      })
  }

  // MARK: - Intents (the view calls these)

  public func continueWithEmail(_ address: String) {
    store.send(SignInActionContinueTapped(provider: SignInProviderEmail(address: address)))
  }

  public func continueAsGuest() {
    store.send(SignInActionContinueTapped(provider: SignInProviderGuest.shared))
  }

  // MARK: - State to view state

  private func apply(_ state: SignInState) {
    viewState.isSigningIn = state.isSigningIn
    viewState.failure = state.failure
  }
}
