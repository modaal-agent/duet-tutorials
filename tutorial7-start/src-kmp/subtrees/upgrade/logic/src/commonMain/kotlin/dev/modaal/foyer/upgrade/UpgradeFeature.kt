// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.upgrade

import dev.modaal.duet.kernel.Effect
import dev.modaal.duet.kernel.Reduced
import dev.modaal.foyer.ports.Plan
import dev.modaal.foyer.ports.PlanOffer
import dev.modaal.foyer.ports.PurchaseOutcome
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// The upgrade flow: three steps as route state, one purchase through the
// purchases port, and one delegate event when the user leaves the last step.
// The entitlement never appears here: the purchase's answer moves the step,
// and the card unlocks from the entitlement stream through the root. Feature
// spec: parity/feature-specs/upgrade.md. Recordings: parity/fixtures/upgrade.*.

// MARK: - State

/** Where the flow is. The shell renders exactly one step from this value. */
@Serializable(with = UpgradeStepSerializer::class)
sealed interface UpgradeStep {
  @Serializable @SerialName("plans") data object Plans : UpgradeStep

  @Serializable @SerialName("confirm") data class Confirm(val plan: Plan) : UpgradeStep

  @Serializable @SerialName("done") data object Done : UpgradeStep
}

@Serializable
data class UpgradeState(
  val step: UpgradeStep = UpgradeStep.Plans,
  /** The plans on offer, empty until the port answers. */
  val offers: List<PlanOffer> = emptyList(),
  /** The in-flight latch: one purchase at a time. */
  val isPurchasing: Boolean = false,
  /** The last purchase failure to show on the confirm step, cleared by the next attempt. */
  val failure: String? = null,
)

// MARK: - Actions

@Serializable(with = UpgradeActionSerializer::class)
sealed interface UpgradeAction {
  /** Shell report: the flow is on screen. */
  @Serializable @SerialName("appeared") data object Appeared : UpgradeAction

  /** Environment report: the purchases port listed its plans. */
  @Serializable @SerialName("plansLoaded") data class PlansLoaded(val offers: List<PlanOffer>) : UpgradeAction

  /** Shell report: a plan card on the first step. */
  @Serializable @SerialName("planSelected") data class PlanSelected(val plan: Plan) : UpgradeAction

  /** Shell report: the confirm step's button. */
  @Serializable @SerialName("confirmTapped") data object ConfirmTapped : UpgradeAction

  /** Environment report: the purchases port answered. */
  @Serializable
  @SerialName("purchaseFinished")
  data class PurchaseFinished(val outcome: PurchaseOutcome) : UpgradeAction

  /** Shell report: Back, on whichever step. Back is an action; the step is the route. */
  @Serializable @SerialName("back") data object Back : UpgradeAction

  /** Shell report: the last step's button. */
  @Serializable @SerialName("doneTapped") data object DoneTapped : UpgradeAction

  /** Shell report: the sheet was dismissed from outside the flow (a swipe, a close button). */
  @Serializable @SerialName("dismissTapped") data object DismissTapped : UpgradeAction
}

// MARK: - Delegate events

@Serializable(with = UpgradeDelegateEventSerializer::class)
sealed interface UpgradeDelegateEvent {
  /** The user left the last step. The host clears the sheet; the entitlement arrived through the stream already. */
  @Serializable @SerialName("completed") data object Completed : UpgradeDelegateEvent

  /** The user left without buying. */
  @Serializable @SerialName("dismissed") data object Dismissed : UpgradeDelegateEvent
}

// MARK: - Effect payloads

@Serializable(with = UpgradeEffectPayloadSerializer::class)
sealed interface UpgradeEffectPayload {
  /** Ask the purchases port for its plans; the answer re-enters as `PlansLoaded`. */
  @Serializable @SerialName("loadPlans") data object LoadPlans : UpgradeEffectPayload

  /** Ask the purchases port to buy a plan; the answer re-enters as `PurchaseFinished`. */
  @Serializable @SerialName("purchase") data class Purchase(val plan: Plan) : UpgradeEffectPayload

  @Serializable
  @SerialName("notifyListener")
  data class NotifyHost(val event: UpgradeDelegateEvent) : UpgradeEffectPayload
}

// MARK: - Reducer

/**
 * The plans load on the first appearance. A selected plan moves to the
 * confirm step; Back from there returns to the plans, and Back on the plans
 * dismisses the flow. Confirming buys the plan, one purchase at a time; a
 * purchased answer moves to the done step, a refusal shows its reason on the
 * confirm step. Done, Back or a dismissal on the last step completes the
 * flow. Neither answer writes an entitlement: the stream does, through the
 * root.
 */
fun upgradeReducer(
  state: UpgradeState,
  action: UpgradeAction,
): Reduced<UpgradeState, UpgradeEffectPayload> =
  when (action) {
    UpgradeAction.Appeared ->
      if (state.offers.isNotEmpty()) Reduced(state)
      else Reduced(state, listOf(Effect.Run(UpgradeEffectPayload.LoadPlans)))

    is UpgradeAction.PlansLoaded -> Reduced(state.copy(offers = action.offers))

    is UpgradeAction.PlanSelected ->
      if (state.step != UpgradeStep.Plans) Reduced(state)
      else Reduced(state.copy(step = UpgradeStep.Confirm(action.plan), failure = null))

    UpgradeAction.ConfirmTapped ->
      when (val step = state.step) {
        is UpgradeStep.Confirm ->
          if (state.isPurchasing) {
            Reduced(state)
          } else {
            Reduced(
              state.copy(isPurchasing = true, failure = null),
              listOf(Effect.Run(UpgradeEffectPayload.Purchase(step.plan))))
          }
        else -> Reduced(state)
      }

    is UpgradeAction.PurchaseFinished ->
      when (val outcome = action.outcome) {
        is PurchaseOutcome.Purchased ->
          Reduced(state.copy(isPurchasing = false, step = UpgradeStep.Done))
        is PurchaseOutcome.Failed ->
          Reduced(state.copy(isPurchasing = false, failure = outcome.reason))
      }

    UpgradeAction.Back ->
      when (state.step) {
        is UpgradeStep.Confirm ->
          if (state.isPurchasing) Reduced(state)
          else Reduced(state.copy(step = UpgradeStep.Plans, failure = null))
        UpgradeStep.Plans -> Reduced(state, listOf(dismissed))
        UpgradeStep.Done -> Reduced(state, listOf(completed))
      }

    UpgradeAction.DoneTapped ->
      if (state.step != UpgradeStep.Done) Reduced(state) else Reduced(state, listOf(completed))

    UpgradeAction.DismissTapped ->
      if (state.step == UpgradeStep.Done) Reduced(state, listOf(completed))
      else Reduced(state, listOf(dismissed))
  }

private val dismissed: Effect<UpgradeEffectPayload> =
  Effect.Run(UpgradeEffectPayload.NotifyHost(UpgradeDelegateEvent.Dismissed))

private val completed: Effect<UpgradeEffectPayload> =
  Effect.Run(UpgradeEffectPayload.NotifyHost(UpgradeDelegateEvent.Completed))
