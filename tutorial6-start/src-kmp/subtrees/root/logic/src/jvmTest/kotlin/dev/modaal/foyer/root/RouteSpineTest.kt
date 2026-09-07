// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.root

import dev.modaal.foyer.home.HomePresentation
import dev.modaal.foyer.main.MainTab
import dev.modaal.foyer.ports.OnboardingPage
import dev.modaal.foyer.ports.Plan
import dev.modaal.foyer.upgrade.UpgradeStep
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The spine's carrier form, pinned: the bytes the two apps save are these,
 * and a payload this version does not read restores nothing.
 */
class RouteSpineTest {

  private val deep =
    RouteSpine(
      phase = RootPhase.Main,
      activeTab = MainTab.Profile,
      profilePath = ProfilePath.EditName,
      homePresented = HomePresentation.Insights,
      upgradeStep = UpgradeStep.Confirm(Plan.Yearly),
      onboardingPage = null,
    )

  @Test
  fun theEncodingIsTheGolden() {
    assertEquals(
      """{"phase":{"case":"main"},"activeTab":{"case":"profile"},""" +
        """"profilePath":{"case":"editName"},"homePresented":{"case":"insights"},""" +
        """"upgradeStep":{"case":"confirm","value":{"plan":{"case":"yearly"}}}}""",
      encodeRouteSpine(deep))
  }

  @Test
  fun theEncodingRoundTrips() {
    assertEquals(deep, decodeRouteSpine(encodeRouteSpine(deep)))
    val gate = RouteSpine(phase = RootPhase.Onboarding, onboardingPage = OnboardingPage.Name)
    assertEquals(gate, decodeRouteSpine(encodeRouteSpine(gate)))
  }

  @Test
  fun aStalePayloadRestoresNothing() {
    assertNull(decodeRouteSpine(null))
    assertNull(decodeRouteSpine(""))
    assertNull(decodeRouteSpine("not json"))
    assertNull(decodeRouteSpine("""{"phase":{"case":"lobby"}}"""))
  }
}
