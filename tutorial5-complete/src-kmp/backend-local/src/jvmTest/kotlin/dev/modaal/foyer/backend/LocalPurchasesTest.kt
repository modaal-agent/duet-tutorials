// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.backend

import dev.modaal.duet.kernel.LiveClock
import dev.modaal.foyer.ports.Entitlement
import dev.modaal.foyer.ports.Plan
import dev.modaal.foyer.ports.PurchaseOutcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

/** The purchases repository: the delay on the clock seam, the stream, the persisted plan. */
class LocalPurchasesTest {

  @Test
  fun aPurchaseFlipsTheStreamAfterItsDelayAndSurvivesARelaunch() = runTest {
    val file = MemoryFile()
    val backend = LocalBackend(file, backgroundScope, LiveClock)
    assertEquals(Entitlement.Free, backend.purchases.entitlements.value)

    var outcome: PurchaseOutcome? = null
    backend.purchases.purchase(Plan.Monthly) { outcome = it }
    advanceTimeBy(LocalPurchasesConfig.PURCHASE_MILLIS - 1)
    runCurrent()
    assertNull(outcome)
    assertEquals(Entitlement.Free, backend.purchases.entitlements.value)

    advanceTimeBy(1)
    runCurrent()
    assertEquals(PurchaseOutcome.Purchased(Plan.Monthly), outcome)
    assertEquals(Entitlement.Premium(Plan.Monthly), backend.purchases.entitlements.value)

    val relaunched = LocalBackend(file, backgroundScope, LiveClock)
    assertEquals(Entitlement.Premium(Plan.Monthly), relaunched.purchases.entitlements.value)
  }

  @Test
  fun theTwoPlansHaveTheirPrices() = runTest {
    val backend = LocalBackend(MemoryFile(), backgroundScope, LiveClock)
    backend.purchases.plans { offers ->
      assertEquals(listOf(Plan.Monthly, Plan.Yearly), offers.map { it.plan })
      assertEquals(listOf("$4.99", "$39.99"), offers.map { it.price })
    }
  }
}
