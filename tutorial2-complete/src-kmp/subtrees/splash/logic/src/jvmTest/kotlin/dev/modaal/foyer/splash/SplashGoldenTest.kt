// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.splash

import dev.modaal.duet.test.FixtureRunner
import kotlin.test.Test

/**
 * Replays the recordings under parity/fixtures/splash.* against the reducer.
 * One test method per branch leaf, so a divergence in one ending fails on its
 * own line; `tools/duet verify` checks that every listed fixture replayed.
 */
class SplashGoldenTest {
  @Test fun ceremonyCompletesLeaf() = replay("splash.ceremony-completes")

  @Test fun safetyNetFiresLeaf() = replay("splash.safety-net-fires")

  @Test fun bothPathsNotifyTwiceLeaf() = replay("splash.both-paths-notify-twice")

  @Test fun repeatAppearInertLeaf() = replay("splash.repeat-appear-inert")

  private fun replay(leaf: String) {
    FixtureRunner.run(
      fixture = leaf,
      stateSerializer = SplashState.serializer(),
      actionSerializer = SplashActionSerializer,
      payloadSerializer = SplashEffectPayloadSerializer,
      reducer = ::splashReducer,
    )
  }
}
