// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import FoyerBridge
import FoyerKit
import Foundation

/// What the home tab consumes from its parent: the items port and the
/// purchases port.
/// sourcery: DuetComponent
/// sourcery: CreateMock
public protocol HomeDependency: AnyObject {
  var items: any ItemsPort { get }
  var purchases: any PurchasesPort { get }
}

final class LiveHomeEnvironment: NSObject, HomeEnvironment {
  private let items: any ItemsPort
  private let purchases: any PurchasesPort

  init(items: any ItemsPort, purchases: any PurchasesPort) {
    self.items = items
    self.purchases = purchases
  }

  func loadItems(onItems: @escaping ([Item]) -> Void) {
    items.items(onItems: onItems)
  }

  func purchase(plan: Plan, onOutcome: @escaping (PurchaseOutcome) -> Void) {
    purchases.purchase(plan: plan, onOutcome: onOutcome)
  }
}

extension HomeComponent {
  @MainActor
  func environment() -> LiveHomeEnvironment {
    LiveHomeEnvironment(items: items, purchases: purchases)
  }
}

// MARK: - Builder

public final class HomeChild {
  public let shell: HomeViewShell

  init(shell: HomeViewShell) {
    self.shell = shell
  }
}

public final class HomeBuilder {
  private let dependency: HomeDependency

  public init(dependency: HomeDependency) {
    self.dependency = dependency
  }

  @MainActor
  public func buildHome() -> HomeChild {
    let component = HomeComponent(dependency: dependency)
    let scope = mainImmediateStoreScope()
    let store = makeHomeStore(environment: component.environment(), scope: scope)
    let bridged = HomeKitStore(
      state: homeStateFlow(store: store),
      send: { store.send(action: $0) },
      teardown: {
        store.teardown()
        cancelStoreScope(scope: scope)
      })
    return HomeChild(shell: HomeViewShell(store: bridged))
  }
}
