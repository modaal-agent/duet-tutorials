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
import kotlinx.coroutines.test.runTest

/** The account repository's onboarding write, and what the session stream and the next sign-in carry after it. */
class LocalAccountTest {

  @Test
  fun completingOnboardingReachesTheSessionAndSurvivesARelaunch() = runTest {
    val file = MemoryFile()
    val backend = LocalBackend(file, backgroundScope, LiveClock)
    backend.auth.signIn(SignInProvider.Email("ann@example.com")) {}
    assertEquals(Session.SignedIn("ann", hasOnboarded = false), backend.auth.sessions.value)

    backend.account.completeOnboarding("Ann", listOf("digest", "tips"))
    assertEquals(Session.SignedIn("Ann", hasOnboarded = true), backend.auth.sessions.value)

    // The relaunch: the persisted fact arrives with the stream's first value,
    // and the next sign-in of the same account reports it too.
    val relaunched = LocalBackend(file, backgroundScope, LiveClock)
    assertEquals(Session.SignedIn("Ann", hasOnboarded = true), relaunched.auth.sessions.value)
    relaunched.auth.signOut {}
    var outcome: SignInOutcome? = null
    relaunched.auth.signIn(SignInProvider.Guest) { outcome = it }
    assertEquals(true, assertIs<SignInOutcome.SignedIn>(outcome).hasOnboarded)
  }
}
