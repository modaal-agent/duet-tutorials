// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.preferences

import dev.modaal.duet.kernel.Effect
import dev.modaal.duet.kernel.Reduced
import dev.modaal.foyer.ports.OnboardingPage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// The last onboarding step: a handful of toggles, at least one of which must
// be on. Readiness follows the selection and is published laterally on every
// change. Feature spec: parity/feature-specs/preferences.md. Recordings:
// parity/fixtures/preferences.*.

object PreferenceKeys {
  /** The toggles, in the order the screen lists them. */
  val ALL: List<String> = listOf("digest", "reminders", "tips")
}

// MARK: - State

@Serializable
data class PreferencesState(
  /** The keys that are on, in `PreferenceKeys.ALL`'s order. */
  val selected: List<String> = emptyList(),
  /** At least one toggle is on. Published to the seam on every change. */
  val isReady: Boolean = false,
)

// MARK: - Actions

@Serializable(with = PreferencesActionSerializer::class)
sealed interface PreferencesAction {
  @Serializable
  @SerialName("preferenceToggled")
  data class PreferenceToggled(val key: String) : PreferencesAction

  @Serializable @SerialName("continueTapped") data object ContinueTapped : PreferencesAction
}

// MARK: - Delegate events

@Serializable(with = PreferencesDelegateEventSerializer::class)
sealed interface PreferencesDelegateEvent {
  /** The step is done; the level keeps the selection. */
  @Serializable
  @SerialName("continued")
  data class Continued(val preferences: List<String>) : PreferencesDelegateEvent
}

// MARK: - Effect payloads

@Serializable(with = PreferencesEffectPayloadSerializer::class)
sealed interface PreferencesEffectPayload {
  /** Tell the lateral seam this step's readiness. A void call; nothing re-enters. */
  @Serializable
  @SerialName("publishReadiness")
  data class PublishReadiness(val step: OnboardingPage, val ready: Boolean) : PreferencesEffectPayload

  @Serializable
  @SerialName("notifyListener")
  data class NotifyHost(val event: PreferencesDelegateEvent) : PreferencesEffectPayload
}

// MARK: - Reducer

/**
 * A toggle flips one key and publishes the readiness when it changed; an
 * unknown key is inert. Continue with nothing on is inert; with a selection
 * it climbs the keys.
 */
fun preferencesReducer(
  state: PreferencesState,
  action: PreferencesAction,
): Reduced<PreferencesState, PreferencesEffectPayload> =
  when (action) {
    is PreferencesAction.PreferenceToggled ->
      if (action.key !in PreferenceKeys.ALL) {
        Reduced(state)
      } else {
        val selected =
          if (action.key in state.selected) state.selected - action.key
          else PreferenceKeys.ALL.filter { it in state.selected || it == action.key }
        val ready = selected.isNotEmpty()
        val next = state.copy(selected = selected, isReady = ready)
        if (ready == state.isReady) {
          Reduced(next)
        } else {
          Reduced(
            next,
            listOf(
              Effect.Run(
                PreferencesEffectPayload.PublishReadiness(OnboardingPage.Preferences, ready))))
        }
      }

    PreferencesAction.ContinueTapped ->
      if (!state.isReady) {
        Reduced(state)
      } else {
        Reduced(
          state,
          listOf(
            Effect.Run(
              PreferencesEffectPayload.NotifyHost(
                PreferencesDelegateEvent.Continued(state.selected)))))
      }
  }
