// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.preferences

import dev.modaal.duet.kernel.Store
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow

fun preferencesEffectHandler(
  environment: PreferencesEnvironment,
): (PreferencesEffectPayload) -> Flow<PreferencesAction> = { payload ->
  flow {
    when (payload) {
      is PreferencesEffectPayload.PublishReadiness ->
        environment.updateReadiness(payload.step, payload.ready)
      is PreferencesEffectPayload.NotifyHost -> environment.notifyHost(payload.event)
    }
  }
}

fun makePreferencesStore(
  environment: PreferencesEnvironment,
  scope: CoroutineScope,
): Store<PreferencesState, PreferencesAction, PreferencesEffectPayload> =
  Store(
    initialState = PreferencesState(),
    reducer = ::preferencesReducer,
    handler = preferencesEffectHandler(environment),
    scope = scope,
  )

fun preferencesStateFlow(
  store: Store<PreferencesState, PreferencesAction, PreferencesEffectPayload>,
): StateFlow<PreferencesState> = store.state
