// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.app.workers

import dev.modaal.duet.kernel.LiveClock
import dev.modaal.duet.shells.Relay
import dev.modaal.duet.test.WorkerTester
import dev.modaal.foyer.backend.LocalAuthConfig
import dev.modaal.foyer.backend.LocalBackend
import dev.modaal.foyer.backend.MemoryFile
import dev.modaal.foyer.ports.SignInProvider
import dev.modaal.foyer.root.AuthSnapshot
import dev.modaal.foyer.root.RootAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

/**
 * The session worker through the harness: start, drive the backend, read
 * what reached the relay, finish. The worker carries no recordings; the
 * backend runs on the test scheduler's virtual time.
 */
class SessionWorkerTest {

  @Test
  fun theStickyStreamSeedsTheRootAndEveryChangeFollows() = runTest {
    val backend = LocalBackend(MemoryFile(), backgroundScope, LiveClock)
    val relay = Relay<RootAction>()
    val reported = mutableListOf<RootAction>()
    relay.sink = reported::add
    val tester = WorkerTester(SessionWorker(backend.auth, relay))

    tester.start(backgroundScope)
    runCurrent()
    assertEquals(listOf<RootAction>(RootAction.AuthChanged(AuthSnapshot.SignedOut)), reported)

    backend.auth.signIn(SignInProvider.Email("ann@example.com")) {}
    runCurrent()
    assertEquals(RootAction.AuthChanged(AuthSnapshot.SignedIn("ann")), reported.last())

    backend.account.saveDisplayName("Ann B") {}
    runCurrent()
    assertEquals(RootAction.AuthChanged(AuthSnapshot.SignedIn("Ann B")), reported.last())

    tester.finish()
    assertTrue(tester.isFinished)
  }

  @Test
  fun aGuestExpiryReachesTheRootAsASignedOutReport() = runTest {
    val backend = LocalBackend(MemoryFile(), backgroundScope, LiveClock)
    val relay = Relay<RootAction>()
    val reported = mutableListOf<RootAction>()
    relay.sink = reported::add
    val tester = WorkerTester(SessionWorker(backend.auth, relay))
    tester.start(backgroundScope)
    backend.auth.signIn(SignInProvider.Guest) {}
    runCurrent()
    assertEquals(RootAction.AuthChanged(AuthSnapshot.SignedIn("Guest")), reported.last())

    advanceTimeBy(LocalAuthConfig.GUEST_SESSION_MINUTES * 60_000)
    runCurrent()
    assertEquals(RootAction.AuthChanged(AuthSnapshot.SignedOut), reported.last())
    tester.finish()
  }

  @Test
  fun teardownStopsTheReports() = runTest {
    val backend = LocalBackend(MemoryFile(), backgroundScope, LiveClock)
    val relay = Relay<RootAction>()
    val reported = mutableListOf<RootAction>()
    relay.sink = reported::add
    val tester = WorkerTester(SessionWorker(backend.auth, relay))
    tester.start(backgroundScope)
    runCurrent()
    tester.finish()

    backend.auth.signIn(SignInProvider.Guest) {}
    runCurrent()
    assertEquals(1, reported.size, "nothing after the bracket closes")
  }
}
