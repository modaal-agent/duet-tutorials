// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.onboarding

import dev.modaal.duet.test.FixtureRunner
import kotlin.test.Test

/** Replays the recordings under parity/fixtures/onboarding.* against the reducer. */
class OnboardingGoldenTest {
  @Test fun stepsAdvanceToCompletionLeaf() = replay("onboarding.steps-advance-to-completion")

  @Test fun backWalksThePagesLeaf() = replay("onboarding.back-walks-the-pages")

  @Test fun backOnWelcomeInertLeaf() = replay("onboarding.back-on-welcome-inert")

  private fun replay(leaf: String) {
    FixtureRunner.run(
      fixture = leaf,
      stateSerializer = OnboardingState.serializer(),
      actionSerializer = OnboardingActionSerializer,
      payloadSerializer = OnboardingEffectPayloadSerializer,
      reducer = ::onboardingReducer,
    )
  }
}
