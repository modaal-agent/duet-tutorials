// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.editname

import dev.modaal.duet.test.FixtureRunner
import kotlin.test.Test

/** Replays the recordings under parity/fixtures/editname.* against the reducer. */
class EditNameGoldenTest {
  @Test fun validNameSavesLeaf() = replay("editname.valid-name-saves")

  @Test fun emptyNameRejectedLeaf() = replay("editname.empty-name-rejected")

  @Test fun longNameRejectedLeaf() = replay("editname.long-name-rejected")

  @Test fun cancelClosesLeaf() = replay("editname.cancel-closes")

  private fun replay(leaf: String) {
    FixtureRunner.run(
      fixture = leaf,
      stateSerializer = EditNameState.serializer(),
      actionSerializer = EditNameActionSerializer,
      payloadSerializer = EditNameEffectPayloadSerializer,
      reducer = ::editNameReducer,
    )
  }
}
