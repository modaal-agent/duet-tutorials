// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import FoyerBridge
import FoyerKit
import Foundation

/// What the home tab consumes from its parent: the items port.
/// sourcery: DuetComponent
/// sourcery: CreateMock
public protocol HomeDependency: AnyObject {
  var items: any ItemsPort { get }
  var analytics: any AnalyticsTracking { get }
}

final class LiveHomeEnvironment: NSObject, HomeEnvironment {
  private let items: any ItemsPort
  private let analytics: any AnalyticsTracking
  private let onDelegate: (HomeDelegateEvent) -> Void

  init(
    items: any ItemsPort,
    analytics: any AnalyticsTracking,
    onDelegate: @escaping (HomeDelegateEvent) -> Void
  ) {
    self.items = items
    self.analytics = analytics
    self.onDelegate = onDelegate
  }

  func track(event: TrackedEvent) {
    analytics.track(event: event)
  }

  func loadItems(onItems: @escaping ([Item]) -> Void) {
    items.items(onItems: onItems)
  }

  func notifyHost(event: HomeDelegateEvent) {
    onDelegate(event)
  }
}

extension HomeComponent {
  @MainActor
  func environment(onDelegate: @escaping (HomeDelegateEvent) -> Void) -> LiveHomeEnvironment {
    LiveHomeEnvironment(items: items, analytics: analytics, onDelegate: onDelegate)
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

  /// `presented` is the restored route sliver, or nil.
  @MainActor
  public func buildHome(
    presented: HomePresentation?,
    onDelegate: @escaping (HomeDelegateEvent) -> Void
  ) -> HomeChild {
    let component = HomeComponent(dependency: dependency)
    let scope = mainImmediateStoreScope()
    let store = makeHomeStore(
      environment: component.environment(onDelegate: onDelegate),
      scope: scope,
      initialState: HomeState(items: [], isLoading: false, entitlement: EntitlementFree.shared, presented: presented))
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
