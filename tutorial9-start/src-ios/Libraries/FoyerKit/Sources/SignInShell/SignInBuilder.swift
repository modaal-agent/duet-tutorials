// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import FoyerBridge
import FoyerKit
import Foundation

// The sign-in level's composition triple: Dependency, Component, Builder.
// One file per level, named for the Builder.

/// What the sign-in level consumes from its parent: the auth port, and
/// nothing else. The parent's Component conforms; delete this member and
/// the root's conformance fails to compile.
///
/// `DuetComponent` generates the level's Component
/// (Generated/SignInShellComponents.swift): one forwarder per member.
/// `CreateMock` generates the test double the shell spec composes over
/// (Tests/SignInShellTests/Generated/). Both are `tools/duet mocks` output,
/// drift-gated by `tools/duet mocks --check`.
/// sourcery: DuetComponent
/// sourcery: CreateMock
public protocol SignInDependency: AnyObject {
  var auth: any AuthPort { get }
}

/// The live environment, implemented in Swift against the bridged Kotlin
/// interface: the feature's narrow seam over the app's auth port.
final class LiveSignInEnvironment: NSObject, SignInEnvironment {
  private let auth: any AuthPort
  private let onDelegate: (SignInDelegateEvent) -> Void

  init(auth: any AuthPort, onDelegate: @escaping (SignInDelegateEvent) -> Void) {
    self.auth = auth
    self.onDelegate = onDelegate
  }

  func signIn(provider: SignInProvider, onOutcome: @escaping (SignInOutcome) -> Void) {
    auth.signIn(provider: provider, onOutcome: onOutcome)
  }

  func notifyHost(event: SignInDelegateEvent) {
    onDelegate(event)
  }
}

/// The environment factory is a Component member: the environment is
/// assembled from what the level consumes, and the Component holds that.
extension SignInComponent {
  @MainActor
  func environment(
    onDelegate: @escaping (SignInDelegateEvent) -> Void
  ) -> LiveSignInEnvironment {
    LiveSignInEnvironment(auth: auth, onDelegate: onDelegate)
  }
}

// MARK: - Builder

/// What a mount owns. The shell owns the store's lifetime, and the mount's
/// bracket is `shell.activate()` / `shell.deactivate()`.
public final class SignInChild {
  public let shell: SignInViewShell

  init(shell: SignInViewShell) {
    self.shell = shell
  }
}

/// Builds one sign-in mount: the Component once per mount, then scope,
/// Kotlin store, Swift mirror, shell. The Builder resolves nothing itself.
public final class SignInBuilder {
  private let dependency: SignInDependency

  public init(dependency: SignInDependency) {
    self.dependency = dependency
  }

  @MainActor
  public func buildSignIn(
    onDelegate: @escaping (SignInDelegateEvent) -> Void
  ) -> SignInChild {
    let component = SignInComponent(dependency: dependency)
    let scope = mainImmediateStoreScope()
    let store = makeSignInStore(
      environment: component.environment(onDelegate: onDelegate),
      scope: scope)
    let bridged = SignInKitStore(
      state: signInStateFlow(store: store),
      send: { store.send(action: $0) },
      teardown: {
        store.teardown()
        cancelStoreScope(scope: scope)
      })
    return SignInChild(shell: SignInViewShell(store: bridged))
  }
}
