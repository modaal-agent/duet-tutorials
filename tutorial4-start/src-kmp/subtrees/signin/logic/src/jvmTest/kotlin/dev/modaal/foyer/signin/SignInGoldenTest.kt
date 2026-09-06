// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.signin

import dev.modaal.duet.test.FixtureRunner
import kotlin.test.Test

/** Replays the recordings under parity/fixtures/signin.* against the reducer. */
class SignInGoldenTest {
  @Test fun emailSignsInLeaf() = replay("signin.email-signs-in")

  @Test fun guestSignsInLeaf() = replay("signin.guest-signs-in")

  @Test fun emptyAddressFailsLeaf() = replay("signin.empty-address-fails")

  @Test fun portFailureLandsLeaf() = replay("signin.port-failure-lands")

  private fun replay(leaf: String) {
    FixtureRunner.run(
      fixture = leaf,
      stateSerializer = SignInState.serializer(),
      actionSerializer = SignInActionSerializer,
      payloadSerializer = SignInEffectPayloadSerializer,
      reducer = ::signInReducer,
    )
  }
}
