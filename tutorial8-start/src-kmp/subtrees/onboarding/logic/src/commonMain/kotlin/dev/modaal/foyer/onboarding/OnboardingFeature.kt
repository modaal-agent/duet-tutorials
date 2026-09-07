// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.onboarding

import dev.modaal.duet.kernel.Effect
import dev.modaal.duet.kernel.Reduced
import dev.modaal.foyer.name.NameDelegateEvent
import dev.modaal.foyer.ports.OnboardingPage
import dev.modaal.foyer.preferences.PreferencesDelegateEvent
import dev.modaal.foyer.welcome.WelcomeDelegateEvent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// The onboarding level: a page as route state, one step child mounted from
// it, the progress row mounted beside the steps for the level's lifetime,
// and Back as an action. Each step's Continued arrives as this level's
// action; the last one completes the level. Feature spec:
// parity/feature-specs/onboarding.md. Recordings: parity/fixtures/onboarding.*.

// MARK: - State

@Serializable
data class OnboardingState(
  /** Which step is mounted. The shell mounts from this value and projects it to the progress row. */
  val page: OnboardingPage = OnboardingPage.Welcome,
  /** What the name step handed up, once it did. */
  val name: String? = null,
  /** What the preferences step handed up, once it did. */
  val preferences: List<String> = emptyList(),
)

// MARK: - Actions

@Serializable(with = OnboardingActionSerializer::class)
sealed interface OnboardingAction {
  /** Shell report: Back, from the system or a button. Inert on the first page. */
  @Serializable @SerialName("back") data object Back : OnboardingAction

  /** The steps' delegate events, received as this level's actions. */
  @Serializable @SerialName("welcome") data class Welcome(val event: WelcomeDelegateEvent) : OnboardingAction

  @Serializable @SerialName("name") data class Name(val event: NameDelegateEvent) : OnboardingAction

  @Serializable
  @SerialName("preferences")
  data class Preferences(val event: PreferencesDelegateEvent) : OnboardingAction
}

// MARK: - Delegate events

@Serializable(with = OnboardingDelegateEventSerializer::class)
sealed interface OnboardingDelegateEvent {
  /** Every step is done; the root persists the answers and mounts main. */
  @Serializable
  @SerialName("completed")
  data class Completed(val name: String, val preferences: List<String>) : OnboardingDelegateEvent
}

// MARK: - Effect payloads

@Serializable(with = OnboardingEffectPayloadSerializer::class)
sealed interface OnboardingEffectPayload {
  @Serializable
  @SerialName("notifyListener")
  data class NotifyHost(val event: OnboardingDelegateEvent) : OnboardingEffectPayload
}

// MARK: - Reducer

/**
 * Each step's Continued moves the page forward and keeps what the step
 * handed up; the last one climbs `Completed` with the answers. Back moves
 * the page back and is inert on the first page. A step's event on another
 * page is inert: the shell mounts one step at a time, so it never happens
 * in production, and the guard keeps a replay honest.
 */
fun onboardingReducer(
  state: OnboardingState,
  action: OnboardingAction,
): Reduced<OnboardingState, OnboardingEffectPayload> =
  when (action) {
    OnboardingAction.Back ->
      when (state.page) {
        OnboardingPage.Welcome -> Reduced(state)
        OnboardingPage.Name -> Reduced(state.copy(page = OnboardingPage.Welcome))
        OnboardingPage.Preferences -> Reduced(state.copy(page = OnboardingPage.Name))
      }

    is OnboardingAction.Welcome ->
      if (state.page != OnboardingPage.Welcome) Reduced(state)
      else
        when (action.event) {
          WelcomeDelegateEvent.Continued -> Reduced(state.copy(page = OnboardingPage.Name))
        }

    is OnboardingAction.Name ->
      if (state.page != OnboardingPage.Name) Reduced(state)
      else
        when (val event = action.event) {
          is NameDelegateEvent.Continued ->
            Reduced(state.copy(page = OnboardingPage.Preferences, name = event.name))
        }

    is OnboardingAction.Preferences ->
      if (state.page != OnboardingPage.Preferences) Reduced(state)
      else
        when (val event = action.event) {
          is PreferencesDelegateEvent.Continued -> {
            val next = state.copy(preferences = event.preferences)
            Reduced(
              next,
              listOf(
                Effect.Run(
                  OnboardingEffectPayload.NotifyHost(
                    OnboardingDelegateEvent.Completed(next.name ?: "", next.preferences)))))
          }
        }
  }
