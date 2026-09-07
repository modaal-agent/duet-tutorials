// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.main

import dev.modaal.duet.kernel.Effect
import dev.modaal.duet.test.*
import dev.modaal.foyer.home.HomeDelegateEvent
import dev.modaal.foyer.ports.DeepLink
import dev.modaal.foyer.profile.ProfileDelegateEvent
import dev.modaal.foyer.upgrade.UpgradeDelegateEvent
import kotlin.test.Test

/** The scenario the main recordings are compiled from. */
class MainScenarioTest {
  @Test
  fun mainScenario() {
    val s =
      scenario<MainState, MainAction, MainEffectPayload>(
        feature = "main",
        description =
          "The main level switches tabs, mounts the upgrade flow in its sheet slot " +
            "on either tab's request and clears it on the flow's Completed or " +
            "Dismissed, relays the profile tree's sign-out request upward unchanged, " +
            "and routes a forwarded deep link to the sheet or on down to the profile tab.",
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
          then("state is untouched") { it.activeTab == MainTab.Home && it.sheet == null }
          thenEffects("the request climbs unchanged") {
            it ==
              effectsOf<MainEffectPayload>(
                Effect.Run(MainEffectPayload.NotifyHost(MainDelegateEvent.SignOutRequested)))
          }
        }

        branch("home opens the upgrade flow") {
          whenAction(
            "the home tab's promo asks for the flow",
            MainAction.Home(HomeDelegateEvent.UpgradeRequested))
          then("the sheet mounts the flow over the home tab") {
            it.sheet == MainSheet.Upgrade && it.activeTab == MainTab.Home
          }
          thenEffects("nothing: mounting is the shell's job") { it.isEmpty() }
          whenAction("the flow completes", MainAction.Upgrade(UpgradeDelegateEvent.Completed))
          then("the sheet is cleared; nothing else moved") {
            it.sheet == null && it.activeTab == MainTab.Home
          }
          thenEffects("nothing: the entitlement is not this level's to write") { it.isEmpty() }
        }

        branch("profile opens the upgrade flow") {
          whenAction("the Profile tab", MainAction.TabSelected(MainTab.Profile))
          whenAction(
            "the profile tab's plan row asks for the flow",
            MainAction.Profile(ProfileDelegateEvent.UpgradeRequested))
          then("the same slot, over the profile tab") {
            it.sheet == MainSheet.Upgrade && it.activeTab == MainTab.Profile
          }
          whenAction("the flow is dismissed", MainAction.Upgrade(UpgradeDelegateEvent.Dismissed))
          then("the sheet is cleared") { it.sheet == null }
        }

        branch("link opens the upgrade flow") {
          whenAction("the root forwards the upgrade link", MainAction.OpenLink(DeepLink.Upgrade))
          then("the sheet mounts the flow, the tab stays") {
            it.sheet == MainSheet.Upgrade && it.activeTab == MainTab.Home
          }
          thenEffects("nothing") { it.isEmpty() }
        }

        branch("link travels to the profile tab") {
          whenAction(
            "the root forwards the account link",
            MainAction.OpenLink(DeepLink.ProfileAccount))
          then("the profile tab is selected") { it.activeTab == MainTab.Profile && it.sheet == null }
          thenEffects("the link travels on down") {
            it ==
              effectsOf<MainEffectPayload>(
                Effect.Run(MainEffectPayload.ForwardLink(DeepLink.ProfileAccount)))
          }
        }
      }

    ScenarioRunner.verifyOrRecord(
      s, MainState.serializer(), MainActionSerializer, MainEffectPayloadSerializer, ::mainReducer)
  }
}
