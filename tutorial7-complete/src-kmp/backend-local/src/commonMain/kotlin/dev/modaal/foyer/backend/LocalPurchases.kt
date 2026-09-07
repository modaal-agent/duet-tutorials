// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.backend

import dev.modaal.duet.kernel.KernelClock
import dev.modaal.foyer.ports.Entitlement
import dev.modaal.foyer.ports.Plan
import dev.modaal.foyer.ports.PlanOffer
import dev.modaal.foyer.ports.PurchaseOutcome
import dev.modaal.foyer.ports.PurchasesPort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object LocalPurchasesConfig {
  /** A purchase takes this long, on the clock seam, so the in-flight state is visible. */
  const val PURCHASE_MILLIS = 1_000L
}

/**
 * The purchases port, on device. Two plans with fixed prices; a purchase
 * succeeds after [LocalPurchasesConfig.PURCHASE_MILLIS] and is persisted.
 * `entitlements` is sticky and is the only writer of the value the home's
 * paid check reads: the purchase's own callback carries the outcome, never
 * the entitlement.
 */
class LocalPurchases(
  private val storage: LocalStorage,
  private val clock: KernelClock,
  private val scope: CoroutineScope,
) : PurchasesPort {
  private val mutableEntitlements = MutableStateFlow(storage.record.plan.toEntitlement())
  override val entitlements: StateFlow<Entitlement> = mutableEntitlements.asStateFlow()

  override fun plans(onPlans: (List<PlanOffer>) -> Unit) = onPlans(offers)

  override fun purchase(plan: Plan, onOutcome: (PurchaseOutcome) -> Unit) {
    scope.launch {
      clock.sleep(LocalPurchasesConfig.PURCHASE_MILLIS * 1_000_000L)
      storage.update { it.copy(plan = plan) }
      mutableEntitlements.value = Entitlement.Premium(plan)
      onOutcome(PurchaseOutcome.Purchased(plan))
    }
  }

  private companion object {
    val offers = listOf(PlanOffer(Plan.Monthly, "$4.99"), PlanOffer(Plan.Yearly, "$39.99"))
  }
}

private fun Plan?.toEntitlement(): Entitlement =
  if (this == null) Entitlement.Free else Entitlement.Premium(this)
