// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.app

import dev.modaal.foyer.account.AccountAction
import dev.modaal.foyer.backend.KeyValueFile
import dev.modaal.foyer.backend.LocalAuthConfig
import dev.modaal.foyer.backend.MemoryFile
import dev.modaal.foyer.home.HomeAction
import dev.modaal.foyer.home.HomePresentation
import dev.modaal.foyer.ports.Entitlement
import dev.modaal.foyer.ports.Plan
import dev.modaal.foyer.ports.SignInProvider
import dev.modaal.foyer.profile.ProfileAction
import dev.modaal.foyer.root.AuthSnapshot
import dev.modaal.foyer.root.RootPhase
import dev.modaal.foyer.signin.SignInAction
import dev.modaal.foyer.splash.SplashAction
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
 * the two workers: the auth seed and every session change arrive through
 * the session worker, the entitlement through the entitlement worker.
 */
class RootFlowTest {

  private class TestRootDependency(override val storage: KeyValueFile = MemoryFile()) : RootDependency

  @Test
  fun theTreeMountsFromStateAndTheSignOutClimbsToTheGate() = runTest {
    val root = RootBuilder(TestRootDependency()).buildRoot(backgroundScope)
    runCurrent()
    // The session worker's first report: the sticky stream's current value.
    assertEquals(AuthSnapshot.SignedOut, root.store.state.value.auth)
    val splash = assertIs<RootChildMount.Splash>(root.child.value)

    // The splash completes; the root raises the gate and the splash is gone.
    splash.store.send(SplashAction.Appeared)
    splash.store.send(SplashAction.CeremonyFinished)
    runCurrent()
    assertEquals(RootPhase.SignIn, root.store.state.value.phase)
    val gate = assertIs<RootChildMount.SignIn>(root.child.value)

    // The gate completes through the backend; main mounts with the derived
    // display name, both tabs built and the slice delivered.
    gate.store.send(SignInAction.ContinueTapped(SignInProvider.Email("ann@example.com")))
    runCurrent()
    assertEquals(AuthSnapshot.SignedIn("ann"), root.store.state.value.auth)
    val main = assertIs<RootChildMount.Main>(root.child.value).mount
    assertEquals("ann", main.profile.store.state.value.displayName)
    assertEquals(Entitlement.Free, main.home.state.value.entitlement)
    assertNull(main.profile.account.value)

    // The profile tab mounts the account screen from its state.
    main.profile.store.send(ProfileAction.AccountTapped)
    runCurrent()
    val account = assertNotNull(main.profile.account.value)
    assertEquals("ann", account.store.state.value.displayName)

    // The sign-out climbs four levels; the gate is up and main is torn down.
    account.store.send(AccountAction.SignOutTapped)
    runCurrent()
    assertEquals(RootPhase.SignIn, root.store.state.value.phase)
    assertEquals(AuthSnapshot.SignedOut, root.store.state.value.auth)
    assertIs<RootChildMount.SignIn>(root.child.value)

    root.teardown()
  }

  @Test
  fun aSavedNameReachesTheProfileHeader() = runTest {
    val root = RootBuilder(TestRootDependency()).buildRoot(backgroundScope)
    runCurrent()
    assertIs<RootChildMount.Splash>(root.child.value).store.send(SplashAction.CeremonyFinished)
    runCurrent()
    assertIs<RootChildMount.SignIn>(root.child.value)
      .store
      .send(SignInAction.ContinueTapped(SignInProvider.Guest))
    runCurrent()
    val main = assertIs<RootChildMount.Main>(root.child.value).mount
    main.profile.store.send(ProfileAction.AccountTapped)
    runCurrent()
    val account = assertNotNull(main.profile.account.value)
    account.store.send(AccountAction.EditNameTapped)
    runCurrent()
    val editor = assertNotNull(account.editName.value)

    editor.send(dev.modaal.foyer.editname.EditNameAction.DraftChanged("Ann B"))
    editor.send(dev.modaal.foyer.editname.EditNameAction.SaveTapped)
    runCurrent()

    assertNull(account.editName.value, "the editor is dismissed")
    assertEquals("Ann B", account.store.state.value.displayName)
    assertEquals("Ann B", main.profile.store.state.value.displayName)
    // The session stream carried the name too, so a remount reads it.
    assertEquals(AuthSnapshot.SignedIn("Ann B"), root.store.state.value.auth)
    root.teardown()
  }

  @Test
  fun aGuestSessionExpiresUnderMainAndRaisesTheGate() = runTest {
    val root = RootBuilder(TestRootDependency()).buildRoot(backgroundScope)
    runCurrent()
    assertIs<RootChildMount.Splash>(root.child.value).store.send(SplashAction.CeremonyFinished)
    runCurrent()
    assertIs<RootChildMount.SignIn>(root.child.value)
      .store
      .send(SignInAction.ContinueTapped(SignInProvider.Guest))
    runCurrent()
    assertIs<RootChildMount.Main>(root.child.value)

    advanceTimeBy(LocalAuthConfig.GUEST_SESSION_MINUTES * 60_000)
    runCurrent()
    assertEquals(RootPhase.SignIn, root.store.state.value.phase)
    assertIs<RootChildMount.SignIn>(root.child.value)
    root.teardown()
  }

  @Test
  fun aPurchaseUnlocksTheCardThroughTheStreamAndSurvivesARelaunch() = runTest {
    val file = MemoryFile()
    val root = RootBuilder(TestRootDependency(file)).buildRoot(backgroundScope)
    runCurrent()
    assertIs<RootChildMount.Splash>(root.child.value).store.send(SplashAction.CeremonyFinished)
    runCurrent()
    assertIs<RootChildMount.SignIn>(root.child.value)
      .store
      .send(SignInAction.ContinueTapped(SignInProvider.Email("ann@example.com")))
    runCurrent()
    val main = assertIs<RootChildMount.Main>(root.child.value).mount

    main.home.send(HomeAction.InsightsTapped)
    assertEquals(HomePresentation.Promo, main.home.state.value.presented)
    main.home.send(HomeAction.PurchaseTapped)
    advanceTimeBy(1_000)
    runCurrent()
    assertEquals(Entitlement.Premium(Plan.Monthly), root.store.state.value.entitlement)
    assertEquals(Entitlement.Premium(Plan.Monthly), main.home.state.value.entitlement)
    assertEquals(Entitlement.Premium(Plan.Monthly), main.profile.store.state.value.entitlement)
    assertNull(main.home.state.value.presented)
    root.teardown()

    // The relaunch: the persisted session skips the gate, the persisted plan
    // arrives with the first report of each stream.
    val relaunched = RootBuilder(TestRootDependency(file)).buildRoot(backgroundScope)
    runCurrent()
    assertEquals(AuthSnapshot.SignedIn("ann"), relaunched.store.state.value.auth)
    assertEquals(Entitlement.Premium(Plan.Monthly), relaunched.store.state.value.entitlement)
    assertIs<RootChildMount.Splash>(relaunched.child.value).store.send(SplashAction.CeremonyFinished)
    runCurrent()
    val mainAgain = assertIs<RootChildMount.Main>(relaunched.child.value).mount
    assertEquals(Entitlement.Premium(Plan.Monthly), mainAgain.home.state.value.entitlement)
    relaunched.teardown()
  }
}
