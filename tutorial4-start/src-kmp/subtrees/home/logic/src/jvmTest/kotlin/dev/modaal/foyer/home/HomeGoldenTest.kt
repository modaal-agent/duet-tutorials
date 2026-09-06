// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.home

import dev.modaal.duet.test.FixtureRunner
import kotlin.test.Test

/** Replays the recordings under parity/fixtures/home.* against the reducer. */
class HomeGoldenTest {
  @Test fun loadsOnceLeaf() = replay("home.loads-once")

  @Test fun reappearKeepsItemsLeaf() = replay("home.reappear-keeps-items")

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
