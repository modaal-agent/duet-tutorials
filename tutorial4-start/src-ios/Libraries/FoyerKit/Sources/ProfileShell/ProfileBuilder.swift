// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import AccountShell
import FoyerBridge
import FoyerKit
import Foundation

/// What the profile tab consumes: nothing of its own. Both members are its
/// subtree's, forwarded down to the account screen.
/// sourcery: DuetComponent
/// sourcery: CreateMock
public protocol ProfileDependency: AnyObject {
  var auth: any AuthPort { get }
  var account: any AccountPort { get }
}

extension ProfileComponent: AccountDependency {}

final class LiveProfileEnvironment: NSObject, ProfileEnvironment {
  private let onDelegate: (ProfileDelegateEvent) -> Void

  init(onDelegate: @escaping (ProfileDelegateEvent) -> Void) {
    self.onDelegate = onDelegate
  }

  func notifyHost(event: ProfileDelegateEvent) {
    onDelegate(event)
  }
}

extension ProfileComponent {
  @MainActor
  func environment(
    onDelegate: @escaping (ProfileDelegateEvent) -> Void
  ) -> LiveProfileEnvironment {
    LiveProfileEnvironment(onDelegate: onDelegate)
  }
}

// MARK: - Builder

public final class ProfileChild {
  public let shell: ProfileViewShell

  init(shell: ProfileViewShell) {
    self.shell = shell
  }
}

public final class ProfileBuilder {
  private let dependency: ProfileDependency

  public init(dependency: ProfileDependency) {
    self.dependency = dependency
  }

  @MainActor
  public func buildProfile(
    displayName: String,
    onDelegate: @escaping (ProfileDelegateEvent) -> Void
  ) -> ProfileChild {
    let component = ProfileComponent(dependency: dependency)
    let scope = mainImmediateStoreScope()
    let store = makeProfileStore(
      displayName: displayName,
      environment: component.environment(onDelegate: onDelegate),
      scope: scope)
    let bridged = ProfileKitStore(
      state: profileStateFlow(store: store),
      send: { store.send(action: $0) },
      teardown: {
        store.teardown()
        cancelStoreScope(scope: scope)
      })
    let shell = ProfileViewShell(store: bridged) { currentName, onDelegate in
      AccountBuilder(dependency: component)
        .buildAccount(displayName: currentName, onDelegate: onDelegate)
    }
    return ProfileChild(shell: shell)
  }
}
