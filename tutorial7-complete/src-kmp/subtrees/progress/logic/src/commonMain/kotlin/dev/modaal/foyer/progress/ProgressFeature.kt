// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.progress

import dev.modaal.duet.kernel.Effect
import dev.modaal.duet.kernel.Reduced
import dev.modaal.foyer.ports.OnboardingPage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// The progress row beside the onboarding steps. Two values reach it by two
// routes: the page, projected down from the level that owns it, and each
// step's readiness, arriving laterally from siblings it never sees. Feature
// spec: parity/feature-specs/progress.md. Recordings: parity/fixtures/progress.*.

// MARK: - State

@Serializable
data class ProgressState(
  /** The level's page, projected down. The reducer reads it and never writes it on its own. */
  val page: OnboardingPage = OnboardingPage.Welcome,
  /** Each step's readiness as the seam last reported it; a step absent here has not reported. */
  val readiness: Map<OnboardingPage, Boolean> = emptyMap(),
  /** The observation is running; one per mount. */
  val isObserving: Boolean = false,
)

// MARK: - Actions

@Serializable(with = ProgressActionSerializer::class)
sealed interface ProgressAction {
  /** Shell report: the row is on screen. */
  @Serializable @SerialName("appeared") data object Appeared : ProgressAction

  /** The level's slice arrived: the page moved. */
  @Serializable @SerialName("pageChanged") data class PageChanged(val page: OnboardingPage) : ProgressAction

  /** Environment report: the seam carried a step's readiness. */
  @Serializable
  @SerialName("readinessChanged")
  data class ReadinessChanged(val step: OnboardingPage, val ready: Boolean) : ProgressAction
}

// MARK: - Effect payloads

@Serializable(with = ProgressEffectPayloadSerializer::class)
sealed interface ProgressEffectPayload {
  /** Subscribe to the seam; every value re-enters as `ReadinessChanged`, until the store tears down. */
  @Serializable @SerialName("observeReadiness") data object ObserveReadiness : ProgressEffectPayload
}

// MARK: - Reducer

/**
 * The first appearance starts the observation and nothing else does. The
 * page and the readiness are written as they arrive; the row derives "step n
 * of 3" and the ticks from them.
 */
fun progressReducer(
  state: ProgressState,
  action: ProgressAction,
): Reduced<ProgressState, ProgressEffectPayload> =
  when (action) {
    ProgressAction.Appeared ->
      if (state.isObserving) Reduced(state)
      else
        Reduced(
          state.copy(isObserving = true),
          listOf(Effect.Run(ProgressEffectPayload.ObserveReadiness)))

    is ProgressAction.PageChanged -> Reduced(state.copy(page = action.page))

    is ProgressAction.ReadinessChanged ->
      Reduced(state.copy(readiness = state.readiness + (action.step to action.ready)))
  }

/** The row's number, one-based, from the projected page. */
val OnboardingPage.stepNumber: Int
  get() = ordinal + 1

/** How many steps the level has. */
val STEP_COUNT: Int = OnboardingPage.entries.size
