// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.home

import dev.modaal.duet.kernel.Store
import dev.modaal.foyer.ports.Item
import dev.modaal.foyer.ports.awaitCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow

fun homeEffectHandler(environment: HomeEnvironment): (HomeEffectPayload) -> Flow<HomeAction> =
  { payload ->
    flow {
      when (payload) {
        HomeEffectPayload.LoadItems -> {
          val items = awaitCallback<List<Item>> { onItems -> environment.loadItems(onItems) }
          emit(HomeAction.ItemsLoaded(items))
        }
      }
    }
  }

fun makeHomeStore(
  environment: HomeEnvironment,
  scope: CoroutineScope,
): Store<HomeState, HomeAction, HomeEffectPayload> =
  Store(
    initialState = HomeState(),
    reducer = ::homeReducer,
    handler = homeEffectHandler(environment),
    scope = scope,
  )

fun homeStateFlow(store: Store<HomeState, HomeAction, HomeEffectPayload>): StateFlow<HomeState> =
  store.state
