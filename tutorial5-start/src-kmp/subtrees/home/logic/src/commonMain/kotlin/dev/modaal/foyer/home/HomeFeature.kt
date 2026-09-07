// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.home

import dev.modaal.duet.kernel.Effect
import dev.modaal.duet.kernel.Reduced
import dev.modaal.foyer.ports.Entitlement
import dev.modaal.foyer.ports.Item
import dev.modaal.foyer.ports.Plan
import dev.modaal.foyer.ports.PurchaseOutcome
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// The home tab: a list loaded once from the items port, and the Insights
// card, locked while the entitlement is Free. Feature spec:
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
  /** The in-flight latch for the promo's one-tap purchase. */
  val isPurchasing: Boolean = false,
  /** The last purchase failure to show, cleared by the next attempt. */
  val failure: String? = null,
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
  @Serializable @SerialName("purchaseTapped") data object PurchaseTapped : HomeAction

  /** Environment report: the purchases port answered. */
  @Serializable
  @SerialName("purchaseFinished")
  data class PurchaseFinished(val outcome: PurchaseOutcome) : HomeAction

  /** Shell report: the presented screen's Back. */
  @Serializable @SerialName("dismissed") data object Dismissed : HomeAction
}

// MARK: - Effect payloads

@Serializable(with = HomeEffectPayloadSerializer::class)
sealed interface HomeEffectPayload {
  /** Ask the items port for the list; the answer re-enters as `ItemsLoaded`. */
  @Serializable @SerialName("loadItems") data object LoadItems : HomeEffectPayload

  /** Ask the purchases port to buy a plan; the answer re-enters as `PurchaseFinished`. */
  @Serializable @SerialName("purchase") data class Purchase(val plan: Plan) : HomeEffectPayload
}

// MARK: - Reducer

/**
 * The list loads on the first appearance and stays. The Insights card opens
 * the promo while the entitlement is Free and the summary once it is
 * Premium; the promo's one tap buys the monthly plan. A finished purchase
 * closes the promo and nothing more: the card unlocks when the entitlement
 * stream emits, which reaches this reducer as `EntitlementChanged`.
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

    HomeAction.PurchaseTapped ->
      if (state.isPurchasing) {
        Reduced(state)
      } else {
        Reduced(
          state.copy(isPurchasing = true, failure = null),
          listOf(Effect.Run(HomeEffectPayload.Purchase(Plan.Monthly))))
      }

    is HomeAction.PurchaseFinished ->
      when (val outcome = action.outcome) {
        is PurchaseOutcome.Purchased -> Reduced(state.copy(isPurchasing = false, presented = null))
        is PurchaseOutcome.Failed ->
          Reduced(state.copy(isPurchasing = false, failure = outcome.reason))
      }

    HomeAction.Dismissed -> Reduced(state.copy(presented = null))
  }
