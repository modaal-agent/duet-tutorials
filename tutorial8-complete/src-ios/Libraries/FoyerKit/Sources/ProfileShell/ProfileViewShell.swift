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
  @MainActor (
    _ displayName: String, _ child: AccountRoute?,
    _ onDelegate: @escaping (AccountDelegateEvent) -> Void
  ) -> AccountChild

@MainActor
public final class ProfileViewState: ObservableObject {
  @Published public internal(set) var displayName = ""
  /// The plan row's value, from the projected entitlement. The view names it.
  @Published public internal(set) var plan = PlanLabel.free
  @Published public internal(set) var account: AccountChild?

  public init() {}
}

public final class ProfileViewShell: ViewShell {
  public let viewState = ProfileViewState()
  public let store: ProfileKitStore
  private let mountAccount: AccountMount
  private var account: ChildSlot<AccountKey, AccountChild>?
  /// The editor the restored route names; consumed by the first account mount.
  private var restoredEditor: Bool
  /// The state being applied. The mirror publishes before it assigns, so a
  /// child built during a projection reads this, not `store.state`.
  private var applying: ProfileState?

  private enum AccountKey: Hashable {
    case account
  }

  public init(store: ProfileKitStore, restoredEditor: Bool, mountAccount: @escaping AccountMount) {
    self.store = store
    self.restoredEditor = restoredEditor
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
    let editor: AccountRoute? = restoredEditor ? AccountRouteEditName.shared : nil
    restoredEditor = false
    let child = mountAccount((applying ?? store.state).displayName, editor) { [weak self] event in
      self?.store.send(ProfileActionAccount(event: event))
    }
    child.shell.activate()
    return child
  }

  // MARK: - Intents

  public func accountTapped() { store.send(ProfileActionAccountTapped.shared) }

  public func planTapped() { store.send(ProfileActionPlanTapped.shared) }

  /// The parent's forward: a deep link the main level handed down.
  public func openLink(_ link: DeepLink) { store.send(ProfileActionOpenLink(link: link)) }

  /// The tree's depth as one route sliver, for the spine.
  public var routePath: ProfilePath? {
    guard let account = viewState.account else { return nil }
    return account.shell.viewState.editor == nil ? ProfilePathAccount.shared : ProfilePathEditName.shared
  }

  /// The parent's projection: the root's entitlement slice, as this tab's action.
  public func entitlementChanged(_ entitlement: Entitlement) {
    store.send(ProfileActionEntitlementChanged(entitlement: entitlement))
  }

  // MARK: - State to view state, and the child mount

  private func apply(_ state: ProfileState) {
    applying = state
    defer { applying = nil }
    viewState.displayName = state.displayName
    viewState.plan = planLabel(of: state.entitlement)
    account?.reconcile(key: state.child == nil ? nil : .account)
    viewState.account = account?.activeHandle
  }

  private func planLabel(of entitlement: Entitlement) -> PlanLabel {
    switch onEnum(of: entitlement) {
    case .free: .free
    case .premium(let premium):
      switch onEnum(of: premium.plan) {
      case .monthly: .premiumMonthly
      case .yearly: .premiumYearly
      }
    }
  }
}

/// The plan row's value, as the view names it. The Kotlin `Entitlement`
/// crosses the bridge as a class; this key is what the view switches on and
/// what the tests compare. The shell projects no string for it.
public enum PlanLabel: Equatable {
  case free
  case premiumMonthly
  case premiumYearly
}
