// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.app.services

import dev.modaal.foyer.ports.Plan
import dev.modaal.foyer.ports.PlanOffer
import dev.modaal.foyer.ports.PurchaseOutcome
import dev.modaal.foyer.ports.PurchasesPort

/**
 * The purchases port as a mock service: two plans with fixed prices, and a
 * purchase that succeeds at once. Nothing consumes it yet; Tutorial 4's
 * entitlement stream and Tutorial 5's upgrade flow do.
 */
class MockPurchases : PurchasesPort {
  override fun plans(onPlans: (List<PlanOffer>) -> Unit) =
    onPlans(listOf(PlanOffer(Plan.Monthly, "$4.99"), PlanOffer(Plan.Yearly, "$39.99")))

  override fun purchase(plan: Plan, onOutcome: (PurchaseOutcome) -> Unit) =
    onOutcome(PurchaseOutcome.Purchased(plan))
}
