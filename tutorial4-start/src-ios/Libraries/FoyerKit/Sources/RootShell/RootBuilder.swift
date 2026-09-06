// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import FoyerBridge
import FoyerKit
import Foundation
import MainShell
import SignInShell
import SplashShell

/// What the one root mount owns. The scene retains it and brackets it with
/// `shell.activate()` / `shell.deactivate()`.
public final class RootChild {
  public let shell: RootViewShell

  init(shell: RootViewShell) {
    self.shell = shell
  }
}

/// The child builders, over the root's Component. The shell calls these when
/// the phase names a child; each child's delegate events route back to the
/// root store as actions.
final class RootChildMounter: RootChildMounting {
  private let component: RootComponent

  init(component: RootComponent) {
    self.component = component
  }

  func mountSplash(onDelegate: @escaping (SplashDelegateEvent) -> Void) -> SplashChild {
    SplashBuilder().buildSplash(onDelegate: onDelegate)
  }

  func mountSignIn(onDelegate: @escaping (SignInDelegateEvent) -> Void) -> SignInChild {
    SignInBuilder(dependency: component).buildSignIn(onDelegate: onDelegate)
  }

  func mountMain(
    displayName: String, onDelegate: @escaping (MainDelegateEvent) -> Void
  ) -> MainChild {
    MainBuilder(dependency: component).buildMain(displayName: displayName, onDelegate: onDelegate)
  }
}

/// The root Builder: constructs the Component once per mount, builds the
/// root store and shell, and seeds the auth snapshot.
public final class RootBuilder {
  private let dependency: RootDependency

  public init(dependency: RootDependency) {
    self.dependency = dependency
  }

  @MainActor
  public func buildRoot() -> RootChild {
    let component = RootComponent(dependency: dependency)
    let scope = mainImmediateStoreScope()
    let store = makeRootStore(scope: scope)
    let bridged = RootKitStore(
      state: rootStateFlow(store: store),
      send: { store.send(action: $0) },
      teardown: {
        store.teardown()
        cancelStoreScope(scope: scope)
      })
    let shell = RootViewShell(store: bridged, mounter: RootChildMounter(component: component))
    // The auth seed: the mock keeps no session, so the root starts signed
    // out. Tutorial 4's session worker sends this from the auth port's stream.
    bridged.send(RootActionAuthChanged(auth: AuthSnapshotSignedOut.shared))
    return RootChild(shell: shell)
  }
}
