// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.app

import dev.modaal.foyer.account.AccountAction
import dev.modaal.foyer.backend.KeyValueFile
import dev.modaal.foyer.backend.LocalAuthConfig
import dev.modaal.foyer.backend.MemoryFile
import dev.modaal.foyer.editname.EditNameAction
import dev.modaal.foyer.home.HomeAction
import dev.modaal.foyer.home.HomePresentation
import dev.modaal.foyer.main.MainSheet
import dev.modaal.foyer.main.MainTab
import dev.modaal.foyer.name.NameAction
import dev.modaal.foyer.ports.DeepLink
import dev.modaal.foyer.ports.Entitlement
import dev.modaal.foyer.ports.OnboardingPage
import dev.modaal.foyer.ports.Plan
import dev.modaal.foyer.ports.SignInProvider
import dev.modaal.foyer.preferences.PreferencesAction
import dev.modaal.foyer.profile.ProfileAction
import dev.modaal.foyer.profile.ProfileRoute
import dev.modaal.foyer.root.AuthSnapshot
import dev.modaal.foyer.root.ProfilePath
import dev.modaal.foyer.root.RootAction
import dev.modaal.foyer.root.RootPhase
import dev.modaal.foyer.root.RouteSpine
import dev.modaal.foyer.root.decodeRouteSpine
import dev.modaal.foyer.root.encodeRouteSpine
import dev.modaal.foyer.signin.SignInAction
import dev.modaal.foyer.splash.SplashAction
import dev.modaal.foyer.upgrade.UpgradeAction
import dev.modaal.foyer.upgrade.UpgradeStep
import dev.modaal.foyer.welcome.WelcomeAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

/**
 * The whole tree, headless: the real root pair over the on-device backend
 * in a memory file, no Compose and no Android in the loop. The walks cross
 * the two workers, the onboarding gate with its lateral seam, the sheet
 * slot, the deep links and the route spine.
 */
class RootFlowTest {

  private class TestRootDependency(override val storage: KeyValueFile = MemoryFile()) : RootDependency

  /** Splash, then the sign-in gate with the given provider. */
  private fun TestScope.signIn(root: RootMount, provider: SignInProvider) {
    runCurrent()
    assertIs<RootChildMount.Splash>(root.child.value).store.send(SplashAction.CeremonyFinished)
    runCurrent()
    assertIs<RootChildMount.SignIn>(root.child.value).store.send(SignInAction.ContinueTapped(provider))
    runCurrent()
  }

  /** The three onboarding steps, as a new account walks them. */
  private fun TestScope.onboard(root: RootMount, name: String) {
    val onboarding = assertIs<RootChildMount.Onboarding>(root.child.value).mount
    assertIs<OnboardingStepMount.Welcome>(onboarding.step.value).store.send(WelcomeAction.ContinueTapped)
    runCurrent()
    val nameStep = assertIs<OnboardingStepMount.Name>(onboarding.step.value).store
    nameStep.send(NameAction.DraftChanged(name))
    nameStep.send(NameAction.ContinueTapped)
    runCurrent()
    val preferences = assertIs<OnboardingStepMount.Preferences>(onboarding.step.value).store
    preferences.send(PreferencesAction.PreferenceToggled("digest"))
    preferences.send(PreferencesAction.ContinueTapped)
    runCurrent()
  }

