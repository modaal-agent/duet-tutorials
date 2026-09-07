// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.editname

import dev.modaal.duet.kernel.Effect
import dev.modaal.duet.test.TestStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

/**
 * The save round trip over the generated `EditNameEnvironmentMock`: the
 * port's callback re-enters as `SaveFinished`, and the saved name reaches
 * the sink.
 */
class EditNameTestStoreTest {

  @Test
  fun aSaveRoundTripsThroughThePort() = runTest {
    val environment = EditNameEnvironmentMock()
    environment.saveNameHandler = { _, onSaved -> onSaved() }
    val store =
      TestStore(
        initialState = EditNameState(draft = "Ann B"),
        reducer = ::editNameReducer,
        handler = editNameEffectHandler(environment),
        scope = this,
      )

    store.send(EditNameAction.SaveTapped) { it.copy(isSaving = true) }
    store.expectEffects(listOf(Effect.Run(EditNameEffectPayload.SaveName("Ann B"))))
    runCurrent()

    store.receive(EditNameAction.SaveFinished) { it.copy(isSaving = false) }
    store.expectEffects(
      listOf(Effect.Run(EditNameEffectPayload.NotifyHost(EditNameDelegateEvent.Saved("Ann B")))))
    runCurrent()
    store.finish()

    assertEquals(listOf("Ann B"), environment.saveNameArgs)
    assertEquals(
      listOf<EditNameDelegateEvent>(EditNameDelegateEvent.Saved("Ann B")),
      environment.notifyHostArgs)
  }
}
