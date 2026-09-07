// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import DuetShells
import FoyerBridge
import FoyerKit
import Foundation
import MainShell
import OnboardingShell
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

  func mountOnboarding(
    page: OnboardingPage,
    onDelegate: @escaping (OnboardingDelegateEvent) -> Void
  ) -> OnboardingChild {
    OnboardingBuilder(dependency: component).buildOnboarding(page: page, onDelegate: onDelegate)
  }

  func mountMain(
    displayName: String,
    entitlement: Projected<Entitlement>,
    restored: RouteSpine?,
    onDelegate: @escaping (MainDelegateEvent) -> Void
  ) -> MainChild {
    MainBuilder(dependency: component)
      .buildMain(
        displayName: displayName, entitlement: entitlement, restored: restored,
        onDelegate: onDelegate)
  }
}

/// The root Builder: constructs the Component once per mount, builds the
/// root store, the shell and the two workers the shell adopts at activation.
public final class RootBuilder {
  private let dependency: RootDependency

  public init(dependency: RootDependency) {
    self.dependency = dependency
  }

  /// `restored` is the spine a previous process saved, or nil for a fresh
  /// start. The splash replays either way; the slivers below the phase apply
  /// once, when the child they belong to mounts.
  @MainActor
  public func buildRoot(restored: RouteSpine?) -> RootChild {
    let scope = mainImmediateStoreScope()
    let component = RootComponent(dependency: dependency, scope: scope)
    var shellBox: RootViewShell?
    let store = makeRootStore(
      environment: LiveRootEnvironment(
        account: component.account,
        onForward: { link in shellBox?.forward(link) }),
      scope: scope)
    let bridged = RootKitStore(
      state: rootStateFlow(store: store),
      send: { store.send(action: $0) },
      teardown: {
        store.teardown()
        cancelStoreScope(scope: scope)
      })
    // The workers' ingress: every report is a root action. The streams are
    // sticky, so the auth seed is the session worker's first report.
    let relay = Relay<any RootAction>()
    relay.bindSink(bridged) { store, action in store.send(action) }
    let shell = RootViewShell(
      store: bridged,
      mounter: RootChildMounter(component: component),
      workers: RootWorkers(
        session: SessionWorker(auth: component.auth, relay: relay),
        entitlement: EntitlementWorker(purchases: component.purchases, relay: relay)),
      restored: restored)
    shellBox = shell
    return RootChild(shell: shell)
  }
}
