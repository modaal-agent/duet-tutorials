// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.welcome

import dev.modaal.duet.kernel.Effect
import dev.modaal.duet.kernel.Reduced
import dev.modaal.foyer.ports.OnboardingPage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// The first onboarding step: nothing to enter, so it is ready at mount. It
// publishes that readiness laterally, for the progress row it never sees.
// Feature spec: parity/feature-specs/welcome.md. Recordings:
// parity/fixtures/welcome.*.

// MARK: - State

@Serializable
data class WelcomeState(
  /** Ready to continue. Nothing on this step can make it false. */
  val isReady: Boolean = true,
)

// MARK: - Actions

@Serializable(with = WelcomeActionSerializer::class)
sealed interface WelcomeAction {
  /** Shell report: the step is on screen. */
  @Serializable @SerialName("appeared") data object Appeared : WelcomeAction

  @Serializable @SerialName("continueTapped") data object ContinueTapped : WelcomeAction
}

// MARK: - Delegate events

@Serializable(with = WelcomeDelegateEventSerializer::class)
sealed interface WelcomeDelegateEvent {
  @Serializable @SerialName("continued") data object Continued : WelcomeDelegateEvent
}

// MARK: - Effect payloads

@Serializable(with = WelcomeEffectPayloadSerializer::class)
sealed interface WelcomeEffectPayload {
  /** Tell the lateral seam this step's readiness. A void call; nothing re-enters. */
  @Serializable
  @SerialName("publishReadiness")
  data class PublishReadiness(val step: OnboardingPage, val ready: Boolean) : WelcomeEffectPayload

  @Serializable
  @SerialName("notifyListener")
  data class NotifyHost(val event: WelcomeDelegateEvent) : WelcomeEffectPayload
}

// MARK: - Reducer

/** Appearing publishes the readiness; Continue climbs to the level. */
fun welcomeReducer(
  state: WelcomeState,
  action: WelcomeAction,
): Reduced<WelcomeState, WelcomeEffectPayload> =
  when (action) {
    WelcomeAction.Appeared ->
      Reduced(
        state,
        listOf(
          Effect.Run(WelcomeEffectPayload.PublishReadiness(OnboardingPage.Welcome, state.isReady))))

    WelcomeAction.ContinueTapped ->
      Reduced(
        state,
        listOf(Effect.Run(WelcomeEffectPayload.NotifyHost(WelcomeDelegateEvent.Continued))))
  }
