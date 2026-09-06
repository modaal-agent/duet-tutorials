// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import Combine
import DuetShells
import FoyerBridge
import FoyerKit
import Foundation

public typealias HomeKitStore = BridgedStore<HomeState, any HomeAction>

@MainActor
public final class HomeViewState: ObservableObject {
  @Published public internal(set) var items: [Item] = []
  @Published public internal(set) var isLoading = false

  public init() {}
}

public final class HomeViewShell: ViewShell {
  public let viewState = HomeViewState()
  public let store: HomeKitStore

  public init(store: HomeKitStore) {
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

  // MARK: - Intents

  public func appeared() { store.send(HomeActionAppeared.shared) }

  // MARK: - State to view state

  private func apply(_ state: HomeState) {
    viewState.items = state.items
    viewState.isLoading = state.isLoading
  }
}
