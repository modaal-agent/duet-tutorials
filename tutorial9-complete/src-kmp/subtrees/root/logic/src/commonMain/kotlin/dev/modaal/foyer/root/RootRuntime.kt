// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.root

import dev.modaal.duet.kernel.Store
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow

fun rootEffectHandler(environment: RootEnvironment): (RootEffectPayload) -> Flow<RootAction> =
  { payload ->
    flow {
      when (payload) {
        is RootEffectPayload.ForwardLink -> environment.forwardLink(payload.link)
        is RootEffectPayload.CompleteOnboarding ->
          environment.completeOnboarding(payload.name, payload.preferences)
        is RootEffectPayload.Track -> environment.track(payload.event)
      }
    }
  }

/** The store the composition root hosts. */
fun makeRootStore(
  environment: RootEnvironment,
  scope: CoroutineScope,
): Store<RootState, RootAction, RootEffectPayload> =
  Store(
    initialState = RootState(),
    reducer = ::rootReducer,
    handler = rootEffectHandler(environment),
    scope = scope,
  )

fun rootStateFlow(store: Store<RootState, RootAction, RootEffectPayload>): StateFlow<RootState> =
  store.state