  @Test
  fun theTreeMountsFromStateAndTheSignOutClimbsToTheGate() = runTest {
    val root = RootBuilder(TestRootDependency()).buildRoot(backgroundScope)
    runCurrent()
    // The session worker's first report: the sticky stream's current value.
    assertEquals(AuthSnapshot.SignedOut, root.store.state.value.auth)
    signIn(root, SignInProvider.Email("ann@example.com"))
    // A new account: the onboarding gate, then main with the derived name.
    assertEquals(AuthSnapshot.SignedIn("ann", hasOnboarded = false), root.store.state.value.auth)
    assertEquals(RootPhase.Onboarding, root.store.state.value.phase)
    onboard(root, "Ann")
    assertEquals(RootPhase.Main, root.store.state.value.phase)
    assertEquals(AuthSnapshot.SignedIn("Ann", hasOnboarded = true), root.store.state.value.auth)
    val main = assertIs<RootChildMount.Main>(root.child.value).mount
    assertEquals("Ann", main.profile.store.state.value.displayName)
    assertEquals(Entitlement.Free, main.home.state.value.entitlement)
    assertNull(main.profile.account.value)

    // The profile tab mounts the account screen from its state.
    main.profile.store.send(ProfileAction.AccountTapped)
    runCurrent()
    val account = assertNotNull(main.profile.account.value)
    assertEquals("Ann", account.store.state.value.displayName)

    // The sign-out climbs four levels; the gate is up and main is torn down.
    account.store.send(AccountAction.SignOutTapped)
    runCurrent()
    assertEquals(RootPhase.SignIn, root.store.state.value.phase)
    assertEquals(AuthSnapshot.SignedOut, root.store.state.value.auth)
    assertIs<RootChildMount.SignIn>(root.child.value)
    root.teardown()
  }

  @Test
  fun theOnboardingGateFeedsTheProgressRowFromBothRoutes() = runTest {
    val root = RootBuilder(TestRootDependency()).buildRoot(backgroundScope)
    signIn(root, SignInProvider.Guest)
    val onboarding = assertIs<RootChildMount.Onboarding>(root.child.value).mount
    val progress = onboarding.progress
    progress.send(dev.modaal.foyer.progress.ProgressAction.Appeared)
    runCurrent()
    // The page, projected down: step 1 of 3.
    assertEquals(OnboardingPage.Welcome, progress.state.value.page)
    // The readiness, laterally: the welcome step publishes when it appears.
    val welcome = assertIs<OnboardingStepMount.Welcome>(onboarding.step.value).store
    welcome.send(WelcomeAction.Appeared)
    runCurrent()
    assertEquals(mapOf(OnboardingPage.Welcome to true), progress.state.value.readiness)

    welcome.send(WelcomeAction.ContinueTapped)
    runCurrent()
    assertEquals(OnboardingPage.Name, progress.state.value.page)
    val nameStep = assertIs<OnboardingStepMount.Name>(onboarding.step.value).store
    nameStep.send(NameAction.DraftChanged("A"))
    runCurrent()
    assertEquals(true, progress.state.value.readiness[OnboardingPage.Name])
    nameStep.send(NameAction.DraftChanged(""))
    runCurrent()
    assertEquals(false, progress.state.value.readiness[OnboardingPage.Name])

    // Back is the level's action; the page moves and the row follows.
    onboarding.store.send(dev.modaal.foyer.onboarding.OnboardingAction.Back)
    runCurrent()
    assertEquals(OnboardingPage.Welcome, progress.state.value.page)
    assertIs<OnboardingStepMount.Welcome>(onboarding.step.value)
    root.teardown()
  }

  @Test
  fun aSavedNameReachesTheProfileHeader() = runTest {
    val root = RootBuilder(TestRootDependency()).buildRoot(backgroundScope)
    signIn(root, SignInProvider.Guest)
    onboard(root, "Guest")
    val main = assertIs<RootChildMount.Main>(root.child.value).mount
    main.profile.store.send(ProfileAction.AccountTapped)
    runCurrent()
    val account = assertNotNull(main.profile.account.value)
    account.store.send(AccountAction.EditNameTapped)
    runCurrent()
    val editor = assertNotNull(account.editName.value)

    editor.send(EditNameAction.DraftChanged("Ann B"))
    editor.send(EditNameAction.SaveTapped)
    runCurrent()

    assertNull(account.editName.value, "the editor is dismissed")
    assertEquals("Ann B", account.store.state.value.displayName)
    assertEquals("Ann B", main.profile.store.state.value.displayName)
    // The session stream carried the name too, so a remount reads it.
    assertEquals(AuthSnapshot.SignedIn("Ann B", hasOnboarded = true), root.store.state.value.auth)
    root.teardown()
  }

