// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.home

import dev.modaal.duet.kernel.Effect
import dev.modaal.duet.test.*
import dev.modaal.foyer.ports.Entitlement
import dev.modaal.foyer.ports.Item
import dev.modaal.foyer.ports.Plan
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
            "is Premium; the promo's one tap closes it and asks the host for the " +
            "upgrade flow, and the card unlocks from the entitlement stream alone.",
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
          thenEffects("exactly the event: presenting is the shell's job, counting is this reducer's") {
            it == effectsOf<HomeEffectPayload>(Effect.Run(HomeEffectPayload.Track(HomeEvents.promoViewed)))
          }
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
          thenEffects("nothing: the summary is not the promo") { it.isEmpty() }
        }

        branch("promo requests the upgrade") {
          whenAction("the Insights card, while Free", HomeAction.InsightsTapped)
          whenAction("the promo's button", HomeAction.UpgradeTapped)
          then("the promo closes, still Free") {
            it.presented == null && it.entitlement == Entitlement.Free
          }
          thenEffects("exactly the UpgradeRequested delegate") {
            it ==
              effectsOf<HomeEffectPayload>(
                Effect.Run(HomeEffectPayload.NotifyHost(HomeDelegateEvent.UpgradeRequested)))
          }
        }

        branch("card unlocks from the stream") {
          whenAction("the Insights card, while Free", HomeAction.InsightsTapped)
          whenAction("the promo's button", HomeAction.UpgradeTapped)
          whenAction(
            "the entitlement stream emits after the flow's purchase",
            HomeAction.EntitlementChanged(Entitlement.Premium(Plan.Monthly)))
          then("the card is unlocked; the flow's own answer never reached this reducer") {
            it.entitlement == Entitlement.Premium(Plan.Monthly) && it.presented == null
          }
          thenEffects("nothing") { it.isEmpty() }
        }
      }

    ScenarioRunner.verifyOrRecord(
      s, HomeState.serializer(), HomeActionSerializer, HomeEffectPayloadSerializer, ::homeReducer)
  }
}

private val twoItems = listOf(Item("1", "Welcome note"), Item("2", "Getting started"))
