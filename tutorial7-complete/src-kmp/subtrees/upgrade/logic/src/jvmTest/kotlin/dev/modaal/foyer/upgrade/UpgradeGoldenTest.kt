// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.upgrade

import dev.modaal.duet.test.FixtureRunner
import kotlin.test.Test

/** Replays the recordings under parity/fixtures/upgrade.* against the reducer. */
class UpgradeGoldenTest {
  @Test fun plansLoadOnceLeaf() = replay("upgrade.plans-load-once")

  @Test fun selectConfirmPurchaseDoneLeaf() = replay("upgrade.select-confirm-purchase-done")

  @Test fun backWalksTheStepsLeaf() = replay("upgrade.back-walks-the-steps")

  @Test fun purchaseFailureLandsLeaf() = replay("upgrade.purchase-failure-lands")

  @Test fun backOnDoneCompletesLeaf() = replay("upgrade.back-on-done-completes")

  @Test fun dismissFromOutsideLeaf() = replay("upgrade.dismiss-from-outside")

  private fun replay(leaf: String) {
    FixtureRunner.run(
      fixture = leaf,
      stateSerializer = UpgradeState.serializer(),
      actionSerializer = UpgradeActionSerializer,
      payloadSerializer = UpgradeEffectPayloadSerializer,
      reducer = ::upgradeReducer,
    )
  }
}
