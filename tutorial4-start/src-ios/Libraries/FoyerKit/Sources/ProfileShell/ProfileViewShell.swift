// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import AccountShell
import Combine
import DuetShells
import FoyerBridge
import FoyerKit
import Foundation

public typealias ProfileKitStore = BridgedStore<ProfileState, any ProfileAction>

public typealias AccountMount =
  @MainActor (_ displayName: String, _ onDelegate: @escaping (AccountDelegateEvent) -> Void)
    -> AccountChild

@MainActor
public final class ProfileViewState: ObservableObject {
  @Published public internal(set) var displayName = ""
  @Published public internal(set) var account: AccountChild?

  public init() {}
}

public final class ProfileViewShell: ViewShell {
  public let viewState = ProfileViewState()
  public let store: ProfileKitStore
  private let mountAccount: AccountMount
  private var account: ChildSlot<AccountKey, AccountChild>?
  /// The state being applied. The mirror publishes before it assigns, so a
  /// child built during a projection reads this, not `store.state`.
  private var applying: ProfileState?

  private enum AccountKey: Hashable {
    case account
  }

  public init(store: ProfileKitStore, mountAccount: @escaping AccountMount) {
    self.store = store
    self.mountAccount = mountAccount
    super.init()
  }

  override public func bind() {
    host.adopt(store)
    account = host.adopt(
      ChildSlot<AccountKey, AccountChild>(
        build: { [weak self] _ in self?.buildAccount() },
        teardown: { $0.shell.deactivate() }))
    host.adopt(
      StateTransitions(state: store.$state) { [weak self] _, state in
        self?.apply(state)
      })
  }

  private func buildAccount() -> AccountChild {
    let child = mountAccount((applying ?? store.state).displayName) { [weak self] event in
      self?.store.send(ProfileActionAccount(event: event))
    }
    child.shell.activate()
    return child
  }

  // MARK: - Intents

  public func accountTapped() { store.send(ProfileActionAccountTapped.shared) }

  // MARK: - State to view state, and the child mount

  private func apply(_ state: ProfileState) {
    applying = state
    defer { applying = nil }
    viewState.displayName = state.displayName
    account?.reconcile(key: state.child == nil ? nil : .account)
    viewState.account = account?.activeHandle
  }
}
