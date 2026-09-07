// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.account

import dev.modaal.duet.kernel.Store
import dev.modaal.foyer.ports.awaitCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow

fun accountEffectHandler(
  environment: AccountEnvironment,
): (AccountEffectPayload) -> Flow<AccountAction> = { payload ->
  flow {
    when (payload) {
      AccountEffectPayload.SignOut -> {
        awaitCallback<Unit> { onDone -> environment.signOut { onDone(Unit) } }
        emit(AccountAction.SignedOut)
      }
      is AccountEffectPayload.NotifyHost -> environment.notifyHost(payload.event)
    }
  }
}

/** The store a shell hosts, seeded with the display name the parent shows. */
fun makeAccountStore(
  displayName: String,
  environment: AccountEnvironment,
  scope: CoroutineScope,
): Store<AccountState, AccountAction, AccountEffectPayload> =
  Store(
    initialState = AccountState(displayName = displayName),
    reducer = ::accountReducer,
    handler = accountEffectHandler(environment),
    scope = scope,
  )

fun accountStateFlow(
  store: Store<AccountState, AccountAction, AccountEffectPayload>,
): StateFlow<AccountState> = store.state
