// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.app

import dev.modaal.foyer.home.HomePresentation
import dev.modaal.foyer.main.MainSheet
import dev.modaal.foyer.main.MainTab
import dev.modaal.foyer.ports.OnboardingPage
import dev.modaal.foyer.root.ProfilePath
import dev.modaal.foyer.root.RootPhase
import dev.modaal.foyer.root.RouteSpine

/**
 * The Android back-dispatch table, as pure predicates. Every `BackHandler`
 * in the Compose screens is enabled by exactly one function here, and
 * Compose dispatches a back press to the last composed enabled handler, so
 * [COMPOSITION_ORDER], shallow to deep with the sheet last, is the
 * precedence: the deepest enabled handler wins. The sheet's handler is the
 * modal bottom sheet's own, which reports through its dismiss request. The
 * splash and the sign-in gate install no handler, and the onboarding gate's
 * first page installs none either: back on a gate leaves the app.
 * `BackPolicyTest` replays [winner] per spine.
 */
object BackPolicy {

  enum class Level {
    ONBOARDING,
    HOME_PRESENTED,
    ACCOUNT,
    EDIT_NAME,
    UPGRADE,
  }

  /** The order the screens compose their handlers in; the winner is the last enabled level. */
  val COMPOSITION_ORDER: List<Level> =
    listOf(Level.ONBOARDING, Level.HOME_PRESENTED, Level.ACCOUNT, Level.EDIT_NAME, Level.UPGRADE)

  fun onboardingEnabled(page: OnboardingPage): Boolean = page != OnboardingPage.Welcome

  fun homePresentedEnabled(presented: HomePresentation?): Boolean = presented != null

  fun accountEnabled(path: ProfilePath?): Boolean = path != null

  fun editNameEnabled(path: ProfilePath?): Boolean = path == ProfilePath.EditName

  fun upgradeEnabled(sheet: MainSheet?): Boolean = sheet != null

  /** The handler a back press reaches for this spine, or null when the system handles it. */
  fun winner(spine: RouteSpine): Level? =
    when (spine.phase) {
      RootPhase.Splash,
      RootPhase.SignIn -> null
      RootPhase.Onboarding ->
        Level.ONBOARDING.takeIf { onboardingEnabled(spine.onboardingPage ?: OnboardingPage.Welcome) }
      RootPhase.Main ->
        COMPOSITION_ORDER.lastOrNull { level ->
          when (level) {
            Level.ONBOARDING -> false
            Level.HOME_PRESENTED ->
              spine.activeTab == MainTab.Home && homePresentedEnabled(spine.homePresented)
            Level.ACCOUNT -> spine.activeTab == MainTab.Profile && accountEnabled(spine.profilePath)
            Level.EDIT_NAME -> spine.activeTab == MainTab.Profile && editNameEnabled(spine.profilePath)
            Level.UPGRADE -> upgradeEnabled(spine.upgradeStep?.let { MainSheet.Upgrade })
          }
        }
    }
}
