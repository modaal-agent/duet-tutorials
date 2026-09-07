// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import EditNameShell
import FoyerBridge
import FoyerKit
import Foundation

/// What the account screen consumes from its parent: the auth port for the
/// sign-out, and the account port its child, the name editor, saves through.
/// A level names what its subtree needs, so the parent supplies it once.
/// sourcery: DuetComponent
/// sourcery: CreateMock
public protocol AccountDependency: AnyObject {
  var auth: any AuthPort { get }
  var account: any AccountPort { get }
}

/// The Component satisfies the editor's Dependency with its own members: the
/// names line up, so the conformance is one empty extension.
extension AccountComponent: EditNameDependency {}

final class LiveAccountEnvironment: NSObject, AccountEnvironment {
  private let auth: any AuthPort
  private let onDelegate: (AccountDelegateEvent) -> Void

  init(auth: any AuthPort, onDelegate: @escaping (AccountDelegateEvent) -> Void) {
    self.auth = auth
    self.onDelegate = onDelegate
  }

  func signOut(onDone: @escaping () -> Void) {
    auth.signOut(onDone: onDone)
  }

  func notifyHost(event: AccountDelegateEvent) {
    onDelegate(event)
  }
}

extension AccountComponent {
  @MainActor
  func environment(
    onDelegate: @escaping (AccountDelegateEvent) -> Void
  ) -> LiveAccountEnvironment {
    LiveAccountEnvironment(auth: auth, onDelegate: onDelegate)
  }
}

// MARK: - Builder

public final class AccountChild {
  public let shell: AccountViewShell

  init(shell: AccountViewShell) {
    self.shell = shell
  }
}

public final class AccountBuilder {
  private let dependency: AccountDependency

  public init(dependency: AccountDependency) {
    self.dependency = dependency
  }

  /// Builds the account mount. The editor is not built here: the shell
  /// mounts it from `state.child`, through the factory this Builder hands
  /// it, over the same per-mount Component.
  @MainActor
  public func buildAccount(
    displayName: String,
    onDelegate: @escaping (AccountDelegateEvent) -> Void
  ) -> AccountChild {
    let component = AccountComponent(dependency: dependency)
    let scope = mainImmediateStoreScope()
    let store = makeAccountStore(
      displayName: displayName,
      environment: component.environment(onDelegate: onDelegate),
      scope: scope)
    let bridged = AccountKitStore(
      state: accountStateFlow(store: store),
      send: { store.send(action: $0) },
      teardown: {
        store.teardown()
        cancelStoreScope(scope: scope)
      })
    let shell = AccountViewShell(store: bridged) { currentName, onDelegate in
      EditNameBuilder(dependency: component)
        .buildEditName(currentName: currentName, onDelegate: onDelegate)
    }
    return AccountChild(shell: shell)
  }
}
