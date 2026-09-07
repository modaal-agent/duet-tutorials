// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.progress

import dev.modaal.duet.test.FixtureRunner
import kotlin.test.Test

/** Replays the recordings under parity/fixtures/progress.* against the reducer. */
class ProgressGoldenTest {
  @Test fun appearanceObservesOnceLeaf() = replay("progress.appearance-observes-once")

  @Test fun pageProjectsDownLeaf() = replay("progress.page-projects-down")

  @Test fun readinessArrivesLaterallyLeaf() = replay("progress.readiness-arrives-laterally")

  private fun replay(leaf: String) {
    FixtureRunner.run(
      fixture = leaf,
      stateSerializer = ProgressState.serializer(),
      actionSerializer = ProgressActionSerializer,
      payloadSerializer = ProgressEffectPayloadSerializer,
      reducer = ::progressReducer,
    )
  }
}
