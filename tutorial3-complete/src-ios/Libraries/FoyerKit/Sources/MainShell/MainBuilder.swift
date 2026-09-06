// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import FoyerBridge
import FoyerKit
import Foundation
import HomeShell
import ProfileShell

/// What the main level consumes: the union of what its two tabs' subtrees need.
/// sourcery: DuetComponent
/// sourcery: CreateMock
public protocol MainDependency: AnyObject {
  var items: any ItemsPort { get }
  var auth: any AuthPort { get }
  var account: any AccountPort { get }
}

extension MainComponent: HomeDependency {}
extension MainComponent: ProfileDependency {}

final class LiveMainEnvironment: NSObject, MainEnvironment {
  private let onDelegate: (MainDelegateEvent) -> Void

  init(onDelegate: @escaping (MainDelegateEvent) -> Void) {
    self.onDelegate = onDelegate
  }

  func notifyHost(event: MainDelegateEvent) {
    onDelegate(event)
  }
}

extension MainComponent {
  @MainActor
  func environment(onDelegate: @escaping (MainDelegateEvent) -> Void) -> LiveMainEnvironment {
    LiveMainEnvironment(onDelegate: onDelegate)
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
  /// Component, and the shell brackets them with its own activation.
  @MainActor
  public func buildMain(
    displayName: String,
    onDelegate: @escaping (MainDelegateEvent) -> Void
  ) -> MainChild {
    let component = MainComponent(dependency: dependency)
    let scope = mainImmediateStoreScope()
    let store = makeMainStore(
      environment: component.environment(onDelegate: onDelegate),
      scope: scope)
    let bridged = MainKitStore(
      state: mainStateFlow(store: store),
      send: { store.send(action: $0) },
      teardown: {
        store.teardown()
        cancelStoreScope(scope: scope)
      })
    let shell = MainViewShell(store: bridged)
    let home = HomeBuilder(dependency: component).buildHome()
    let profile = ProfileBuilder(dependency: component)
      .buildProfile(displayName: displayName) { [weak shell] event in
        shell?.store.send(MainActionProfile(event: event))
      }
    shell.attach(home: home, profile: profile)
    return MainChild(shell: shell)
  }
}
