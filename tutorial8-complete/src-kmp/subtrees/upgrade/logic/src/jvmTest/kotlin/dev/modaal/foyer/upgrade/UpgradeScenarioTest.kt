// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.upgrade

import dev.modaal.duet.kernel.Effect
import dev.modaal.duet.test.*
import dev.modaal.foyer.ports.Plan
import dev.modaal.foyer.ports.PlanOffer
import dev.modaal.foyer.ports.PurchaseFailure
import dev.modaal.foyer.ports.PurchaseOutcome
import kotlin.test.Test

/** The scenario the upgrade recordings are compiled from. */
class UpgradeScenarioTest {
  @Test
  fun upgradeScenario() {
    val s =
      scenario<UpgradeState, UpgradeAction, UpgradeEffectPayload>(
        feature = "upgrade",
        description =
          "The upgrade flow loads its plans once, walks plans to confirm to done as " +
            "route state with Back as an action, buys one plan at a time through the " +
            "purchases port, shows a refusal on the confirm step, and completes or " +
            "dismisses through its delegate. It never writes an entitlement.",
        source =
          "src-kmp/subtrees/upgrade/logic/src/jvmTest/kotlin/" +
            "dev/modaal/foyer/upgrade/UpgradeScenarioTest.kt",
      ) {
        given(UpgradeState())

        whenAction("the flow appears", UpgradeAction.Appeared)
        thenEffects("exactly the plans load") {
          it == effectsOf<UpgradeEffectPayload>(Effect.Run(UpgradeEffectPayload.LoadPlans))
        }
        whenAction("the port lists its plans", UpgradeAction.PlansLoaded(offers))
        then("the plans are there, on the first step") {
          it.offers == offers && it.step == UpgradeStep.Plans
        }

        branch("plans load once") {
          whenAction("the flow appears again", UpgradeAction.Appeared)
          thenEffects("nothing: the plans are loaded") { it.isEmpty() }
        }

        branch("select confirm purchase done") {
          whenAction("the yearly card", UpgradeAction.PlanSelected(Plan.Yearly))
          then("the confirm step, for that plan") { it.step == UpgradeStep.Confirm(Plan.Yearly) }
          thenEffects("nothing: moving is a state change") { it.isEmpty() }
          whenAction("Confirm", UpgradeAction.ConfirmTapped)
          then("a purchase is in flight") { it.isPurchasing && it.failure == null }
          thenEffects("exactly the yearly purchase") {
            it ==
              effectsOf<UpgradeEffectPayload>(
                Effect.Run(UpgradeEffectPayload.Purchase(Plan.Yearly)))
          }
          whenAction("a second tap while in flight", UpgradeAction.ConfirmTapped)
          thenEffects("nothing: one purchase at a time") { it.isEmpty() }
          whenAction(
            "the port answers",
            UpgradeAction.PurchaseFinished(PurchaseOutcome.Purchased(Plan.Yearly)))
          then("the done step; no entitlement anywhere in this state") {
            it.step == UpgradeStep.Done && !it.isPurchasing
          }
          thenEffects("nothing: the stream carries the entitlement") { it.isEmpty() }
          whenAction("Done", UpgradeAction.DoneTapped)
          thenEffects("exactly the Completed delegate") {
            it ==
              effectsOf<UpgradeEffectPayload>(
                Effect.Run(UpgradeEffectPayload.NotifyHost(UpgradeDelegateEvent.Completed)))
          }
        }

        branch("back walks the steps") {
          whenAction("the monthly card", UpgradeAction.PlanSelected(Plan.Monthly))
          whenAction("Back on the confirm step", UpgradeAction.Back)
          then("the plans again") { it.step == UpgradeStep.Plans }
          thenEffects("nothing: Back inside the flow is a state change") { it.isEmpty() }
          whenAction("Back on the plans", UpgradeAction.Back)
          then("state is untouched") { it.step == UpgradeStep.Plans }
          thenEffects("the Dismissed delegate: the host clears the sheet") {
            it ==
              effectsOf<UpgradeEffectPayload>(
                Effect.Run(UpgradeEffectPayload.NotifyHost(UpgradeDelegateEvent.Dismissed)))
          }
        }

        branch("purchase failure lands") {
          whenAction("the monthly card", UpgradeAction.PlanSelected(Plan.Monthly))
          whenAction("Confirm", UpgradeAction.ConfirmTapped)
          whenAction(
            "the port refuses",
            UpgradeAction.PurchaseFinished(PurchaseOutcome.Failed(PurchaseFailure.Declined)))
          then("the reason shows on the confirm step") {
            it.step == UpgradeStep.Confirm(Plan.Monthly) &&
              !it.isPurchasing &&
              it.failure == PurchaseFailure.Declined
          }
          thenEffects("nothing") { it.isEmpty() }
        }

        branch("back on done completes") {
          whenAction("the monthly card", UpgradeAction.PlanSelected(Plan.Monthly))
          whenAction("Confirm", UpgradeAction.ConfirmTapped)
          whenAction(
            "the port answers",
            UpgradeAction.PurchaseFinished(PurchaseOutcome.Purchased(Plan.Monthly)))
          whenAction("Back on the done step", UpgradeAction.Back)
          thenEffects("the Completed delegate: leaving the last step by any route completes") {
            it ==
              effectsOf<UpgradeEffectPayload>(
                Effect.Run(UpgradeEffectPayload.NotifyHost(UpgradeDelegateEvent.Completed)))
          }
        }

        branch("dismiss from outside") {
          whenAction("the sheet is swiped away", UpgradeAction.DismissTapped)
          thenEffects("the Dismissed delegate") {
            it ==
              effectsOf<UpgradeEffectPayload>(
                Effect.Run(UpgradeEffectPayload.NotifyHost(UpgradeDelegateEvent.Dismissed)))
          }
        }
      }

    ScenarioRunner.verifyOrRecord(
      s,
      UpgradeState.serializer(),
      UpgradeActionSerializer,
      UpgradeEffectPayloadSerializer,
      ::upgradeReducer)
  }
}

private val offers = listOf(PlanOffer(Plan.Monthly, "$4.99"), PlanOffer(Plan.Yearly, "$39.99"))
