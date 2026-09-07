// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.signin

import dev.modaal.duet.kernel.Store
import dev.modaal.foyer.ports.SignInOutcome
import dev.modaal.foyer.ports.awaitCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow

/**
 * The effect handler: `SignIn` calls the environment and waits for its one
 * callback, which becomes `SignInFinished`; `NotifyHost` calls the sink and
 * emits nothing.
 */
fun signInEffectHandler(
  environment: SignInEnvironment,
): (SignInEffectPayload) -> Flow<SignInAction> = { payload ->
  flow {
    when (payload) {
      is SignInEffectPayload.SignIn -> {
        val outcome =
          awaitCallback<SignInOutcome> { onOutcome ->
            environment.signIn(payload.provider, onOutcome)
          }
        emit(SignInAction.SignInFinished(outcome))
      }
      is SignInEffectPayload.NotifyHost -> environment.notifyHost(payload.event)
    }
  }
}

/** The store every shell hosts: the reducer and the handler, wired. */
fun makeSignInStore(
  environment: SignInEnvironment,
  scope: CoroutineScope,
): Store<SignInState, SignInAction, SignInEffectPayload> =
  Store(
    initialState = SignInState(),
    reducer = ::signInReducer,
    handler = signInEffectHandler(environment),
    scope = scope,
  )

/** [Store.state] in a concrete-typed position, for the Apple boundary. */
fun signInStateFlow(
  store: Store<SignInState, SignInAction, SignInEffectPayload>,
): StateFlow<SignInState> = store.state
