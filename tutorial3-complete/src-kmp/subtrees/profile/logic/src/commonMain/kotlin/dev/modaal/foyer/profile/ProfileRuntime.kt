// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.profile

import dev.modaal.duet.kernel.Store
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow

fun profileEffectHandler(
  environment: ProfileEnvironment,
): (ProfileEffectPayload) -> Flow<ProfileAction> = { payload ->
  flow {
    when (payload) {
      is ProfileEffectPayload.NotifyHost -> environment.notifyHost(payload.event)
    }
  }
}

/** The store a shell hosts, seeded with the display name from the root's auth snapshot. */
fun makeProfileStore(
  displayName: String,
  environment: ProfileEnvironment,
  scope: CoroutineScope,
): Store<ProfileState, ProfileAction, ProfileEffectPayload> =
  Store(
    initialState = ProfileState(displayName = displayName),
    reducer = ::profileReducer,
    handler = profileEffectHandler(environment),
    scope = scope,
  )

fun profileStateFlow(
  store: Store<ProfileState, ProfileAction, ProfileEffectPayload>,
): StateFlow<ProfileState> = store.state
