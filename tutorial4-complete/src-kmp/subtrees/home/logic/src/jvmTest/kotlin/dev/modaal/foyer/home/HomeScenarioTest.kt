// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.home

import dev.modaal.duet.kernel.Effect
import dev.modaal.duet.test.*
import dev.modaal.foyer.ports.Entitlement
import dev.modaal.foyer.ports.Item
import dev.modaal.foyer.ports.Plan
import dev.modaal.foyer.ports.PurchaseOutcome
import kotlin.test.Test

/** The scenario the home recordings are compiled from. */
class HomeScenarioTest {
  @Test
  fun homeScenario() {
    val s =
      scenario<HomeState, HomeAction, HomeEffectPayload>(
        feature = "home",
        description =
          "The home tab loads its list once, on the first appearance; a repeat " +
            "appearance while loading or once loaded is inert. The Insights card " +
            "opens the promo while the entitlement is Free and the summary once it " +
            "is Premium; the promo's one tap buys the monthly plan, and the card " +
            "unlocks from the entitlement stream, not from the purchase's answer.",
        source =
          "src-kmp/subtrees/home/logic/src/jvmTest/kotlin/" +
            "dev/modaal/foyer/home/HomeScenarioTest.kt",
      ) {
        given(HomeState())

        whenAction("the tab appears", HomeAction.Appeared)
        then("loading") { it.isLoading && it.items.isEmpty() }
        thenEffects("exactly the load") {
          it == effectsOf<HomeEffectPayload>(Effect.Run(HomeEffectPayload.LoadItems))
        }

        branch("loads once") {
          whenAction("the tab appears again while loading", HomeAction.Appeared)
          thenEffects("nothing: one load at a time") { it.isEmpty() }
          whenAction("the port answers", HomeAction.ItemsLoaded(twoItems))
          then("the list is there, loading over") { it.items == twoItems && !it.isLoading }
        }

        branch("reappear keeps items") {
          whenAction("the port answers", HomeAction.ItemsLoaded(twoItems))
          whenAction("the tab appears again after a switch", HomeAction.Appeared)
          then("the list is untouched") { it.items == twoItems && !it.isLoading }
          thenEffects("nothing: no reload") { it.isEmpty() }
        }

        branch("locked card presents promo") {
          whenAction("the Insights card, while Free", HomeAction.InsightsTapped)
          then("the promo is presented") { it.presented == HomePresentation.Promo }
          thenEffects("nothing: presenting is the shell's job") { it.isEmpty() }
          whenAction("Back", HomeAction.Dismissed)
          then("the promo is gone") { it.presented == null }
        }

        branch("unlocked card presents insights") {
          whenAction(
            "the root projects a Premium entitlement",
            HomeAction.EntitlementChanged(Entitlement.Premium(Plan.Yearly)))
          then("the slice is written") { it.entitlement == Entitlement.Premium(Plan.Yearly) }
          whenAction("the Insights card, while Premium", HomeAction.InsightsTapped)
          then("the summary is presented") { it.presented == HomePresentation.Insights }
          thenEffects("nothing") { it.isEmpty() }
        }

        branch("purchase flips through the stream") {
          whenAction("the Insights card, while Free", HomeAction.InsightsTapped)
          whenAction("the promo's button", HomeAction.PurchaseTapped)
          then("a purchase is in flight") { it.isPurchasing && it.failure == null }
          thenEffects("exactly the monthly purchase") {
            it == effectsOf<HomeEffectPayload>(Effect.Run(HomeEffectPayload.Purchase(Plan.Monthly)))
          }
          whenAction("a second tap while in flight", HomeAction.PurchaseTapped)
          thenEffects("nothing: one purchase at a time") { it.isEmpty() }
          whenAction(
            "the entitlement stream emits first",
            HomeAction.EntitlementChanged(Entitlement.Premium(Plan.Monthly)))
          then("the card is unlocked, the promo still up") {
            it.entitlement == Entitlement.Premium(Plan.Monthly) &&
              it.presented == HomePresentation.Promo
          }
          whenAction(
            "the port answers",
            HomeAction.PurchaseFinished(PurchaseOutcome.Purchased(Plan.Monthly)))
          then("the promo closes; the entitlement was never written here") {
            !it.isPurchasing && it.presented == null && it.entitlement == Entitlement.Premium(Plan.Monthly)
          }
        }

        branch("purchase failure lands") {
          whenAction("the Insights card, while Free", HomeAction.InsightsTapped)
          whenAction("the promo's button", HomeAction.PurchaseTapped)
          whenAction(
            "the port refuses",
            HomeAction.PurchaseFinished(PurchaseOutcome.Failed("Payment declined.")))
          then("the failure shows on the promo, still Free") {
            !it.isPurchasing &&
              it.failure == "Payment declined." &&
              it.presented == HomePresentation.Promo &&
              it.entitlement == Entitlement.Free
          }
        }
      }

    ScenarioRunner.verifyOrRecord(
      s, HomeState.serializer(), HomeActionSerializer, HomeEffectPayloadSerializer, ::homeReducer)
  }
}

private val twoItems = listOf(Item("1", "Welcome note"), Item("2", "Getting started"))
