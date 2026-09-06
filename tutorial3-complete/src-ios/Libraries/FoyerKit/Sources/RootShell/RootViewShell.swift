// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import Combine
import DuetShells
import FoyerBridge
import FoyerKit
import Foundation
import MainShell
import SignInShell
import SplashShell

public typealias RootKitStore = BridgedStore<RootState, any RootAction>

/// The child the root has mounted, as the view renders it.
public enum RootChildMount {
  case splash(SplashChild)
  case signIn(SignInChild)
  case main(MainChild)
}

/// How the shell mounts each child; the Builder supplies the conformer over
/// the root's Component.
@MainActor
protocol RootChildMounting: AnyObject {
  func mountSplash(onDelegate: @escaping (SplashDelegateEvent) -> Void) -> SplashChild
  func mountSignIn(onDelegate: @escaping (SignInDelegateEvent) -> Void) -> SignInChild
  func mountMain(displayName: String, onDelegate: @escaping (MainDelegateEvent) -> Void)
    -> MainChild
}

@MainActor
public final class RootViewState: ObservableObject {
  @Published public internal(set) var child: RootChildMount?

  public init() {}
}

/// The root's shell has one duty beyond the three: mounting the child the
/// phase names, exactly one at a time, and tearing down the one that left.
public final class RootViewShell: ViewShell {
  public let viewState = RootViewState()
  public let store: RootKitStore
  private let mounter: RootChildMounting
  private var child: ChildSlot<PhaseKey, RootChildMount>?
  /// The state being applied. The mirror publishes before it assigns, so a
  /// child built during a projection reads this, not `store.state`.
  private var applying: RootState?

  private enum PhaseKey: Hashable {
    case splash
    case signIn
    case main
  }

  init(store: RootKitStore, mounter: RootChildMounting) {
    self.store = store
    self.mounter = mounter
    super.init()
  }

  override public func bind() {
    host.adopt(store)
    child = host.adopt(
      ChildSlot<PhaseKey, RootChildMount>(
        build: { [weak self] key in self?.build(key) },
        teardown: { mount in
          switch mount {
          case .splash(let child): child.shell.deactivate()
          case .signIn(let child): child.shell.deactivate()
          case .main(let child): child.shell.deactivate()
          }
        }))
    host.adopt(
      StateTransitions(state: store.$state) { [weak self] _, state in
        self?.apply(state)
      })
  }

  /// Each child's delegate events are the root's actions.
  private func build(_ key: PhaseKey) -> RootChildMount {
    switch key {
    case .splash:
      let child = mounter.mountSplash { [weak self] event in
        self?.store.send(RootActionSplash(event: event))
      }
      child.shell.activate()
      return .splash(child)
    case .signIn:
      let child = mounter.mountSignIn { [weak self] event in
        self?.store.send(RootActionSignIn(event: event))
      }
      child.shell.activate()
      return .signIn(child)
    case .main:
      let auth = (applying ?? store.state).auth
      let child = mounter.mountMain(displayName: displayName(of: auth)) {
        [weak self] event in
        self?.store.send(RootActionMain(event: event))
      }
      child.shell.activate()
      return .main(child)
    }
  }

  // MARK: - State to view state, and the child mount

  private func apply(_ state: RootState) {
    applying = state
    defer { applying = nil }
    let key: PhaseKey =
      switch onEnum(of: state.phase) {
      case .splash: .splash
      case .signIn: .signIn
      case .main: .main
      }
    child?.reconcile(key: key)
    viewState.child = child?.activeHandle
  }

  /// The name the profile tree shows: the session's, or the guest name.
  private func displayName(of auth: AuthSnapshot) -> String {
    if case .signedIn(let signedIn) = onEnum(of: auth) {
      return signedIn.displayName
    }
    return "Guest"
  }
}
