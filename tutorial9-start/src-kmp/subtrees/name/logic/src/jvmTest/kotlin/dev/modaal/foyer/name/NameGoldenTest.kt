// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.name

import dev.modaal.duet.test.FixtureRunner
import kotlin.test.Test

/** Replays the recordings under parity/fixtures/name.* against the reducer. */
class NameGoldenTest {
  @Test fun validDraftPublishesReadinessLeaf() = replay("name.valid-draft-publishes-readiness")

  @Test fun emptyNameRejectedLeaf() = replay("name.empty-name-rejected")

  @Test fun continueClimbsLeaf() = replay("name.continue-climbs")

  private fun replay(leaf: String) {
    FixtureRunner.run(
      fixture = leaf,
      stateSerializer = NameState.serializer(),
      actionSerializer = NameActionSerializer,
      payloadSerializer = NameEffectPayloadSerializer,
      reducer = ::nameReducer,
    )
  }
}
