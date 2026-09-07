// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import Combine
import DuetShells
import FoyerBridge
import FoyerKit
import Foundation

public typealias HomeKitStore = BridgedStore<HomeState, any HomeAction>

/// What the tab presents over the list, as the view switches on it.
public enum HomePresented: Equatable {
  case promo
  case insights
}

@MainActor
public final class HomeViewState: ObservableObject {
  @Published public internal(set) var items: [Item] = []
  @Published public internal(set) var isLoading = false
  /// The card's treatment: locked while the projected entitlement is Free.
  @Published public internal(set) var isInsightsLocked = true
  @Published public internal(set) var presented: HomePresented?

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

  public func insightsTapped() { store.send(HomeActionInsightsTapped.shared) }

  public func upgradeTapped() { store.send(HomeActionUpgradeTapped.shared) }

  public func dismissed() { store.send(HomeActionDismissed.shared) }

  /// The parent's projection: the root's entitlement slice, as this tab's action.
  public func entitlementChanged(_ entitlement: Entitlement) {
    store.send(HomeActionEntitlementChanged(entitlement: entitlement))
  }

  // MARK: - State to view state

  private func apply(_ state: HomeState) {
    viewState.items = state.items
    viewState.isLoading = state.isLoading
    if case .free = onEnum(of: state.entitlement) {
      viewState.isInsightsLocked = true
    } else {
      viewState.isInsightsLocked = false
    }
    viewState.presented =
      switch state.presented.map(onEnum(of:)) {
      case .promo: .promo
      case .insights: .insights
      case nil: nil
      }
  }
}
