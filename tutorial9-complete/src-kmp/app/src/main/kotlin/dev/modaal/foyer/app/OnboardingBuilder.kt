// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.app

import dev.modaal.duet.kernel.Store
import dev.modaal.duet.services.telemetry.AnalyticsProviding
import dev.modaal.duet.services.telemetry.TrackedEvent
import dev.modaal.duet.shells.ChildSlot
import dev.modaal.duet.shells.StateTransitions
import dev.modaal.duet.shells.StoreHost
import dev.modaal.foyer.app.workers.OnboardingProgressWorker
import dev.modaal.foyer.name.NameAction
import dev.modaal.foyer.name.NameDelegateEvent
import dev.modaal.foyer.name.NameEffectPayload
import dev.modaal.foyer.name.NameEnvironment
import dev.modaal.foyer.name.NameState
import dev.modaal.foyer.name.makeNameStore
import dev.modaal.foyer.onboarding.OnboardingAction
import dev.modaal.foyer.onboarding.OnboardingDelegateEvent
import dev.modaal.foyer.onboarding.OnboardingEffectPayload
import dev.modaal.foyer.onboarding.OnboardingEnvironment
import dev.modaal.foyer.onboarding.OnboardingState
import dev.modaal.foyer.onboarding.makeOnboardingStore
import dev.modaal.foyer.ports.OnboardingPage
import dev.modaal.foyer.ports.ReadinessObserving
import dev.modaal.foyer.ports.ReadinessSubscription
import dev.modaal.foyer.ports.ReadinessUpdating
import dev.modaal.foyer.ports.StepReadiness
import dev.modaal.foyer.preferences.PreferencesAction
import dev.modaal.foyer.preferences.PreferencesDelegateEvent
import dev.modaal.foyer.preferences.PreferencesEffectPayload
import dev.modaal.foyer.preferences.PreferencesEnvironment
import dev.modaal.foyer.preferences.PreferencesState
import dev.modaal.foyer.preferences.makePreferencesStore
import dev.modaal.foyer.progress.ProgressAction
import dev.modaal.foyer.progress.ProgressEffectPayload
import dev.modaal.foyer.progress.ProgressEnvironment
import dev.modaal.foyer.progress.ProgressState
import dev.modaal.foyer.progress.makeProgressStore
import dev.modaal.foyer.welcome.WelcomeAction
import dev.modaal.foyer.welcome.WelcomeDelegateEvent
import dev.modaal.foyer.welcome.WelcomeEffectPayload
import dev.modaal.foyer.welcome.WelcomeEnvironment
import dev.modaal.foyer.welcome.WelcomeState
import dev.modaal.foyer.welcome.makeWelcomeStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// The onboarding level's composition, and the four leaves under it. The
// level's Component is the steps' and the progress row's lowest common
// ancestor, so it owns the one readiness worker they share and hands it
// down each Dependency chain as the narrow port each side declares.

typealias OnboardingStore = Store<OnboardingState, OnboardingAction, OnboardingEffectPayload>
typealias WelcomeStore = Store<WelcomeState, WelcomeAction, WelcomeEffectPayload>
typealias NameStore = Store<NameState, NameAction, NameEffectPayload>
typealias PreferencesStore = Store<PreferencesState, PreferencesAction, PreferencesEffectPayload>
typealias ProgressStore = Store<ProgressState, ProgressAction, ProgressEffectPayload>

/** What the onboarding level consumes from its parent: the analytics sink. Its subtree's needs are its own. */
interface OnboardingDependency : AnalyticsProviding

/** What a step consumes: the seam's producer side. */
interface StepDependency {
  val readinessUpdates: ReadinessUpdating
}

/** What the progress row consumes: the seam's consumer side. */
interface ProgressDependency {
  val readinessObservation: ReadinessObserving
}

/**
 * Owns the readiness worker once, lazily, and satisfies both narrow ports
 * with it. Neither a step nor the progress row can name the other side.
 */
class OnboardingComponent(dependency: OnboardingDependency, scope: CoroutineScope) :
  OnboardingDependency by dependency, StepDependency, ProgressDependency {
  val progressWorker: OnboardingProgressWorker by lazy { OnboardingProgressWorker(scope) }

  override val readinessUpdates: ReadinessUpdating
    get() = progressWorker

  override val readinessObservation: ReadinessObserving
    get() = progressWorker

  fun environment(onDelegate: (OnboardingDelegateEvent) -> Unit): OnboardingEnvironment =
    object : OnboardingEnvironment {
      override fun notifyHost(event: OnboardingDelegateEvent) = onDelegate(event)

      override fun track(event: TrackedEvent) = analytics.track(event)
    }
}

/** The step the level has mounted, as the render layer sees it. */
sealed interface OnboardingStepMount {
  data class Welcome(val store: WelcomeStore) : OnboardingStepMount

  data class Name(val store: NameStore) : OnboardingStepMount

  data class Preferences(val store: PreferencesStore) : OnboardingStepMount
}

