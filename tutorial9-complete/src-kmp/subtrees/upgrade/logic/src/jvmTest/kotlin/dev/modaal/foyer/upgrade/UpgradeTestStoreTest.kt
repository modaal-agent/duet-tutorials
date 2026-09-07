// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.upgrade

import dev.modaal.duet.kernel.Effect
import dev.modaal.duet.test.TestStore
import dev.modaal.foyer.ports.Plan
import dev.modaal.foyer.ports.PurchaseOutcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

/** The purchase round trip over the generated `UpgradeEnvironmentMock`. */
class UpgradeTestStoreTest {

  @Test
  fun thePurchaseAnswerMovesTheStepAndWritesNoEntitlement() = runTest {
    val environment = UpgradeEnvironmentMock()
    environment.purchaseHandler = { plan, onOutcome -> onOutcome(PurchaseOutcome.Purchased(plan)) }
    val store =
      TestStore(
        initialState = UpgradeState(step = UpgradeStep.Confirm(Plan.Monthly)),
        reducer = ::upgradeReducer,
        handler = upgradeEffectHandler(environment),
        scope = this,
      )

    store.send(UpgradeAction.ConfirmTapped) { it.copy(isPurchasing = true) }
    store.expectEffects(listOf(Effect.Run(UpgradeEffectPayload.Purchase(Plan.Monthly))))
    runCurrent()

    store.receive(UpgradeAction.PurchaseFinished(PurchaseOutcome.Purchased(Plan.Monthly))) {
      it.copy(isPurchasing = false, step = UpgradeStep.Done)
    }
    store.expectEffects(
      listOf(Effect.Run(UpgradeEffectPayload.Track(UpgradeEvents.completed(Plan.Monthly)))))
    runCurrent()
    store.finish()
    assertEquals(listOf<Plan>(Plan.Monthly), environment.purchaseArgs)
    assertEquals(0, environment.notifyHostCallCount)
    // The handler forwarded the event to the environment's sink member, once.
    assertEquals(listOf(UpgradeEvents.completed(Plan.Monthly)), environment.trackArgs)
  }
}
