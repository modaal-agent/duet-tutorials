// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.app.workers

import dev.modaal.duet.kernel.LiveClock
import dev.modaal.duet.shells.Relay
import dev.modaal.duet.test.WorkerTester
import dev.modaal.foyer.backend.LocalBackend
import dev.modaal.foyer.backend.LocalPurchasesConfig
import dev.modaal.foyer.backend.MemoryFile
import dev.modaal.foyer.ports.Entitlement
import dev.modaal.foyer.ports.Plan
import dev.modaal.foyer.root.RootAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

/** The entitlement worker through the harness: the sticky seed, then the purchase's flip. */
class EntitlementWorkerTest {

  @Test
  fun thePurchaseReachesTheRootThroughTheStreamAlone() = runTest {
    val backend = LocalBackend(MemoryFile(), backgroundScope, LiveClock)
    val relay = Relay<RootAction>()
    val reported = mutableListOf<RootAction>()
    relay.sink = reported::add
    val tester = WorkerTester(EntitlementWorker(backend.purchases, relay))

    tester.start(backgroundScope)
    runCurrent()
    assertEquals(listOf<RootAction>(RootAction.EntitlementChanged(Entitlement.Free)), reported)

    backend.purchases.purchase(Plan.Yearly) {}
    runCurrent()
    assertEquals(1, reported.size, "nothing until the purchase completes")
    advanceTimeBy(LocalPurchasesConfig.PURCHASE_MILLIS)
    runCurrent()
    assertEquals(
      RootAction.EntitlementChanged(Entitlement.Premium(Plan.Yearly)), reported.last())

    tester.finish()
  }
}
