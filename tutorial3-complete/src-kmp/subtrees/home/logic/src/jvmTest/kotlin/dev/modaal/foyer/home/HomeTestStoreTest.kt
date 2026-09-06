// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.home

import dev.modaal.duet.kernel.Effect
import dev.modaal.duet.test.TestStore
import dev.modaal.foyer.ports.Item
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

/** The load round trip over the generated `HomeEnvironmentMock`. */
class HomeTestStoreTest {

  @Test
  fun theListArrivesAsAnAction() = runTest {
    val environment = HomeEnvironmentMock()
    val items = listOf(Item("1", "Welcome note"))
    environment.loadItemsHandler = { onItems -> onItems(items) }
    val store =
      TestStore(
        initialState = HomeState(),
        reducer = ::homeReducer,
        handler = homeEffectHandler(environment),
        scope = this,
      )

    store.send(HomeAction.Appeared) { it.copy(isLoading = true) }
    store.expectEffects(listOf(Effect.Run(HomeEffectPayload.LoadItems)))
    runCurrent()

    store.receive(HomeAction.ItemsLoaded(items)) { it.copy(items = items, isLoading = false) }
    runCurrent()
    store.finish()
    assertEquals(1, environment.loadItemsCallCount)
  }
}
