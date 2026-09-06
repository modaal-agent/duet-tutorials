// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.main

import dev.modaal.duet.kernel.Effect
import dev.modaal.duet.kernel.Reduced
import dev.modaal.foyer.profile.ProfileDelegateEvent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// The main level: two tabs, both children mounted for the level's lifetime,
// and the sign-out relay. Feature spec: parity/feature-specs/main.md.
// Recordings: parity/fixtures/main.*.

// MARK: - State

@Serializable(with = MainTabSerializer::class)
sealed interface MainTab {
  @Serializable @SerialName("home") data object Home : MainTab

  @Serializable @SerialName("profile") data object Profile : MainTab
}

@Serializable data class MainState(val activeTab: MainTab = MainTab.Home)

// MARK: - Actions

@Serializable(with = MainActionSerializer::class)
sealed interface MainAction {
  @Serializable @SerialName("tabSelected") data class TabSelected(val tab: MainTab) : MainAction

  /** The profile tab's delegate events, received as this level's actions. */
  @Serializable
  @SerialName("profile")
  data class Profile(val event: ProfileDelegateEvent) : MainAction
}

// MARK: - Delegate events

@Serializable(with = MainDelegateEventSerializer::class)
sealed interface MainDelegateEvent {
  /** Relayed from the profile tree; the root raises the gate. */
  @Serializable @SerialName("signOutRequested") data object SignOutRequested : MainDelegateEvent
}

// MARK: - Effect payloads

@Serializable(with = MainEffectPayloadSerializer::class)
sealed interface MainEffectPayload {
  @Serializable
  @SerialName("notifyListener")
  data class NotifyHost(val event: MainDelegateEvent) : MainEffectPayload
}

// MARK: - Reducer

/** Tabs switch; the profile tree's sign-out request is relayed upward unchanged. */
fun mainReducer(state: MainState, action: MainAction): Reduced<MainState, MainEffectPayload> =
  when (action) {
    is MainAction.TabSelected -> Reduced(state.copy(activeTab = action.tab))

    is MainAction.Profile ->
      when (action.event) {
        ProfileDelegateEvent.SignOutRequested ->
          Reduced(
            state,
            listOf(Effect.Run(MainEffectPayload.NotifyHost(MainDelegateEvent.SignOutRequested))))
      }
  }
