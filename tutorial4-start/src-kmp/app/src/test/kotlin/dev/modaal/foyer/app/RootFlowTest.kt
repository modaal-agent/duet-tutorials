// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.app

import dev.modaal.foyer.account.AccountAction
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
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

/**
 * The whole tree, headless: the real root pair over the mock services, no
 * Compose and no Android in the loop. One walk from the splash through the
 * gate into main, down the profile tree to the account screen, and the
 * sign-out climbing back to the gate.
 */
class RootFlowTest {

  private object TestRootDependency : RootDependency

  @Test
  fun theTreeMountsFromStateAndTheSignOutClimbsToTheGate() = runTest {
    val root = RootBuilder(TestRootDependency).buildRoot(backgroundScope)
    runCurrent()
    assertEquals(AuthSnapshot.SignedOut, root.store.state.value.auth)
    val splash = assertIs<RootChildMount.Splash>(root.child.value)

    // The splash completes; the root raises the gate and the splash is gone.
    splash.store.send(SplashAction.Appeared)
    splash.store.send(SplashAction.CeremonyFinished)
    runCurrent()
    assertEquals(RootPhase.SignIn, root.store.state.value.phase)
    val gate = assertIs<RootChildMount.SignIn>(root.child.value)

    // The gate completes through the mock auth service; main mounts with the
    // derived display name, both tabs built.
    gate.store.send(SignInAction.ContinueTapped(SignInProvider.Email("ann@example.com")))
    runCurrent()
    assertEquals(AuthSnapshot.SignedIn("ann"), root.store.state.value.auth)
    val main = assertIs<RootChildMount.Main>(root.child.value).mount
    assertEquals("ann", main.profile.store.state.value.displayName)
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
    val root = RootBuilder(TestRootDependency).buildRoot(backgroundScope)
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
    root.teardown()
  }
}
