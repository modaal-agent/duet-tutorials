// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.main

import dev.modaal.duet.kernel.Store
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow

fun mainEffectHandler(environment: MainEnvironment): (MainEffectPayload) -> Flow<MainAction> =
  { payload ->
    flow {
      when (payload) {
        is MainEffectPayload.NotifyHost -> environment.notifyHost(payload.event)
      }
    }
  }

fun makeMainStore(
  environment: MainEnvironment,
  scope: CoroutineScope,
): Store<MainState, MainAction, MainEffectPayload> =
  Store(
    initialState = MainState(),
    reducer = ::mainReducer,
    handler = mainEffectHandler(environment),
    scope = scope,
  )

fun mainStateFlow(store: Store<MainState, MainAction, MainEffectPayload>): StateFlow<MainState> =
  store.state
