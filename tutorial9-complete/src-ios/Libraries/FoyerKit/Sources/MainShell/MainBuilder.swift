// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import FoyerBridge
import FoyerKit
import Foundation
import HomeShell
import ProfileShell
import UpgradeShell

/// What the main level consumes: the union of what its two tabs' subtrees and the flow need.
/// sourcery: DuetComponent
/// sourcery: CreateMock
public protocol MainDependency: AnyObject {
  var items: any ItemsPort { get }
  var purchases: any PurchasesPort { get }
  var auth: any AuthPort { get }
  var account: any AccountPort { get }
  /// Consumed by every feature under this level; the level itself tracks nothing.
  var analytics: any AnalyticsTracking { get }
}

extension MainComponent: HomeDependency {}
extension MainComponent: ProfileDependency {}
extension MainComponent: UpgradeDependency {}

final class LiveMainEnvironment: NSObject, MainEnvironment {
  private let onDelegate: (MainDelegateEvent) -> Void
  private let onForward: (DeepLink) -> Void

  init(onDelegate: @escaping (MainDelegateEvent) -> Void, onForward: @escaping (DeepLink) -> Void) {
    self.onDelegate = onDelegate
    self.onForward = onForward
  }

  func notifyHost(event: MainDelegateEvent) {
    onDelegate(event)
  }

  func forwardLink(link: DeepLink) {
    onForward(link)
  }
}

/// The flow builder, over the level's Component.
final class MainSheetMounter: MainSheetMounting {
  private let component: MainComponent

  init(component: MainComponent) {
    self.component = component
  }

  func mountUpgrade(step: UpgradeStep, onDelegate: @escaping (UpgradeDelegateEvent) -> Void)
    -> UpgradeChild
  {
    UpgradeBuilder(dependency: component).buildUpgrade(step: step, onDelegate: onDelegate)
  }
}

// MARK: - Builder

public final class MainChild {
  public let shell: MainViewShell

  init(shell: MainViewShell) {
    self.shell = shell
  }
}

public final class MainBuilder {
  private let dependency: MainDependency

  public init(dependency: MainDependency) {
    self.dependency = dependency
  }

  /// Builds the main mount and both tabs: they live for the level's
  /// lifetime, so the Builder constructs them here over the same per-mount
  /// Component, and the shell brackets them with its own activation. The
  /// entitlement is the root's slice; the shell projects it into both tabs.
  /// `restored` is the spine's main-level slivers, applied as each store's
  /// initial state, or nil.
  @MainActor
  public func buildMain(
    displayName: String,
    entitlement: Projected<Entitlement>,
    restored: RouteSpine?,
    onDelegate: @escaping (MainDelegateEvent) -> Void
  ) -> MainChild {
    let component = MainComponent(dependency: dependency)
    let scope = mainImmediateStoreScope()
    var shellBox: MainViewShell?
    let store = makeMainStore(
      environment: LiveMainEnvironment(
        onDelegate: onDelegate,
        onForward: { link in shellBox?.forward(link) }),
      scope: scope,
      initialState: MainState(
        activeTab: restored?.activeTab ?? MainTabHome.shared,
        sheet: restored?.upgradeStep == nil ? nil : MainSheetUpgrade.shared))
    let bridged = MainKitStore(
      state: mainStateFlow(store: store),
      send: { store.send(action: $0) },
      teardown: {
        store.teardown()
        cancelStoreScope(scope: scope)
      })
    let shell = MainViewShell(
      store: bridged,
      entitlement: entitlement,
      mounter: MainSheetMounter(component: component),
      restoredStep: restored?.upgradeStep)
    shellBox = shell
    let home = HomeBuilder(dependency: component)
      .buildHome(presented: restored?.homePresented) { [weak shell] event in
        shell?.store.send(MainActionHome(event: event))
      }
    let profile = ProfileBuilder(dependency: component)
      .buildProfile(displayName: displayName, restoredPath: restored?.profilePath) { [weak shell] event in
        shell?.store.send(MainActionProfile(event: event))
      }
    shell.attach(home: home, profile: profile)
    return MainChild(shell: shell)
  }
}
