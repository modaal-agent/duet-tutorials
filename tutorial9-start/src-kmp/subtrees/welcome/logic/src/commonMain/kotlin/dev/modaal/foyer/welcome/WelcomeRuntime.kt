// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.welcome

import dev.modaal.duet.kernel.Store
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow

fun welcomeEffectHandler(
  environment: WelcomeEnvironment,
): (WelcomeEffectPayload) -> Flow<WelcomeAction> = { payload ->
  flow {
    when (payload) {
      is WelcomeEffectPayload.PublishReadiness ->
        environment.updateReadiness(payload.step, payload.ready)
      is WelcomeEffectPayload.NotifyHost -> environment.notifyHost(payload.event)
    }
  }
}

fun makeWelcomeStore(
  environment: WelcomeEnvironment,
  scope: CoroutineScope,
): Store<WelcomeState, WelcomeAction, WelcomeEffectPayload> =
  Store(
    initialState = WelcomeState(),
    reducer = ::welcomeReducer,
    handler = welcomeEffectHandler(environment),
    scope = scope,
  )

fun welcomeStateFlow(
  store: Store<WelcomeState, WelcomeAction, WelcomeEffectPayload>,
): StateFlow<WelcomeState> = store.state
