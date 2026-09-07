// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.name

import dev.modaal.duet.kernel.Store
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow

fun nameEffectHandler(environment: NameEnvironment): (NameEffectPayload) -> Flow<NameAction> =
  { payload ->
    flow {
      when (payload) {
        is NameEffectPayload.PublishReadiness -> environment.updateReadiness(payload.step, payload.ready)
        is NameEffectPayload.NotifyHost -> environment.notifyHost(payload.event)
      }
    }
  }

fun makeNameStore(
  environment: NameEnvironment,
  scope: CoroutineScope,
): Store<NameState, NameAction, NameEffectPayload> =
  Store(
    initialState = NameState(),
    reducer = ::nameReducer,
    handler = nameEffectHandler(environment),
    scope = scope,
  )

fun nameStateFlow(store: Store<NameState, NameAction, NameEffectPayload>): StateFlow<NameState> =
  store.state
