// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.home

import dev.modaal.foyer.ports.Item
import dev.modaal.foyer.ports.Plan
import dev.modaal.foyer.ports.PurchaseOutcome

/** The home tab's door to the platform: the items call and the purchase call. No delegate yet. */
interface HomeEnvironment {
  /** Load the list; `onItems` fires once with the items port's answer. */
  fun loadItems(onItems: (List<Item>) -> Unit)

  /** Buy a plan; `onOutcome` fires once with the purchases port's answer. */
  fun purchase(plan: Plan, onOutcome: (PurchaseOutcome) -> Unit)
}
