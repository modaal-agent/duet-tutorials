// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import Combine
import DuetShells
import FoyerBridge
import FoyerKit
import Foundation

// The three step leaves and the progress row: each a shell over its own
// store, built by the level's Builder over the level's Component. The steps
// see the seam's producer port, the row its consumer port; none of them
// sees the worker or each other.

/// What a step consumes from its parent: the seam's producer side.
@MainActor
public protocol StepDependency: AnyObject {
  var readinessUpdates: any ReadinessUpdating { get }
}

/// What the progress row consumes from its parent: the seam's consumer side.
@MainActor
public protocol ProgressDependency: AnyObject {
  var readinessObservation: any ReadinessObserving { get }
}

// MARK: - Welcome

public typealias WelcomeKitStore = BridgedStore<WelcomeState, any WelcomeAction>

public final class WelcomeViewShell: ViewShell {
  public let store: WelcomeKitStore

  public init(store: WelcomeKitStore) {
    self.store = store
    super.init()
  }

  override public func bind() {
    host.adopt(store)
  }

  public func appeared() { store.send(WelcomeActionAppeared.shared) }

  public func continueTapped() { store.send(WelcomeActionContinueTapped.shared) }
}

final class LiveWelcomeEnvironment: NSObject, WelcomeEnvironment {
  private let readiness: any ReadinessUpdating
  private let onDelegate: (WelcomeDelegateEvent) -> Void

  init(readiness: any ReadinessUpdating, onDelegate: @escaping (WelcomeDelegateEvent) -> Void) {
    self.readiness = readiness
    self.onDelegate = onDelegate
  }

  func updateReadiness(step: OnboardingPage, ready: Bool) {
    readiness.updateReadiness(step: step, ready: ready)
  }

  func notifyHost(event: WelcomeDelegateEvent) {
    onDelegate(event)
  }
}

public final class WelcomeChild {
  public let shell: WelcomeViewShell

  init(shell: WelcomeViewShell) {
    self.shell = shell
  }
}

public final class WelcomeBuilder {
  private let dependency: StepDependency

  public init(dependency: StepDependency) {
    self.dependency = dependency
  }

  @MainActor
  public func buildWelcome(onDelegate: @escaping (WelcomeDelegateEvent) -> Void) -> WelcomeChild {
    let scope = mainImmediateStoreScope()
    let store = makeWelcomeStore(
      environment: LiveWelcomeEnvironment(
        readiness: dependency.readinessUpdates, onDelegate: onDelegate),
      scope: scope)
    let bridged = WelcomeKitStore(
      state: welcomeStateFlow(store: store),
      send: { store.send(action: $0) },
      teardown: {
        store.teardown()
        cancelStoreScope(scope: scope)
      })
    return WelcomeChild(shell: WelcomeViewShell(store: bridged))
  }
}

// MARK: - Name

public typealias NameKitStore = BridgedStore<NameState, any NameAction>

@MainActor
public final class NameViewState: ObservableObject {
  @Published public internal(set) var draft = ""
  @Published public internal(set) var isReady = false
  /// The refusal, as the reducer wrote it; the view names the string.
  @Published public internal(set) var validation: NameValidation?

  public init() {}
}

public final class NameViewShell: ViewShell {
  public let viewState = NameViewState()
  public let store: NameKitStore

  public init(store: NameKitStore) {
    self.store = store
    super.init()
  }

  override public func bind() {
    host.adopt(store)
    host.adopt(
      StateTransitions(state: store.$state) { [weak self] _, state in
        self?.viewState.draft = state.draft
        self?.viewState.isReady = state.isReady
        self?.viewState.validation = state.validation
      })
  }

  public func draftChanged(_ text: String) { store.send(NameActionDraftChanged(text: text)) }

  public func continueTapped() { store.send(NameActionContinueTapped.shared) }
}

final class LiveNameEnvironment: NSObject, NameEnvironment {
  private let readiness: any ReadinessUpdating
  private let onDelegate: (NameDelegateEvent) -> Void

  init(readiness: any ReadinessUpdating, onDelegate: @escaping (NameDelegateEvent) -> Void) {
    self.readiness = readiness
    self.onDelegate = onDelegate
  }

  func updateReadiness(step: OnboardingPage, ready: Bool) {
    readiness.updateReadiness(step: step, ready: ready)
  }

  func notifyHost(event: NameDelegateEvent) {
    onDelegate(event)
  }
}

public final class NameChild {
  public let shell: NameViewShell

  init(shell: NameViewShell) {
    self.shell = shell
  }
}

public final class NameBuilder {
  private let dependency: StepDependency

  public init(dependency: StepDependency) {
    self.dependency = dependency
  }

  @MainActor
  public func buildName(onDelegate: @escaping (NameDelegateEvent) -> Void) -> NameChild {
    let scope = mainImmediateStoreScope()
    let store = makeNameStore(
      environment: LiveNameEnvironment(readiness: dependency.readinessUpdates, onDelegate: onDelegate),
      scope: scope)
    let bridged = NameKitStore(
      state: nameStateFlow(store: store),
      send: { store.send(action: $0) },
      teardown: {
        store.teardown()
        cancelStoreScope(scope: scope)
      })
    return NameChild(shell: NameViewShell(store: bridged))
  }
}

// MARK: - Preferences

