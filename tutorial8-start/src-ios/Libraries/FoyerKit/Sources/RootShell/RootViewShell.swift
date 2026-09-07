// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import Combine
import DuetShells
import FoyerBridge
import FoyerKit
import Foundation
import MainShell
import OnboardingShell
import SignInShell
import SplashShell

public typealias RootKitStore = BridgedStore<RootState, any RootAction>

/// The child the root has mounted, as the view renders it.
public enum RootChildMount {
  case splash(SplashChild)
  case signIn(SignInChild)
  case onboarding(OnboardingChild)
  case main(MainChild)
}

/// How the shell mounts each child; the Builder supplies the conformer over
/// the root's Component.
@MainActor
protocol RootChildMounting: AnyObject {
  func mountSplash(onDelegate: @escaping (SplashDelegateEvent) -> Void) -> SplashChild
  func mountSignIn(onDelegate: @escaping (SignInDelegateEvent) -> Void) -> SignInChild
  func mountOnboarding(
    page: OnboardingPage,
    onDelegate: @escaping (OnboardingDelegateEvent) -> Void
  ) -> OnboardingChild
  func mountMain(
    displayName: String,
    entitlement: Projected<Entitlement>,
    restored: RouteSpine?,
    onDelegate: @escaping (MainDelegateEvent) -> Void
  ) -> MainChild
}

/// The two workers the root adopts at activation.
struct RootWorkers {
  let session: SessionWorker
  let entitlement: EntitlementWorker
}

@MainActor
public final class RootViewState: ObservableObject {
  @Published public internal(set) var child: RootChildMount?

  public init() {}
}

/// The root's shell has duties beyond the three: mounting the child the
/// phase names, exactly one at a time; adopting the two workers for the
/// mount's lifetime; projecting the entitlement slice the main level hands
/// its tabs; holding a forwarded link until main is up; and reading the
/// route spine the scene saves.
public final class RootViewShell: ViewShell {
  public let viewState = RootViewState()
  public let store: RootKitStore
  private let mounter: RootChildMounting
  private let workers: RootWorkers
  private var child: ChildSlot<PhaseKey, RootChildMount>?
  /// The slice: root state, narrowed to the one value the tabs read.
  private let entitlement = Projected<Entitlement>(EntitlementFree.shared)
  /// The state being applied. The mirror publishes before it assigns, so a
  /// child built during a projection reads this, not `store.state`.
  private var applying: RootState?
  /// The spine a previous process saved; applied to the first child after the splash.
  private var pendingRestore: RouteSpine?
  /// A forwarded link that arrived before main was mounted.
  private var heldLink: DeepLink?

  private enum PhaseKey: Hashable {
    case splash
    case signIn
    case onboarding
    case main
  }

  init(store: RootKitStore, mounter: RootChildMounting, workers: RootWorkers, restored: RouteSpine?) {
    self.store = store
    self.mounter = mounter
    self.workers = workers
    self.pendingRestore = restored
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
          case .onboarding(let child): child.shell.deactivate()
          case .main(let child): child.shell.deactivate()
          }
        }))
    host.adopt(
      StateTransitions(state: store.$state) { [weak self] _, state in
        self?.apply(state)
      })
    // Adopted last, so the first report of each sticky stream lands on a
    // store whose projection is already bound; cancelled first at teardown.
    host.adopt(workers.session)
    host.adopt(workers.entitlement)
  }

  /// Each child's delegate events are the root's actions.
  private func build(_ key: PhaseKey) -> RootChildMount {
    // The spine applies to the first child after the splash, and only when
    // that child is the one the spine names; otherwise it is dropped.
    var restoreFor: RouteSpine?
    if key != .splash {
      restoreFor = pendingRestore.flatMap { spine in
        switch (key, onEnum(of: spine.phase)) {
        case (.main, .main), (.onboarding, .onboarding): spine
        default: nil
        }
      }
      pendingRestore = nil
    }
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
    case .onboarding:
      let child = mounter.mountOnboarding(page: restoreFor?.onboardingPage ?? .welcome) {
        [weak self] event in
        self?.store.send(RootActionOnboarding(event: event))
      }
      child.shell.activate()
      return .onboarding(child)
    case .main:
      let auth = (applying ?? store.state).auth
      let child = mounter.mountMain(
        displayName: displayName(of: auth), entitlement: entitlement, restored: restoreFor
      ) { [weak self] event in
        self?.store.send(RootActionMain(event: event))
      }
      child.shell.activate()
      return .main(child)
    }
  }

  // MARK: - Intents

  /// The scene's report: the operating system handed the app a link it answers.
  public func openLink(_ link: DeepLink) { store.send(RootActionDeepLink(link: link)) }

  /// The `forwardLink` effect lands here. The effect can run before the
  /// mirror has applied the phase change that mounts main, so a link with no
  /// main to receive it waits for the next apply.
  func forward(_ link: DeepLink) {
    if case .main(let main) = child?.activeHandle {
      main.shell.openLink(link)
    } else {
      heldLink = link
    }
  }

  /// Each level's route sliver, read from the live shells: what the scene saves.
  public func routeSpine() -> RouteSpine {
    switch child?.activeHandle {
    case .main(let main):
      main.shell.routeSpine()
    case .onboarding(let onboarding):
      RouteSpine(
        phase: RootPhaseOnboarding.shared, activeTab: nil, profilePath: nil, homePresented: nil,
        upgradeStep: nil, onboardingPage: onboarding.shell.store.state.page)
    default:
      RouteSpine(
        phase: store.state.phase, activeTab: nil, profilePath: nil, homePresented: nil,
        upgradeStep: nil, onboardingPage: nil)
    }
  }

  // MARK: - State to view state, the slice, and the child mount

  private func apply(_ state: RootState) {
    applying = state
    defer { applying = nil }
    // The slice first, so a main level built below reads the current value.
    entitlement.project(state.entitlement)
    let key: PhaseKey =
      switch onEnum(of: state.phase) {
      case .splash: .splash
      case .signIn: .signIn
      case .onboarding: .onboarding
      case .main: .main
      }
    child?.reconcile(key: key)
    viewState.child = child?.activeHandle
    if let link = heldLink, case .main(let main) = viewState.child {
      heldLink = nil
      main.shell.openLink(link)
    }
  }

  /// The name the profile tree shows: the session's, or the guest name.
  private func displayName(of auth: AuthSnapshot) -> String {
    if case .signedIn(let signedIn) = onEnum(of: auth) {
      return signedIn.displayName
    }
    return "Guest"
  }
}
