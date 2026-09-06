// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.profile

import dev.modaal.duet.test.FixtureRunner
import kotlin.test.Test

/** Replays the recordings under parity/fixtures/profile.* against the reducer. */
class ProfileGoldenTest {
  @Test fun accountClosesLeaf() = replay("profile.account-closes")

  @Test fun nameChangeUpdatesHeaderLeaf() = replay("profile.name-change-updates-header")

  @Test fun signOutRelaysLeaf() = replay("profile.sign-out-relays")

  private fun replay(leaf: String) {
    FixtureRunner.run(
      fixture = leaf,
      stateSerializer = ProfileState.serializer(),
      actionSerializer = ProfileActionSerializer,
      payloadSerializer = ProfileEffectPayloadSerializer,
      reducer = ::profileReducer,
    )
  }
}
