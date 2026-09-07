// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.progress

import dev.modaal.duet.kernel.Store
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow

/**
 * The observation as an effect: the flow stays open for the store's lifetime,
 * each value the seam delivers becomes one action, and the store's teardown
 * cancels the flow, which cancels the subscription.
 */
fun progressEffectHandler(
  environment: ProgressEnvironment,
): (ProgressEffectPayload) -> Flow<ProgressAction> = { payload ->
  when (payload) {
    ProgressEffectPayload.ObserveReadiness ->
      callbackFlow {
        val subscription =
          environment.observeReadiness { readiness ->
            trySend(ProgressAction.ReadinessChanged(readiness.step, readiness.ready))
          }
        awaitClose { subscription.cancel() }
      }
  }
}

fun makeProgressStore(
  environment: ProgressEnvironment,
  scope: CoroutineScope,
): Store<ProgressState, ProgressAction, ProgressEffectPayload> =
  Store(
    initialState = ProgressState(),
    reducer = ::progressReducer,
    handler = progressEffectHandler(environment),
    scope = scope,
  )

fun progressStateFlow(
  store: Store<ProgressState, ProgressAction, ProgressEffectPayload>,
): StateFlow<ProgressState> = store.state
