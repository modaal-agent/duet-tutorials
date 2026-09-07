// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.main

import dev.modaal.duet.kernel.Effect
import dev.modaal.duet.test.*
import dev.modaal.foyer.home.HomeAction
import dev.modaal.foyer.home.HomeActionSerializer
import dev.modaal.foyer.home.HomeEffectPayloadSerializer
import dev.modaal.foyer.home.HomeState
import dev.modaal.foyer.home.homeReducer
import dev.modaal.foyer.ports.Entitlement
import dev.modaal.foyer.ports.Plan
import dev.modaal.foyer.upgrade.UpgradeAction
import dev.modaal.foyer.upgrade.UpgradeActionSerializer
import dev.modaal.foyer.upgrade.UpgradeDelegateEvent
import dev.modaal.foyer.upgrade.UpgradeDelegateEventSerializer
import dev.modaal.foyer.upgrade.UpgradeEffectPayload
import dev.modaal.foyer.upgrade.UpgradeEffectPayloadSerializer
import dev.modaal.foyer.upgrade.UpgradeState
import dev.modaal.foyer.upgrade.UpgradeStep
import dev.modaal.foyer.upgrade.upgradeReducer
import kotlin.test.Test

/**
 * The chain scenario for `chain-upgrade-entitlement`: the flow's Completed
 * crosses into the main level as Upgrade(event), which clears the sheet and
 * climbs nothing; the home tab's card is still locked at that point, and it
 * unlocks on the entitlement stream's value, which reaches it as the root's
 * slice. Tutorial 5's closing exercise is this file: it lives in the main
 * module because that module sees all three nodes.
 */
class MainUpgradeEntitlementChainTest {

  private val upgrade =
    ChainNode(
      "upgrade",
      UpgradeState(step = UpgradeStep.Done),
      UpgradeState.serializer(),
      UpgradeActionSerializer,
      UpgradeEffectPayloadSerializer,
      ::upgradeReducer)

  private val main =
    ChainNode(
      "main",
      MainState(sheet = MainSheet.Upgrade),
      MainState.serializer(),
      MainActionSerializer,
      MainEffectPayloadSerializer,
      ::mainReducer)

  private val home =
    ChainNode(
      "home",
      HomeState(),
      HomeState.serializer(),
      HomeActionSerializer,
      HomeEffectPayloadSerializer,
      ::homeReducer)

  @Test
  fun theCardUnlocksOnTheStreamNotOnTheFlowsDone() {
    val chain =
      chainScenario(
        chain = "upgrade-entitlement",
        fixture = "chain-upgrade-entitlement",
        description =
          "The flow's Completed crosses into the main level as Upgrade(event), which " +
            "clears the sheet and climbs nothing; the home tab stays locked until the " +
            "entitlement stream's value reaches it as the root's slice.",
        source =
          "src-kmp/subtrees/main/logic/src/jvmTest/kotlin/" +
            "dev/modaal/foyer/main/MainUpgradeEntitlementChainTest.kt",
      ) {
        whenAction(upgrade, "Done on the last step", UpgradeAction.DoneTapped)
        thenEffects(upgrade, "exactly the Completed delegate") {
          it ==
            effectsOf<UpgradeEffectPayload>(
              Effect.Run(UpgradeEffectPayload.NotifyHost(UpgradeDelegateEvent.Completed)))
        }
        hop(
          "the flow's delegate is the main level's action",
          from = upgrade,
          to = main,
          delegateSerializer = UpgradeDelegateEventSerializer,
        ) { event ->
          MainAction.Upgrade(event)
        }
        then(main, "the sheet is cleared") { it.sheet == null }
        thenEffects(main, "nothing climbs: the entitlement is not the flow's to report") {
          it.isEmpty()
        }
        // The home node starts locked (its initial state above) and nothing the
        // flow or the main level did reached it.
        whenAction(
          home,
          "the entitlement stream's value arrives as the root's slice",
          HomeAction.EntitlementChanged(Entitlement.Premium(Plan.Monthly)))
        then(home, "the card is unlocked") { it.entitlement == Entitlement.Premium(Plan.Monthly) }
        thenEffects(home, "nothing") { it.isEmpty() }
      }

    ChainScenarioRunner.verifyOrRecord(chain)
  }
}
