// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.upgrade

import dev.modaal.foyer.ports.Plan
import dev.modaal.foyer.ports.PlanOffer
import dev.modaal.foyer.ports.PurchaseOutcome

/** The upgrade flow's door to the platform: the two purchases-port calls and the delegate sink. */
interface UpgradeEnvironment {
  /** Ask for the plans on offer; `onPlans` fires once with the purchases port's answer. */
  fun loadPlans(onPlans: (List<PlanOffer>) -> Unit)

  /** Buy a plan; `onOutcome` fires once with the purchases port's answer. */
  fun purchase(plan: Plan, onOutcome: (PurchaseOutcome) -> Unit)

  fun notifyHost(event: UpgradeDelegateEvent)
}
