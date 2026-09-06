// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.root

import dev.modaal.duet.kernel.Store
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow

/** The root has no effects yet, so its handler never runs. */
fun rootEffectHandler(): (RootEffectPayload) -> Flow<RootAction> = { emptyFlow() }

/** The store the composition root hosts. No environment: the root routes. */
fun makeRootStore(scope: CoroutineScope): Store<RootState, RootAction, RootEffectPayload> =
  Store(
    initialState = RootState(),
    reducer = ::rootReducer,
    handler = rootEffectHandler(),
    scope = scope,
  )

fun rootStateFlow(store: Store<RootState, RootAction, RootEffectPayload>): StateFlow<RootState> =
  store.state
