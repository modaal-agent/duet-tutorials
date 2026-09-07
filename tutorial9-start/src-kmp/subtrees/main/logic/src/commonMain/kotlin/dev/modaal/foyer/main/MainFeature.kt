// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.main

import dev.modaal.duet.kernel.Effect
import dev.modaal.duet.kernel.Reduced
import dev.modaal.foyer.home.HomeDelegateEvent
import dev.modaal.foyer.ports.DeepLink
import dev.modaal.foyer.profile.ProfileDelegateEvent
import dev.modaal.foyer.upgrade.UpgradeDelegateEvent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// The main level: two tabs, both children mounted for the level's lifetime,
// one sheet slot the upgrade flow mounts in, the sign-out relay and the
// deep-link fan-out. Feature spec: parity/feature-specs/main.md. Recordings:
// parity/fixtures/main.*.

// MARK: - State

@Serializable(with = MainTabSerializer::class)
sealed interface MainTab {
  @Serializable @SerialName("home") data object Home : MainTab

  @Serializable @SerialName("profile") data object Profile : MainTab
}

/** What the sheet slot mounts over the tabs. One case; the slot is the extension point. */
@Serializable(with = MainSheetSerializer::class)
sealed interface MainSheet {
  @Serializable @SerialName("upgrade") data object Upgrade : MainSheet
}

@Serializable
data class MainState(
  val activeTab: MainTab = MainTab.Home,
  /** The mounted sheet, or none. The shell presents from this value, in each platform's idiom. */
  val sheet: MainSheet? = null,
)

// MARK: - Actions

@Serializable(with = MainActionSerializer::class)
sealed interface MainAction {
  @Serializable @SerialName("tabSelected") data class TabSelected(val tab: MainTab) : MainAction

  /** The home tab's delegate events, received as this level's actions. */
  @Serializable @SerialName("home") data class Home(val event: HomeDelegateEvent) : MainAction

  /** The profile tab's delegate events. */
  @Serializable
  @SerialName("profile")
  data class Profile(val event: ProfileDelegateEvent) : MainAction

  /** The upgrade flow's delegate events. */
  @Serializable
  @SerialName("upgrade")
  data class Upgrade(val event: UpgradeDelegateEvent) : MainAction

  /** The root forwarded a deep link into this level. */
  @Serializable @SerialName("openLink") data class OpenLink(val link: DeepLink) : MainAction
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

  /** Hand a link down to the profile tab; the shell bridges it into `Profile.OpenLink`. */
  @Serializable @SerialName("forwardLink") data class ForwardLink(val link: DeepLink) : MainEffectPayload
}

// MARK: - Reducer

/**
 * Tabs switch. Either tab's `UpgradeRequested` mounts the flow in the sheet
 * slot, and the flow's `Completed` or `Dismissed` clears it; nothing the flow
 * reports touches the entitlement, which reaches the tabs from the root's
 * slice. The profile tree's sign-out request is relayed upward unchanged. A
 * forwarded link sets the tab and the sheet, or travels on down to the
 * profile tab.
 */
fun mainReducer(state: MainState, action: MainAction): Reduced<MainState, MainEffectPayload> =
  when (action) {
    is MainAction.TabSelected -> Reduced(state.copy(activeTab = action.tab))

    is MainAction.Home ->
      when (action.event) {
        HomeDelegateEvent.UpgradeRequested -> Reduced(state.copy(sheet = MainSheet.Upgrade))
      }

    is MainAction.Profile ->
      when (action.event) {
        ProfileDelegateEvent.UpgradeRequested -> Reduced(state.copy(sheet = MainSheet.Upgrade))
        ProfileDelegateEvent.SignOutRequested ->
          Reduced(
            state,
            listOf(Effect.Run(MainEffectPayload.NotifyHost(MainDelegateEvent.SignOutRequested))))
      }

    is MainAction.Upgrade ->
      when (action.event) {
        UpgradeDelegateEvent.Completed,
        UpgradeDelegateEvent.Dismissed -> Reduced(state.copy(sheet = null))
      }

    is MainAction.OpenLink ->
      when (action.link) {
        DeepLink.Upgrade -> Reduced(state.copy(sheet = MainSheet.Upgrade))
        DeepLink.ProfileAccount ->
          Reduced(
            state.copy(activeTab = MainTab.Profile),
            listOf(Effect.Run(MainEffectPayload.ForwardLink(action.link))))
      }
  }
