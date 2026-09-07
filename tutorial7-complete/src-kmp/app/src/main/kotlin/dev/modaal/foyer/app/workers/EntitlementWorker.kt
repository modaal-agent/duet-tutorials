// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.app.workers

import dev.modaal.duet.shells.Relay
import dev.modaal.duet.shells.Working
import dev.modaal.foyer.ports.PurchasesPort
import dev.modaal.foyer.root.RootAction

/**
 * Observes the purchases port's entitlement stream for the root mount's
 * lifetime and reports each value as the root's `EntitlementChanged`: the
 * only path by which the entitlement enters feature state.
 */
class EntitlementWorker(
  private val purchases: PurchasesPort,
  private val relay: Relay<RootAction>,
) : Working {
  override suspend fun run() {
    purchases.entitlements.collect { entitlement ->
      relay.send(RootAction.EntitlementChanged(entitlement))
    }
  }
}
