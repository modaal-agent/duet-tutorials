// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.editname

import dev.modaal.duet.kernel.Store
import dev.modaal.foyer.ports.awaitCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow

fun editNameEffectHandler(
  environment: EditNameEnvironment,
): (EditNameEffectPayload) -> Flow<EditNameAction> = { payload ->
  flow {
    when (payload) {
      is EditNameEffectPayload.SaveName -> {
        awaitCallback<Unit> { onSaved -> environment.saveName(payload.name) { onSaved(Unit) } }
        emit(EditNameAction.SaveFinished)
      }
      is EditNameEffectPayload.NotifyHost -> environment.notifyHost(payload.event)
    }
  }
}

/** The store a shell hosts, seeded with the name being edited. */
fun makeEditNameStore(
  currentName: String,
  environment: EditNameEnvironment,
  scope: CoroutineScope,
): Store<EditNameState, EditNameAction, EditNameEffectPayload> =
  Store(
    initialState = EditNameState(draft = currentName),
    reducer = ::editNameReducer,
    handler = editNameEffectHandler(environment),
    scope = scope,
  )

fun editNameStateFlow(
  store: Store<EditNameState, EditNameAction, EditNameEffectPayload>,
): StateFlow<EditNameState> = store.state
