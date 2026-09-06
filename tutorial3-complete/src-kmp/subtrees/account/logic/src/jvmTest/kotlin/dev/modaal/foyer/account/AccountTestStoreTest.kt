// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.account

import dev.modaal.duet.kernel.Effect
import dev.modaal.duet.test.TestStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

/** The sign-out round trip over the generated `AccountEnvironmentMock`. */
class AccountTestStoreTest {

  @Test
  fun theSignOutClimbsWhenThePortConfirms() = runTest {
    val environment = AccountEnvironmentMock()
    environment.signOutHandler = { onDone -> onDone() }
    val store =
      TestStore(
        initialState = AccountState(displayName = "Ann"),
        reducer = ::accountReducer,
        handler = accountEffectHandler(environment),
        scope = this,
      )

    store.send(AccountAction.SignOutTapped) { it.copy(isSigningOut = true) }
    store.expectEffects(listOf(Effect.Run(AccountEffectPayload.SignOut)))
    runCurrent()

    store.receive(AccountAction.SignedOut) { it.copy(isSigningOut = false) }
    store.expectEffects(
      listOf(Effect.Run(AccountEffectPayload.NotifyHost(AccountDelegateEvent.SignOutRequested))))
    runCurrent()
    store.finish()

    assertEquals(1, environment.signOutCallCount)
    assertEquals(
      listOf<AccountDelegateEvent>(AccountDelegateEvent.SignOutRequested),
      environment.notifyHostArgs)
  }
}
