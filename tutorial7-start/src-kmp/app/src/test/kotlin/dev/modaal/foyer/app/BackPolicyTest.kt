// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.app

import dev.modaal.foyer.home.HomePresentation
import dev.modaal.foyer.main.MainTab
import dev.modaal.foyer.ports.OnboardingPage
import dev.modaal.foyer.ports.Plan
import dev.modaal.foyer.root.ProfilePath
import dev.modaal.foyer.root.RootPhase
import dev.modaal.foyer.root.RouteSpine
import dev.modaal.foyer.upgrade.UpgradeStep
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** The back-dispatch table replayed per spine: the winner is the deepest enabled handler. */
class BackPolicyTest {

  @Test
  fun theGatesAreBackInert() {
    assertNull(BackPolicy.winner(RouteSpine(phase = RootPhase.Splash)))
    assertNull(BackPolicy.winner(RouteSpine(phase = RootPhase.SignIn)))
    assertNull(
      BackPolicy.winner(RouteSpine(phase = RootPhase.Onboarding, onboardingPage = OnboardingPage.Welcome)))
  }

  @Test
  fun aLaterOnboardingPageHandlesBack() {
    assertEquals(
      BackPolicy.Level.ONBOARDING,
      BackPolicy.winner(RouteSpine(phase = RootPhase.Onboarding, onboardingPage = OnboardingPage.Name)))
  }

  @Test
  fun mainWithNothingOpenLeavesTheApp() {
    assertNull(BackPolicy.winner(RouteSpine(phase = RootPhase.Main, activeTab = MainTab.Home)))
    assertNull(BackPolicy.winner(RouteSpine(phase = RootPhase.Main, activeTab = MainTab.Profile)))
  }

  @Test
  fun theDeepestEnabledHandlerWins() {
    assertEquals(
      BackPolicy.Level.HOME_PRESENTED,
      BackPolicy.winner(
        RouteSpine(phase = RootPhase.Main, activeTab = MainTab.Home, homePresented = HomePresentation.Insights)))
    assertEquals(
      BackPolicy.Level.ACCOUNT,
      BackPolicy.winner(
        RouteSpine(phase = RootPhase.Main, activeTab = MainTab.Profile, profilePath = ProfilePath.Account)))
    assertEquals(
      BackPolicy.Level.EDIT_NAME,
      BackPolicy.winner(
        RouteSpine(phase = RootPhase.Main, activeTab = MainTab.Profile, profilePath = ProfilePath.EditName)))
  }

  @Test
  fun aHiddenTabsHandlersAreDead() {
    // The profile tree is mounted but the home tab is shown: its handlers do not fire.
    assertNull(
      BackPolicy.winner(
        RouteSpine(phase = RootPhase.Main, activeTab = MainTab.Home, profilePath = ProfilePath.EditName)))
  }

  @Test
  fun theSheetWinsOverEverything() {
    assertEquals(
      BackPolicy.Level.UPGRADE,
      BackPolicy.winner(
        RouteSpine(
          phase = RootPhase.Main,
          activeTab = MainTab.Profile,
          profilePath = ProfilePath.EditName,
          upgradeStep = UpgradeStep.Confirm(Plan.Monthly))))
  }
}