  @Test
  fun aGuestSessionExpiresUnderMainAndRaisesTheGate() = runTest {
    val root = RootBuilder(TestRootDependency()).buildRoot(backgroundScope)
    signIn(root, SignInProvider.Guest)
    onboard(root, "Guest")
    assertIs<RootChildMount.Main>(root.child.value)

    advanceTimeBy(LocalAuthConfig.GUEST_SESSION_MINUTES * 60_000)
    runCurrent()
    assertEquals(RootPhase.SignIn, root.store.state.value.phase)
    assertIs<RootChildMount.SignIn>(root.child.value)
    root.teardown()
  }

  @Test
  fun theUpgradeFlowUnlocksTheCardThroughTheStreamAndSurvivesARelaunch() = runTest {
    val file = MemoryFile()
    val root = RootBuilder(TestRootDependency(file)).buildRoot(backgroundScope)
    signIn(root, SignInProvider.Email("ann@example.com"))
    onboard(root, "Ann")
    val main = assertIs<RootChildMount.Main>(root.child.value).mount

    // The promo hands off: the main level mounts the flow in its sheet slot.
    main.home.send(HomeAction.InsightsTapped)
    assertEquals(HomePresentation.Promo, main.home.state.value.presented)
    main.home.send(HomeAction.UpgradeTapped)
    runCurrent()
    assertNull(main.home.state.value.presented)
    assertEquals(MainSheet.Upgrade, main.store.state.value.sheet)
    val upgrade = assertNotNull(main.upgrade.value)
    upgrade.send(UpgradeAction.Appeared)
    runCurrent()
    assertEquals(2, upgrade.state.value.offers.size)
    upgrade.send(UpgradeAction.PlanSelected(Plan.Yearly))
    upgrade.send(UpgradeAction.ConfirmTapped)
    advanceTimeBy(1_000)
    runCurrent()
    // The purchase's answer moved the step; the stream unlocked the card.
    assertEquals(UpgradeStep.Done, upgrade.state.value.step)
    assertEquals(Entitlement.Premium(Plan.Yearly), root.store.state.value.entitlement)
    assertEquals(Entitlement.Premium(Plan.Yearly), main.home.state.value.entitlement)
    assertEquals(Entitlement.Premium(Plan.Yearly), main.profile.store.state.value.entitlement)
    upgrade.send(UpgradeAction.DoneTapped)
    runCurrent()
    assertNull(main.store.state.value.sheet)
    assertNull(main.upgrade.value, "the flow is torn down with the sheet")
    root.teardown()

    // The relaunch: the persisted, onboarded session skips both gates, the
    // persisted plan arrives with the first report of each stream.
    val relaunched = RootBuilder(TestRootDependency(file)).buildRoot(backgroundScope)
    runCurrent()
    assertEquals(AuthSnapshot.SignedIn("Ann", hasOnboarded = true), relaunched.store.state.value.auth)
    assertEquals(Entitlement.Premium(Plan.Yearly), relaunched.store.state.value.entitlement)
    assertIs<RootChildMount.Splash>(relaunched.child.value).store.send(SplashAction.CeremonyFinished)
    runCurrent()
    val mainAgain = assertIs<RootChildMount.Main>(relaunched.child.value).mount
    assertEquals(Entitlement.Premium(Plan.Yearly), mainAgain.home.state.value.entitlement)
    relaunched.teardown()
  }

