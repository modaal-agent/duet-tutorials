// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.home

import dev.modaal.duet.test.FixtureRunner
import kotlin.test.Test

/** Replays the recordings under parity/fixtures/home.* against the reducer. */
class HomeGoldenTest {
  @Test fun loadsOnceLeaf() = replay("home.loads-once")

  @Test fun reappearKeepsItemsLeaf() = replay("home.reappear-keeps-items")

  @Test fun lockedCardPresentsPromoLeaf() = replay("home.locked-card-presents-promo")

  @Test fun unlockedCardPresentsInsightsLeaf() = replay("home.unlocked-card-presents-insights")

  @Test fun promoRequestsTheUpgradeLeaf() = replay("home.promo-requests-the-upgrade")

  @Test fun cardUnlocksFromTheStreamLeaf() = replay("home.card-unlocks-from-the-stream")

  private fun replay(leaf: String) {
    FixtureRunner.run(
      fixture = leaf,
      stateSerializer = HomeState.serializer(),
      actionSerializer = HomeActionSerializer,
      payloadSerializer = HomeEffectPayloadSerializer,
      reducer = ::homeReducer,
    )
  }
}
