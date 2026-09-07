// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import FoyerBridge
import FoyerKit
import Foundation

/// What the name editor consumes from its parent: the account port.
/// sourcery: DuetComponent
/// sourcery: CreateMock
public protocol EditNameDependency: AnyObject {
  var account: any AccountPort { get }
}

final class LiveEditNameEnvironment: NSObject, EditNameEnvironment {
  private let account: any AccountPort
  private let onDelegate: (EditNameDelegateEvent) -> Void

  init(account: any AccountPort, onDelegate: @escaping (EditNameDelegateEvent) -> Void) {
    self.account = account
    self.onDelegate = onDelegate
  }

  func saveName(name: String, onSaved: @escaping () -> Void) {
    account.saveDisplayName(name: name, onSaved: onSaved)
  }

  func notifyHost(event: EditNameDelegateEvent) {
    onDelegate(event)
  }
}

extension EditNameComponent {
  @MainActor
  func environment(
    onDelegate: @escaping (EditNameDelegateEvent) -> Void
  ) -> LiveEditNameEnvironment {
    LiveEditNameEnvironment(account: account, onDelegate: onDelegate)
  }
}

// MARK: - Builder

public final class EditNameChild {
  public let shell: EditNameViewShell

  init(shell: EditNameViewShell) {
    self.shell = shell
  }
}

public final class EditNameBuilder {
  private let dependency: EditNameDependency

  public init(dependency: EditNameDependency) {
    self.dependency = dependency
  }

  /// `currentName` seeds the field; the mount belongs to the parent that
  /// shows the name.
  @MainActor
  public func buildEditName(
    currentName: String,
    onDelegate: @escaping (EditNameDelegateEvent) -> Void
  ) -> EditNameChild {
    let component = EditNameComponent(dependency: dependency)
    let scope = mainImmediateStoreScope()
    let store = makeEditNameStore(
      currentName: currentName,
      environment: component.environment(onDelegate: onDelegate),
      scope: scope)
    let bridged = EditNameKitStore(
      state: editNameStateFlow(store: store),
      send: { store.send(action: $0) },
      teardown: {
        store.teardown()
        cancelStoreScope(scope: scope)
      })
    return EditNameChild(shell: EditNameViewShell(store: bridged))
  }
}