  @Test
  fun aDeepLinkDuringTheSplashOpensTheFlowWhenMainMounts() = runTest {
    val file = MemoryFile()
    val first = RootBuilder(TestRootDependency(file)).buildRoot(backgroundScope)
    signIn(first, SignInProvider.Guest)
    onboard(first, "Guest")
    first.teardown()

    val root = RootBuilder(TestRootDependency(file)).buildRoot(backgroundScope)
    runCurrent()
    root.store.send(RootAction.DeepLink(DeepLink.Upgrade))
    runCurrent()
    assertEquals(DeepLink.Upgrade, root.store.state.value.pendingLink, "held during the splash")
    assertIs<RootChildMount.Splash>(root.child.value).store.send(SplashAction.CeremonyFinished)
    runCurrent()
    val main = assertIs<RootChildMount.Main>(root.child.value).mount
    assertNull(root.store.state.value.pendingLink)
    assertEquals(MainSheet.Upgrade, main.store.state.value.sheet)
    assertNotNull(main.upgrade.value)
    root.teardown()
  }

  @Test
  fun aDeepLinkUnderMainReachesTheProfileTree() = runTest {
    val root = RootBuilder(TestRootDependency()).buildRoot(backgroundScope)
    signIn(root, SignInProvider.Guest)
    onboard(root, "Guest")
    val main = assertIs<RootChildMount.Main>(root.child.value).mount
    assertEquals(MainTab.Home, main.store.state.value.activeTab)

    root.store.send(RootAction.DeepLink(DeepLink.ProfileAccount))
    runCurrent()
    assertEquals(MainTab.Profile, main.store.state.value.activeTab)
    assertEquals(ProfileRoute.Account, main.profile.store.state.value.child)
    assertNotNull(main.profile.account.value)
    root.teardown()
  }

  @Test
  fun theTreeRestoresFromASavedSpineAndIgnoresAStaleOne() = runTest {
    val file = MemoryFile()
    val first = RootBuilder(TestRootDependency(file)).buildRoot(backgroundScope)
    signIn(first, SignInProvider.Email("ann@example.com"))
    onboard(first, "Ann")
    val main = assertIs<RootChildMount.Main>(first.child.value).mount
    main.store.send(dev.modaal.foyer.main.MainAction.TabSelected(MainTab.Profile))
    main.profile.store.send(ProfileAction.AccountTapped)
    runCurrent()
    assertNotNull(main.profile.account.value).store.send(AccountAction.EditNameTapped)
    runCurrent()
    // What the Activity saves on process death.
    val saved = encodeRouteSpine(first.routeSpine())
    assertEquals(
      RouteSpine(phase = RootPhase.Main, activeTab = MainTab.Profile, profilePath = ProfilePath.EditName),
      decodeRouteSpine(saved))
    first.teardown()

    // The rebuild: the splash replays, then main mounts with every sliver.
    val restored = RootBuilder(TestRootDependency(file)).buildRoot(backgroundScope, decodeRouteSpine(saved))
    runCurrent()
    assertIs<RootChildMount.Splash>(restored.child.value).store.send(SplashAction.CeremonyFinished)
    runCurrent()
    val mainAgain = assertIs<RootChildMount.Main>(restored.child.value).mount
    assertEquals(MainTab.Profile, mainAgain.store.state.value.activeTab)
    val account = assertNotNull(mainAgain.profile.account.value, "the account screen is mounted")
    assertEquals("Ann", account.store.state.value.displayName)
    assertNotNull(account.editName.value, "the editor is mounted over it")
    assertEquals(ProfilePath.EditName, mainAgain.profile.routePath())
    restored.teardown()

    // A stale payload restores nothing: the tree starts as it always does.
    val fresh = RootBuilder(TestRootDependency(file)).buildRoot(backgroundScope, decodeRouteSpine("{\"phase\":1}"))
    runCurrent()
    assertIs<RootChildMount.Splash>(fresh.child.value).store.send(SplashAction.CeremonyFinished)
    runCurrent()
    val plain = assertIs<RootChildMount.Main>(fresh.child.value).mount
    assertEquals(MainTab.Home, plain.store.state.value.activeTab)
    assertNull(plain.profile.account.value)
    fresh.teardown()
  }
}
