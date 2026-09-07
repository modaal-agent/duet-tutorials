// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.main

import dev.modaal.duet.kernel.Effect
import dev.modaal.duet.test.*
import dev.modaal.foyer.profile.ProfileDelegateEvent
import kotlin.test.Test

/** The scenario the main recordings are compiled from. */
class MainScenarioTest {
  @Test
  fun mainScenario() {
    val s =
      scenario<MainState, MainAction, MainEffectPayload>(
        feature = "main",
        description =
          "The main level switches tabs and relays the profile tree's sign-out " +
            "request upward unchanged.",
        source =
          "src-kmp/subtrees/main/logic/src/jvmTest/kotlin/" +
            "dev/modaal/foyer/main/MainScenarioTest.kt",
      ) {
        given(MainState())

        branch("tab switches") {
          whenAction("the Profile tab", MainAction.TabSelected(MainTab.Profile))
          then("profile is active") { it.activeTab == MainTab.Profile }
          thenEffects("nothing") { it.isEmpty() }
          whenAction("the Home tab", MainAction.TabSelected(MainTab.Home))
          then("home is active again") { it.activeTab == MainTab.Home }
        }

        branch("sign out relays") {
          whenAction(
            "the profile tree requests a sign-out",
            MainAction.Profile(ProfileDelegateEvent.SignOutRequested))
          then("state is untouched") { it.activeTab == MainTab.Home }
          thenEffects("the request climbs unchanged") {
            it ==
              effectsOf<MainEffectPayload>(
                Effect.Run(MainEffectPayload.NotifyHost(MainDelegateEvent.SignOutRequested)))
          }
        }
      }

    ScenarioRunner.verifyOrRecord(
      s, MainState.serializer(), MainActionSerializer, MainEffectPayloadSerializer, ::mainReducer)
  }
}
