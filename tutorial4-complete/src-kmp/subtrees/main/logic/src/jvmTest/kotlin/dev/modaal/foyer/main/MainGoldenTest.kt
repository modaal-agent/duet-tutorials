// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.main

import dev.modaal.duet.test.FixtureRunner
import kotlin.test.Test

/** Replays the recordings under parity/fixtures/main.* against the reducer. */
class MainGoldenTest {
  @Test fun tabSwitchesLeaf() = replay("main.tab-switches")

  @Test fun signOutRelaysLeaf() = replay("main.sign-out-relays")

  private fun replay(leaf: String) {
    FixtureRunner.run(
      fixture = leaf,
      stateSerializer = MainState.serializer(),
      actionSerializer = MainActionSerializer,
      payloadSerializer = MainEffectPayloadSerializer,
      reducer = ::mainReducer,
    )
  }
}
