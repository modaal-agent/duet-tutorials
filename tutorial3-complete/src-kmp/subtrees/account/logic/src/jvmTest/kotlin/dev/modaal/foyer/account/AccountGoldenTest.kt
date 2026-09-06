// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.account

import dev.modaal.duet.test.FixtureRunner
import kotlin.test.Test

/** Replays the recordings under parity/fixtures/account.* against the reducer. */
class AccountGoldenTest {
  @Test fun editNameSavesAndClimbsLeaf() = replay("account.edit-name-saves-and-climbs")

  @Test fun editNameCancelsLeaf() = replay("account.edit-name-cancels")

  @Test fun signOutRequestsOnceLeaf() = replay("account.sign-out-requests-once")

  @Test fun closeClimbsLeaf() = replay("account.close-climbs")

  private fun replay(leaf: String) {
    FixtureRunner.run(
      fixture = leaf,
      stateSerializer = AccountState.serializer(),
      actionSerializer = AccountActionSerializer,
      payloadSerializer = AccountEffectPayloadSerializer,
      reducer = ::accountReducer,
    )
  }
}
