// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.home

import kotlin.test.Test
import kotlin.test.fail

/**
 * Tutorial 5's closing exercise, deliberately failing until you finish it:
 * the Insights card unlocks on the entitlement stream's emission, never on
 * the upgrade flow's Done. Record the chain `chain-upgrade-entitlement`
 * across the upgrade flow, the root and the home tab, list it in the
 * manifest, and replace this class with the chain test that records it.
 */
class Tutorial5ExerciseUpgradeEntitlementChainTest {
  @Test
  fun recordTheUpgradeEntitlementChain() {
    fail(
      "Tutorial 5 exercise: record chain-upgrade-entitlement — the card unlocks on " +
        "EntitlementChanged from the stream, not on the flow's Completed")
  }
}
