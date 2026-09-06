// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import Combine
import DuetShells
import FoyerBridge
import FoyerKit
import Foundation
import HomeShell
import ProfileShell

public typealias MainKitStore = BridgedStore<MainState, any MainAction>

/// The tab, as the view selects it.
public enum MainTabKey: Hashable {
  case home
  case profile
}

@MainActor
public final class MainViewState: ObservableObject {
  @Published public internal(set) var activeTab: MainTabKey = .home

  public init() {}
}

/// The main level's shell: the tab intent, the tab projection, and the
/// bracket over both tabs, which the Builder attached before activation.
public final class MainViewShell: ViewShell {
  public let viewState = MainViewState()
  public let store: MainKitStore
  public private(set) var home: HomeChild?
  public private(set) var profile: ProfileChild?

  public init(store: MainKitStore) {
    self.store = store
    super.init()
  }

  /// The Builder's one call: the two children, built over the level's Component.
  func attach(home: HomeChild, profile: ProfileChild) {
    self.home = home
    self.profile = profile
  }

  override public func bind() {
    host.adopt(store)
    // Both tabs live for this level's lifetime: activated here, deactivated
    // by the host before the store's effects stop.
    if let home, let profile {
      home.shell.activate()
      profile.shell.activate()
      host.adopt {
        profile.shell.deactivate()
        home.shell.deactivate()
      }
    }
    host.adopt(
      StateTransitions(state: store.$state) { [weak self] _, state in
        self?.apply(state)
      })
  }

  // MARK: - Intents

  public func selectTab(_ tab: MainTabKey) {
    switch tab {
    case .home: store.send(MainActionTabSelected(tab: MainTabHome.shared))
    case .profile: store.send(MainActionTabSelected(tab: MainTabProfile.shared))
    }
  }

  // MARK: - State to view state

  private func apply(_ state: MainState) {
    switch onEnum(of: state.activeTab) {
    case .home: viewState.activeTab = .home
    case .profile: viewState.activeTab = .profile
    }
  }
}
