// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import FoyerBridge
import FoyerKit
import Foundation

/// What the upgrade flow consumes from its parent: the purchases port.
/// sourcery: DuetComponent
/// sourcery: CreateMock
public protocol UpgradeDependency: AnyObject {
  var purchases: any PurchasesPort { get }
  var analytics: any AnalyticsTracking { get }
}

final class LiveUpgradeEnvironment: NSObject, UpgradeEnvironment {
  private let purchases: any PurchasesPort
  private let analytics: any AnalyticsTracking
  private let onDelegate: (UpgradeDelegateEvent) -> Void

  init(
    purchases: any PurchasesPort,
    analytics: any AnalyticsTracking,
    onDelegate: @escaping (UpgradeDelegateEvent) -> Void
  ) {
    self.purchases = purchases
    self.analytics = analytics
    self.onDelegate = onDelegate
  }

  func track(event: TrackedEvent) {
    analytics.track(event: event)
  }

  func loadPlans(onPlans: @escaping ([PlanOffer]) -> Void) {
    purchases.plans(onPlans: onPlans)
  }

  func purchase(plan: Plan, onOutcome: @escaping (PurchaseOutcome) -> Void) {
    purchases.purchase(plan: plan, onOutcome: onOutcome)
  }

  func notifyHost(event: UpgradeDelegateEvent) {
    onDelegate(event)
  }
}

extension UpgradeComponent {
  @MainActor
  func environment(
    onDelegate: @escaping (UpgradeDelegateEvent) -> Void
  ) -> LiveUpgradeEnvironment {
    LiveUpgradeEnvironment(purchases: purchases, analytics: analytics, onDelegate: onDelegate)
  }
}

// MARK: - Builder

public final class UpgradeChild {
  public let shell: UpgradeViewShell

  init(shell: UpgradeViewShell) {
    self.shell = shell
  }
}

public final class UpgradeBuilder {
  private let dependency: UpgradeDependency

  public init(dependency: UpgradeDependency) {
    self.dependency = dependency
  }

  /// `step` is the restored route sliver, or the first step.
  @MainActor
  public func buildUpgrade(
    step: UpgradeStep,
    onDelegate: @escaping (UpgradeDelegateEvent) -> Void
  ) -> UpgradeChild {
    let component = UpgradeComponent(dependency: dependency)
    let scope = mainImmediateStoreScope()
    let store = makeUpgradeStore(
      step: step,
      environment: component.environment(onDelegate: onDelegate),
      scope: scope)
    let bridged = UpgradeKitStore(
      state: upgradeStateFlow(store: store),
      send: { store.send(action: $0) },
      teardown: {
        store.teardown()
        cancelStoreScope(scope: scope)
      })
    return UpgradeChild(shell: UpgradeViewShell(store: bridged))
  }
}
