// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.backend

import dev.modaal.duet.kernel.LiveClock
import dev.modaal.foyer.ports.Session
import dev.modaal.foyer.ports.SignInOutcome
import dev.modaal.foyer.ports.SignInProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

/**
 * The auth repository over a memory file. `LiveClock` sleeps on the test
 * scheduler's virtual time under `runTest`, so the guest expiry is a
 * `advanceTimeBy`, not a wait.
 */
class LocalAuthTest {

  @Test
  fun theStreamIsStickyAndStartsSignedOut() = runTest {
    val backend = LocalBackend(MemoryFile(), backgroundScope, LiveClock)
    assertEquals(Session.SignedOut, backend.auth.sessions.value)
  }

  @Test
  fun anEmailSessionSurvivesARelaunch() = runTest {
    val file = MemoryFile()
    var outcome: SignInOutcome? = null
    LocalBackend(file, backgroundScope, LiveClock)
      .auth
      .signIn(SignInProvider.Email("ann@example.com")) { outcome = it }
    assertEquals(SignInOutcome.SignedIn(displayName = null), outcome)

    // A second backend over the same document is the relaunch.
    val relaunched = LocalBackend(file, backgroundScope, LiveClock)
    assertEquals(Session.SignedIn("ann"), relaunched.auth.sessions.value)
  }

  @Test
  fun aGuestSessionExpiresOnTheClockSeam() = runTest {
    val backend = LocalBackend(MemoryFile(), backgroundScope, LiveClock)
    backend.auth.signIn(SignInProvider.Guest) {}
    assertEquals(Session.SignedIn("Guest"), backend.auth.sessions.value)

    advanceTimeBy(LocalAuthConfig.GUEST_SESSION_MINUTES * 60_000 - 1)
    runCurrent()
    assertEquals(Session.SignedIn("Guest"), backend.auth.sessions.value)

    advanceTimeBy(1)
    runCurrent()
    assertEquals(Session.SignedOut, backend.auth.sessions.value)
  }

  @Test
  fun aSavedNameReachesTheSessionAndTheNextSignIn() = runTest {
    val file = MemoryFile()
    val backend = LocalBackend(file, backgroundScope, LiveClock)
    backend.auth.signIn(SignInProvider.Guest) {}
    backend.account.saveDisplayName("Ann B") {}
    assertEquals(Session.SignedIn("Ann B"), backend.auth.sessions.value)

    backend.auth.signOut {}
    assertEquals(Session.SignedOut, backend.auth.sessions.value)
    var outcome: SignInOutcome? = null
    backend.auth.signIn(SignInProvider.Email("ann@example.com")) { outcome = it }
    assertEquals("Ann B", assertIs<SignInOutcome.SignedIn>(outcome).displayName)
  }

  @Test
  fun anEmptyAddressIsRefusedWithoutASession() = runTest {
    val backend = LocalBackend(MemoryFile(), backgroundScope, LiveClock)
    var outcome: SignInOutcome? = null
    backend.auth.signIn(SignInProvider.Email("  ")) { outcome = it }
    assertIs<SignInOutcome.Failed>(outcome)
    assertEquals(Session.SignedOut, backend.auth.sessions.value)
  }
}
