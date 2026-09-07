// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import Combine
import DuetShells
import FoyerBridge
import FoyerKit
import Foundation
import HomeShell
import ProfileShell
import UpgradeShell

public typealias MainKitStore = BridgedStore<MainState, any MainAction>

/// The tab, as the view selects it.
public enum MainTabKey: Hashable {
  case home
  case profile
}

/// How the shell mounts the flow; the Builder supplies the conformer over
/// the level's Component.
@MainActor
protocol MainSheetMounting: AnyObject {
  func mountUpgrade(step: UpgradeStep, onDelegate: @escaping (UpgradeDelegateEvent) -> Void)
    -> UpgradeChild
}

@MainActor
public final class MainViewState: ObservableObject {
  @Published public internal(set) var activeTab: MainTabKey = .home
  /// The flow, while the sheet slot names it. The view presents it as a sheet.
  @Published public internal(set) var upgrade: UpgradeChild?

  public init() {}
}

/// The main level's shell: the tab intent, the tab projection, the bracket
/// over both tabs, which the Builder attached before activation, the root's
/// entitlement slice projected into both tabs as their own action, and the
/// sheet slot the upgrade flow mounts in.
public final class MainViewShell: ViewShell {
  public let viewState = MainViewState()
  public let store: MainKitStore
  public private(set) var home: HomeChild?
  public private(set) var profile: ProfileChild?
  private let entitlement: Projected<Entitlement>
  private let mounter: MainSheetMounting
  private var sheet: ChildSlot<SheetKey, UpgradeChild>?
  /// The step the restored route names; consumed by the first flow mount.
  private var restoredStep: UpgradeStep?

  private enum SheetKey: Hashable {
    case upgrade
  }

  init(
    store: MainKitStore,
    entitlement: Projected<Entitlement>,
    mounter: MainSheetMounting,
    restoredStep: UpgradeStep?
  ) {
    self.store = store
    self.entitlement = entitlement
    self.mounter = mounter
    self.restoredStep = restoredStep
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
    sheet = host.adopt(
      ChildSlot<SheetKey, UpgradeChild>(
        build: { [weak self] _ in self?.buildUpgrade() },
        teardown: { $0.shell.deactivate() }))
    host.adopt(
      StateTransitions(state: store.$state) { [weak self] _, state in
        self?.apply(state)
      })
    // State down: the slice reaches both tabs as an action of their own.
    // The first delivery is the current value, at mount.
    host.adopt(
      entitlement.observe { [weak self] value in
        self?.home?.shell.entitlementChanged(value)
        self?.profile?.shell.entitlementChanged(value)
      })
  }

  /// The flow's delegate events are this level's actions.
  private func buildUpgrade() -> UpgradeChild {
    let step = restoredStep ?? UpgradeStepPlans.shared
    restoredStep = nil
    let child = mounter.mountUpgrade(step: step) { [weak self] event in
      self?.store.send(MainActionUpgrade(event: event))
    }
    child.shell.activate()
    return child
  }

  // MARK: - Intents

  public func selectTab(_ tab: MainTabKey) {
    switch tab {
    case .home: store.send(MainActionTabSelected(tab: MainTabHome.shared))
    case .profile: store.send(MainActionTabSelected(tab: MainTabProfile.shared))
    }
  }

  /// The parent's forward: a deep link the root handed down.
  public func openLink(_ link: DeepLink) { store.send(MainActionOpenLink(link: link)) }

  /// The `forwardLink` effect lands here: the link travels on as the profile tab's own action.
  func forward(_ link: DeepLink) { profile?.shell.openLink(link) }

  /// This level's slivers and its children's, for the route spine.
  public func routeSpine() -> RouteSpine {
    RouteSpine(
      phase: RootPhaseMain.shared,
      activeTab: store.state.activeTab,
      profilePath: profile?.shell.routePath,
      homePresented: home?.shell.store.state.presented,
      upgradeStep: viewState.upgrade?.shell.store.state.step,
      onboardingPage: nil)
  }

  // MARK: - State to view state, and the sheet mount

  private func apply(_ state: MainState) {
    switch onEnum(of: state.activeTab) {
    case .home: viewState.activeTab = .home
    case .profile: viewState.activeTab = .profile
    }
    sheet?.reconcile(key: state.sheet == nil ? nil : .upgrade)
    viewState.upgrade = sheet?.activeHandle
  }
}