/** What one onboarding mount owns: its store, the progress row, and the step the page names. */
class OnboardingMount(
  val store: OnboardingStore,
  val progress: ProgressStore,
  private val host: StoreHost,
) {
  private val mutableStep = MutableStateFlow<OnboardingStepMount?>(null)
  val step: StateFlow<OnboardingStepMount?> = mutableStep

  internal fun publish(step: OnboardingStepMount?) {
    mutableStep.value = step
  }

  fun teardown() = host.teardownAll()
}

class OnboardingBuilder(private val dependency: OnboardingDependency) {
  /** `page` is the restored route sliver, or null for the first page. */
  fun buildOnboarding(
    page: OnboardingPage?,
    onDelegate: (OnboardingDelegateEvent) -> Unit,
    scope: CoroutineScope,
  ): OnboardingMount {
    val component = OnboardingComponent(dependency, scope)
    val host = StoreHost(scope)
    val store =
      host.host(
        makeOnboardingStore(
          page = page ?: OnboardingPage.Welcome,
          environment = component.environment(onDelegate),
          scope = scope,
        ))
    val progress = host.host(ProgressBuilder(component).buildProgress(scope))
    val mount = OnboardingMount(store, progress, host)

    // Exactly one step at a time, keyed on the page. Each step's delegate
    // events route to the level's store as actions.
    val step =
      host.adopt(
        ChildSlot<OnboardingPage, OnboardingStepMount>(
          build = { page ->
            when (page) {
              OnboardingPage.Welcome ->
                OnboardingStepMount.Welcome(
                  WelcomeBuilder(component)
                    .buildWelcome(onDelegate = { store.send(OnboardingAction.Welcome(it)) }, scope = scope))
              OnboardingPage.Name ->
                OnboardingStepMount.Name(
                  NameBuilder(component)
                    .buildName(onDelegate = { store.send(OnboardingAction.Name(it)) }, scope = scope))
              OnboardingPage.Preferences ->
                OnboardingStepMount.Preferences(
                  PreferencesBuilder(component)
                    .buildPreferences(
                      onDelegate = { store.send(OnboardingAction.Preferences(it)) },
                      scope = scope,
                    ))
            }
          },
          teardown = {
            when (it) {
              is OnboardingStepMount.Welcome -> it.store.teardown()
              is OnboardingStepMount.Name -> it.store.teardown()
              is OnboardingStepMount.Preferences -> it.store.teardown()
            }
          },
        ))
    host.adopt(
      StateTransitions(scope, store.state) { _, state ->
        // State down: the page reaches the progress row as its own action.
        progress.send(ProgressAction.PageChanged(state.page))
        step.reconcile(state.page)
        mount.publish(step.activeHandle)
      })
    // The seam's worker, adopted for the level's lifetime: the ancestor
    // brackets it, the steps and the row only hold their ports to it.
    host.adopt(component.progressWorker)
    return mount
  }
}

// MARK: - The four leaves

class WelcomeBuilder(private val dependency: StepDependency) {
  fun buildWelcome(onDelegate: (WelcomeDelegateEvent) -> Unit, scope: CoroutineScope): WelcomeStore =
    makeWelcomeStore(
      environment =
        object : WelcomeEnvironment {
          override fun updateReadiness(step: OnboardingPage, ready: Boolean) =
            dependency.readinessUpdates.updateReadiness(step, ready)

          override fun notifyHost(event: WelcomeDelegateEvent) = onDelegate(event)
        },
      scope = scope,
    )
}

class NameBuilder(private val dependency: StepDependency) {
  fun buildName(onDelegate: (NameDelegateEvent) -> Unit, scope: CoroutineScope): NameStore =
    makeNameStore(
      environment =
        object : NameEnvironment {
          override fun updateReadiness(step: OnboardingPage, ready: Boolean) =
            dependency.readinessUpdates.updateReadiness(step, ready)

          override fun notifyHost(event: NameDelegateEvent) = onDelegate(event)
        },
      scope = scope,
    )
}

class PreferencesBuilder(private val dependency: StepDependency) {
  fun buildPreferences(
    onDelegate: (PreferencesDelegateEvent) -> Unit,
    scope: CoroutineScope,
  ): PreferencesStore =
    makePreferencesStore(
      environment =
        object : PreferencesEnvironment {
          override fun updateReadiness(step: OnboardingPage, ready: Boolean) =
            dependency.readinessUpdates.updateReadiness(step, ready)

          override fun notifyHost(event: PreferencesDelegateEvent) = onDelegate(event)
        },
      scope = scope,
    )
}

class ProgressBuilder(private val dependency: ProgressDependency) {
  fun buildProgress(scope: CoroutineScope): ProgressStore =
    makeProgressStore(
      environment =
        object : ProgressEnvironment {
          override fun observeReadiness(onChange: (StepReadiness) -> Unit): ReadinessSubscription =
            dependency.readinessObservation.observeReadiness(onChange)
        },
      scope = scope,
    )
}
