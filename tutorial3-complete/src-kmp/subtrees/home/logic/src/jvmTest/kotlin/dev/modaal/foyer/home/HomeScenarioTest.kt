// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.home

import dev.modaal.duet.kernel.Effect
import dev.modaal.duet.test.*
import dev.modaal.foyer.ports.Item
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
            "appearance while loading or once loaded is inert.",
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
      }

    ScenarioRunner.verifyOrRecord(
      s, HomeState.serializer(), HomeActionSerializer, HomeEffectPayloadSerializer, ::homeReducer)
  }
}

private val twoItems = listOf(Item("1", "Welcome note"), Item("2", "Getting started"))
