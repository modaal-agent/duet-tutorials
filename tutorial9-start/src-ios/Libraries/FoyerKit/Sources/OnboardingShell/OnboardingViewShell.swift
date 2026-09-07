// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import Combine
import DuetShells
import FoyerBridge
import FoyerKit
import Foundation

public typealias OnboardingKitStore = BridgedStore<OnboardingState, any OnboardingAction>

/// The step the level has mounted, as the view renders it.
public enum OnboardingStepMount {
  case welcome(WelcomeChild)
  case name(NameChild)
  case preferences(PreferencesChild)
}

/// How the shell mounts each step; the Builder supplies the conformer over
/// the level's Component.
@MainActor
protocol OnboardingStepMounting: AnyObject {
  func mountWelcome(onDelegate: @escaping (WelcomeDelegateEvent) -> Void) -> WelcomeChild
  func mountName(onDelegate: @escaping (NameDelegateEvent) -> Void) -> NameChild
  func mountPreferences(onDelegate: @escaping (PreferencesDelegateEvent) -> Void)
    -> PreferencesChild
}

@MainActor
public final class OnboardingViewState: ObservableObject {
  @Published public internal(set) var step: OnboardingStepMount?
  /// Back is offered on every page but the first.
  @Published public internal(set) var canGoBack = false

  public init() {}
}

/// The level's shell: one step mounted from the page, the progress row
/// mounted beside it for the level's lifetime, the page projected into the
/// row, and the readiness worker adopted last.
public final class OnboardingViewShell: ViewShell {
  public let viewState = OnboardingViewState()
  public let store: OnboardingKitStore
  public let progress: ProgressChild
  private let mounter: OnboardingStepMounting
  private let worker: OnboardingProgressWorker
  private var step: ChildSlot<OnboardingPage, OnboardingStepMount>?

  init(
    store: OnboardingKitStore,
    progress: ProgressChild,
    mounter: OnboardingStepMounting,
    worker: OnboardingProgressWorker
  ) {
    self.store = store
    self.progress = progress
    self.mounter = mounter
    self.worker = worker
    super.init()
  }

  override public func bind() {
    host.adopt(store)
    // The row lives for the level's lifetime: activated here, deactivated
    // by the host before the store's effects stop.
    progress.shell.activate()
    host.adopt { [progress] in progress.shell.deactivate() }
    step = host.adopt(
      ChildSlot<OnboardingPage, OnboardingStepMount>(
        build: { [weak self] page in self?.build(page) },
        teardown: { mount in
          switch mount {
          case .welcome(let child): child.shell.deactivate()
          case .name(let child): child.shell.deactivate()
          case .preferences(let child): child.shell.deactivate()
          }
        }))
    host.adopt(
      StateTransitions(state: store.$state) { [weak self] _, state in
        self?.apply(state)
      })
    // The seam's worker, adopted for the level's lifetime: the ancestor
    // brackets it; the steps and the row only hold their ports to it.
    host.adopt(worker)
  }

  /// Each step's delegate events are the level's actions.
  private func build(_ page: OnboardingPage) -> OnboardingStepMount {
    switch page {
    case .welcome:
      let child = mounter.mountWelcome { [weak self] event in
        self?.store.send(OnboardingActionWelcome(event: event))
      }
      child.shell.activate()
      return .welcome(child)
    case .name:
      let child = mounter.mountName { [weak self] event in
        self?.store.send(OnboardingActionName(event: event))
      }
      child.shell.activate()
      return .name(child)
    case .preferences:
      let child = mounter.mountPreferences { [weak self] event in
        self?.store.send(OnboardingActionPreferences(event: event))
      }
      child.shell.activate()
      return .preferences(child)
    }
  }

  // MARK: - Intents

  public func back() { store.send(OnboardingActionBack.shared) }

  // MARK: - State to view state, the projection, and the step mount

  private func apply(_ state: OnboardingState) {
    // State down: the page reaches the row as its own action.
    progress.shell.pageChanged(state.page)
    viewState.canGoBack = state.page != .welcome
    step?.reconcile(key: state.page)
    viewState.step = step?.activeHandle
  }
}
