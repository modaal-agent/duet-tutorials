// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import FoyerBridge
import FoyerKit
import Foundation

// The onboarding level's composition triple. Hand-written rather than
// generated: the level's Component owns an object, the readiness worker,
// and a generated Component forwards only.

/// What the onboarding level consumes from its parent: nothing. Its
/// subtree's needs are its own.
public protocol OnboardingDependency: AnyObject {}

/// Owns the readiness worker once, lazily, as the steps' and the progress
/// row's lowest common ancestor, and satisfies both narrow ports with its
/// seam.
@MainActor
final class OnboardingComponent: StepDependency, ProgressDependency {
  private let dependency: OnboardingDependency
  private(set) lazy var progressWorker = OnboardingProgressWorker()

  init(dependency: OnboardingDependency) {
    self.dependency = dependency
  }

  var readinessUpdates: any ReadinessUpdating { progressWorker.seam }
  var readinessObservation: any ReadinessObserving { progressWorker.seam }
}

final class LiveOnboardingEnvironment: NSObject, OnboardingEnvironment {
  private let onDelegate: (OnboardingDelegateEvent) -> Void

  init(onDelegate: @escaping (OnboardingDelegateEvent) -> Void) {
    self.onDelegate = onDelegate
  }

  func notifyHost(event: OnboardingDelegateEvent) {
    onDelegate(event)
  }
}

/// The step builders, over the level's Component. The shell calls these
/// when the page names a step; each step's delegate events route back to the
/// level's store as actions.
final class OnboardingStepMounter: OnboardingStepMounting {
  private let component: OnboardingComponent

  init(component: OnboardingComponent) {
    self.component = component
  }

  func mountWelcome(onDelegate: @escaping (WelcomeDelegateEvent) -> Void) -> WelcomeChild {
    WelcomeBuilder(dependency: component).buildWelcome(onDelegate: onDelegate)
  }

  func mountName(onDelegate: @escaping (NameDelegateEvent) -> Void) -> NameChild {
    NameBuilder(dependency: component).buildName(onDelegate: onDelegate)
  }

  func mountPreferences(
    onDelegate: @escaping (PreferencesDelegateEvent) -> Void
  ) -> PreferencesChild {
    PreferencesBuilder(dependency: component).buildPreferences(onDelegate: onDelegate)
  }
}

// MARK: - Builder

public final class OnboardingChild {
  public let shell: OnboardingViewShell

  init(shell: OnboardingViewShell) {
    self.shell = shell
  }
}

public final class OnboardingBuilder {
  private let dependency: OnboardingDependency

  public init(dependency: OnboardingDependency) {
    self.dependency = dependency
  }

  /// Builds the level, the progress row and the worker the shell adopts at
  /// activation. `page` is the restored route sliver, or the first page.
  @MainActor
  public func buildOnboarding(
    page: OnboardingPage,
    onDelegate: @escaping (OnboardingDelegateEvent) -> Void
  ) -> OnboardingChild {
    let component = OnboardingComponent(dependency: dependency)
    let scope = mainImmediateStoreScope()
    let store = makeOnboardingStore(
      page: page,
      environment: LiveOnboardingEnvironment(onDelegate: onDelegate),
      scope: scope)
    let bridged = OnboardingKitStore(
      state: onboardingStateFlow(store: store),
      send: { store.send(action: $0) },
      teardown: {
        store.teardown()
        cancelStoreScope(scope: scope)
      })
    let progress = ProgressBuilder(dependency: component).buildProgress()
    let shell = OnboardingViewShell(
      store: bridged,
      progress: progress,
      mounter: OnboardingStepMounter(component: component),
      worker: component.progressWorker)
    return OnboardingChild(shell: shell)
  }
}
