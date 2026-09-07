// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.home

import dev.modaal.duet.kernel.Effect
import dev.modaal.duet.kernel.Reduced
import dev.modaal.foyer.ports.Entitlement
import dev.modaal.foyer.ports.Item
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// The home tab: a list loaded once from the items port, and the Insights
// card, locked while the entitlement is Free. The promo behind a locked card
// hands off to the upgrade flow, which the main level mounts. Feature spec:
// parity/feature-specs/home.md. Recordings: parity/fixtures/home.*.

// MARK: - State

/** What the tab presents over the list: the promo for a locked card, the summary for an unlocked one. */
@Serializable(with = HomePresentationSerializer::class)
sealed interface HomePresentation {
  @Serializable @SerialName("promo") data object Promo : HomePresentation

  @Serializable @SerialName("insights") data object Insights : HomePresentation
}

@Serializable
data class HomeState(
  val items: List<Item> = emptyList(),
  val isLoading: Boolean = false,
  /** The slice the root projects down. The reducer reads it and never writes it on its own. */
  val entitlement: Entitlement = Entitlement.Free,
  val presented: HomePresentation? = null,
)

// MARK: - Actions

@Serializable(with = HomeActionSerializer::class)
sealed interface HomeAction {
  /** Shell report: the tab is on screen. */
  @Serializable @SerialName("appeared") data object Appeared : HomeAction

  /** Environment report: the items port answered. */
  @Serializable @SerialName("itemsLoaded") data class ItemsLoaded(val items: List<Item>) : HomeAction

  /** The root's slice arrived: the entitlement worker wrote a new value. */
  @Serializable
  @SerialName("entitlementChanged")
  data class EntitlementChanged(val entitlement: Entitlement) : HomeAction

  /** Shell report: the Insights card. */
  @Serializable @SerialName("insightsTapped") data object InsightsTapped : HomeAction

  /** Shell report: the promo's one button. */
  @Serializable @SerialName("upgradeTapped") data object UpgradeTapped : HomeAction

  /** Shell report: the presented screen's Back. */
  @Serializable @SerialName("dismissed") data object Dismissed : HomeAction
}

// MARK: - Delegate events

@Serializable(with = HomeDelegateEventSerializer::class)
sealed interface HomeDelegateEvent {
  /** The promo's button: the main level mounts the upgrade flow. */
  @Serializable @SerialName("upgradeRequested") data object UpgradeRequested : HomeDelegateEvent
}

// MARK: - Effect payloads

@Serializable(with = HomeEffectPayloadSerializer::class)
sealed interface HomeEffectPayload {
  /** Ask the items port for the list; the answer re-enters as `ItemsLoaded`. */
  @Serializable @SerialName("loadItems") data object LoadItems : HomeEffectPayload

  @Serializable
  @SerialName("notifyListener")
  data class NotifyHost(val event: HomeDelegateEvent) : HomeEffectPayload
}

// MARK: - Reducer

/**
 * The list loads on the first appearance and stays. The Insights card opens
 * the promo while the entitlement is Free and the summary once it is
 * Premium; the promo's one tap closes it and asks the host for the upgrade
 * flow. The card unlocks when the entitlement stream emits, which reaches
 * this reducer as `EntitlementChanged`; nothing the flow reports does.
 */
fun homeReducer(state: HomeState, action: HomeAction): Reduced<HomeState, HomeEffectPayload> =
  when (action) {
    HomeAction.Appeared ->
      if (state.isLoading || state.items.isNotEmpty()) {
        Reduced(state)
      } else {
        Reduced(state.copy(isLoading = true), listOf(Effect.Run(HomeEffectPayload.LoadItems)))
      }

    is HomeAction.ItemsLoaded -> Reduced(state.copy(items = action.items, isLoading = false))

    is HomeAction.EntitlementChanged -> Reduced(state.copy(entitlement = action.entitlement))

    HomeAction.InsightsTapped ->
      Reduced(
        state.copy(
          presented =
            if (state.entitlement is Entitlement.Premium) HomePresentation.Insights
            else HomePresentation.Promo))

    HomeAction.UpgradeTapped ->
      Reduced(
        state.copy(presented = null),
        listOf(Effect.Run(HomeEffectPayload.NotifyHost(HomeDelegateEvent.UpgradeRequested))))

    HomeAction.Dismissed -> Reduced(state.copy(presented = null))
  }
