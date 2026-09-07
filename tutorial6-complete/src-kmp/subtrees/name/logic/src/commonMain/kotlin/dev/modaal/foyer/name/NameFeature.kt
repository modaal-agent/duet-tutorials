// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.name

import dev.modaal.duet.kernel.Effect
import dev.modaal.duet.kernel.Reduced
import dev.modaal.foyer.editname.validateDisplayName
import dev.modaal.foyer.ports.OnboardingPage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// The second onboarding step: a display name, validated by the same function
// the profile tree's editor uses. Readiness follows the draft and is
// published laterally on every change. Feature spec:
// parity/feature-specs/name.md. Recordings: parity/fixtures/name.*.

// MARK: - State

@Serializable
data class NameState(
  /** The field's text. */
  val draft: String = "",
  /** The draft passes validation. Published to the seam on every change. */
  val isReady: Boolean = false,
  /** The validation message to show after a refused Continue, cleared by the next edit. */
  val validation: String? = null,
)

// MARK: - Actions

@Serializable(with = NameActionSerializer::class)
sealed interface NameAction {
  @Serializable @SerialName("draftChanged") data class DraftChanged(val text: String) : NameAction

  @Serializable @SerialName("continueTapped") data object ContinueTapped : NameAction
}

// MARK: - Delegate events

@Serializable(with = NameDelegateEventSerializer::class)
sealed interface NameDelegateEvent {
  /** The step is done; the level keeps the name. */
  @Serializable @SerialName("continued") data class Continued(val name: String) : NameDelegateEvent
}

// MARK: - Effect payloads

@Serializable(with = NameEffectPayloadSerializer::class)
sealed interface NameEffectPayload {
  /** Tell the lateral seam this step's readiness. A void call; nothing re-enters. */
  @Serializable
  @SerialName("publishReadiness")
  data class PublishReadiness(val step: OnboardingPage, val ready: Boolean) : NameEffectPayload

  @Serializable
  @SerialName("notifyListener")
  data class NotifyHost(val event: NameDelegateEvent) : NameEffectPayload
}

// MARK: - Reducer

/**
 * Every edit re-validates the draft and publishes the readiness when it
 * changed. Continue on a valid draft climbs the trimmed name; on an invalid
 * one it shows the validation message and climbs nothing.
 */
fun nameReducer(state: NameState, action: NameAction): Reduced<NameState, NameEffectPayload> =
  when (action) {
    is NameAction.DraftChanged -> {
      val ready = validateDisplayName(action.text) == null
      val next = state.copy(draft = action.text, isReady = ready, validation = null)
      if (ready == state.isReady) {
        Reduced(next)
      } else {
        Reduced(
          next,
          listOf(Effect.Run(NameEffectPayload.PublishReadiness(OnboardingPage.Name, ready))))
      }
    }

    NameAction.ContinueTapped ->
      when (val validation = validateDisplayName(state.draft)) {
        null ->
          Reduced(
            state,
            listOf(
              Effect.Run(
                NameEffectPayload.NotifyHost(NameDelegateEvent.Continued(state.draft.trim())))))
        else -> Reduced(state.copy(validation = validation))
      }
  }