public typealias PreferencesKitStore = BridgedStore<PreferencesState, any PreferencesAction>

/// One toggle, as the view lists it: the key the reducer knows, and whether
/// it is on. The view names the toggle from the key.
public struct PreferenceRow: Equatable {
  public let key: String
  public let isOn: Bool
}

@MainActor
public final class PreferencesViewState: ObservableObject {
  @Published public internal(set) var rows: [PreferenceRow] = []
  @Published public internal(set) var isReady = false

  public init() {}
}

public final class PreferencesViewShell: ViewShell {
  public let viewState = PreferencesViewState()
  public let store: PreferencesKitStore

  public init(store: PreferencesKitStore) {
    self.store = store
    super.init()
  }

  override public func bind() {
    host.adopt(store)
    host.adopt(
      StateTransitions(state: store.$state) { [weak self] _, state in
        self?.viewState.rows = PreferenceKeys.shared.ALL.map { key in
          PreferenceRow(key: key, isOn: state.selected.contains(key))
        }
        self?.viewState.isReady = state.isReady
      })
  }

  public func toggle(_ key: String) { store.send(PreferencesActionPreferenceToggled(key: key)) }

  public func continueTapped() { store.send(PreferencesActionContinueTapped.shared) }
}

final class LivePreferencesEnvironment: NSObject, PreferencesEnvironment {
  private let readiness: any ReadinessUpdating
  private let onDelegate: (PreferencesDelegateEvent) -> Void

  init(readiness: any ReadinessUpdating, onDelegate: @escaping (PreferencesDelegateEvent) -> Void) {
    self.readiness = readiness
    self.onDelegate = onDelegate
  }

  func updateReadiness(step: OnboardingPage, ready: Bool) {
    readiness.updateReadiness(step: step, ready: ready)
  }

  func notifyHost(event: PreferencesDelegateEvent) {
    onDelegate(event)
  }
}

public final class PreferencesChild {
  public let shell: PreferencesViewShell

  init(shell: PreferencesViewShell) {
    self.shell = shell
  }
}

public final class PreferencesBuilder {
  private let dependency: StepDependency

  public init(dependency: StepDependency) {
    self.dependency = dependency
  }

  @MainActor
  public func buildPreferences(
    onDelegate: @escaping (PreferencesDelegateEvent) -> Void
  ) -> PreferencesChild {
    let scope = mainImmediateStoreScope()
    let store = makePreferencesStore(
      environment: LivePreferencesEnvironment(
        readiness: dependency.readinessUpdates, onDelegate: onDelegate),
      scope: scope)
    let bridged = PreferencesKitStore(
      state: preferencesStateFlow(store: store),
      send: { store.send(action: $0) },
      teardown: {
        store.teardown()
        cancelStoreScope(scope: scope)
      })
    return PreferencesChild(shell: PreferencesViewShell(store: bridged))
  }
}

// MARK: - Progress

public typealias ProgressKitStore = BridgedStore<ProgressState, any ProgressAction>

@MainActor
public final class ProgressViewState: ObservableObject {
  /// One-based, from the projected page.
  @Published public internal(set) var stepNumber = 1
  public let stepCount = Int(STEP_COUNT)
  /// The pages the seam reported ready, in page order.
  @Published public internal(set) var readySteps: Set<Int> = []

  public init() {}
}

public final class ProgressViewShell: ViewShell {
  public let viewState = ProgressViewState()
  public let store: ProgressKitStore

  public init(store: ProgressKitStore) {
    self.store = store
    super.init()
  }

  override public func bind() {
    host.adopt(store)
    host.adopt(
      StateTransitions(state: store.$state) { [weak self] _, state in
        self?.viewState.stepNumber = Int(state.page.stepNumber)
        self?.viewState.readySteps = Set(
          state.readiness.compactMap { page, ready in
            ready.boolValue ? Int(page.stepNumber) : nil
          })
      })
  }

  public func appeared() { store.send(ProgressActionAppeared.shared) }

  /// The parent's projection: the level's page, as this row's action.
  public func pageChanged(_ page: OnboardingPage) {
    store.send(ProgressActionPageChanged(page: page))
  }
}

final class LiveProgressEnvironment: NSObject, ProgressEnvironment {
  private let readiness: any ReadinessObserving

  init(readiness: any ReadinessObserving) {
    self.readiness = readiness
  }

  func observeReadiness(onChange: @escaping (StepReadiness) -> Void) -> any ReadinessSubscription {
    readiness.observeReadiness(onChange: onChange)
  }
}

public final class ProgressChild {
  public let shell: ProgressViewShell

  init(shell: ProgressViewShell) {
    self.shell = shell
  }
}

public final class ProgressBuilder {
  private let dependency: ProgressDependency

  public init(dependency: ProgressDependency) {
    self.dependency = dependency
  }

  @MainActor
  public func buildProgress() -> ProgressChild {
    let scope = mainImmediateStoreScope()
    let store = makeProgressStore(
      environment: LiveProgressEnvironment(readiness: dependency.readinessObservation),
      scope: scope)
    let bridged = ProgressKitStore(
      state: progressStateFlow(store: store),
      send: { store.send(action: $0) },
      teardown: {
        store.teardown()
        cancelStoreScope(scope: scope)
      })
    return ProgressChild(shell: ProgressViewShell(store: bridged))
  }
}
