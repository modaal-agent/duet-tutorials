// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.welcome

import dev.modaal.duet.test.FixtureRunner
import kotlin.test.Test

/** Replays the recordings under parity/fixtures/welcome.* against the reducer. */
class WelcomeGoldenTest {
  @Test fun appearancePublishesReadinessLeaf() = replay("welcome.appearance-publishes-readiness")

  @Test fun continueClimbsLeaf() = replay("welcome.continue-climbs")

  private fun replay(leaf: String) {
    FixtureRunner.run(
      fixture = leaf,
      stateSerializer = WelcomeState.serializer(),
      actionSerializer = WelcomeActionSerializer,
      payloadSerializer = WelcomeEffectPayloadSerializer,
      reducer = ::welcomeReducer,
    )
  }
}
