// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.preferences

import dev.modaal.duet.test.FixtureRunner
import kotlin.test.Test

/** Replays the recordings under parity/fixtures/preferences.* against the reducer. */
class PreferencesGoldenTest {
  @Test fun togglePublishesReadinessLeaf() = replay("preferences.toggle-publishes-readiness")

  @Test fun continueNeedsOneLeaf() = replay("preferences.continue-needs-one")

  @Test fun continueClimbsLeaf() = replay("preferences.continue-climbs")

  private fun replay(leaf: String) {
    FixtureRunner.run(
      fixture = leaf,
      stateSerializer = PreferencesState.serializer(),
      actionSerializer = PreferencesActionSerializer,
      payloadSerializer = PreferencesEffectPayloadSerializer,
      reducer = ::preferencesReducer,
    )
  }
}
