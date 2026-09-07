// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.root

import dev.modaal.duet.test.FixtureRunner
import kotlin.test.Test

/** Replays the recordings under parity/fixtures/root.* against the reducer. */
class RootGoldenTest {
  @Test fun splashBeforeAuthHoldsLeaf() = replay("root.splash-before-auth-holds")

  @Test fun gateAfterSplashLeaf() = replay("root.gate-after-splash")

  @Test fun gatePicksOnboardingLeaf() = replay("root.gate-picks-onboarding")

  @Test fun onboardingCompletesIntoMainLeaf() = replay("root.onboarding-completes-into-main")

  @Test fun lateSplashInertLeaf() = replay("root.late-splash-inert")

  @Test fun signedInSkipsGateLeaf() = replay("root.signed-in-skips-gate")

  @Test fun signOutReturnsToGateLeaf() = replay("root.sign-out-returns-to-gate")

  @Test fun sessionExpiryReturnsToGateLeaf() = replay("root.session-expiry-returns-to-gate")

  @Test fun entitlementChangesLeaf() = replay("root.entitlement-changes")

  @Test fun linkForwardsUnderMainLeaf() = replay("root.link-forwards-under-main")

  @Test fun linkWaitsForMainLeaf() = replay("root.link-waits-for-main")

  private fun replay(leaf: String) {
    FixtureRunner.run(
      fixture = leaf,
      stateSerializer = RootState.serializer(),
      actionSerializer = RootActionSerializer,
      payloadSerializer = RootEffectPayloadSerializer,
      reducer = ::rootReducer,
    )
  }
}
