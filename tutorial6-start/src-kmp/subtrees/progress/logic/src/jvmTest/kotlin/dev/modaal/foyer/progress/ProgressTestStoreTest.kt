// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.progress

import dev.modaal.duet.kernel.Effect
import dev.modaal.duet.test.TestStore
import dev.modaal.foyer.ports.OnboardingPage
import dev.modaal.foyer.ports.ReadinessSubscription
import dev.modaal.foyer.ports.StepReadiness
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

/**
 * The seam's values re-enter as actions, and the store's teardown cancels
 * the subscription, over the generated `ProgressEnvironmentMock` with a
 * hand-written sticky source behind its handler.
 */
class ProgressTestStoreTest {
  @Test
  fun eachSeamValueReentersAsAnActionUntilTeardown() = runTest {
    val environment = ProgressEnvironmentMock()
    var sink: ((StepReadiness) -> Unit)? = null
    var cancelled = false
    environment.observeReadinessHandler = { onChange ->
      sink = onChange
      // Sticky: the current value first.
      onChange(StepReadiness(OnboardingPage.Welcome, true))
      object : ReadinessSubscription {
        override fun cancel() {
          cancelled = true
        }
      }
    }
    val store =
      TestStore(
        initialState = ProgressState(),
        reducer = ::progressReducer,
        handler = progressEffectHandler(environment),
        scope = this,
      )

    store.send(ProgressAction.Appeared) { it.copy(isObserving = true) }
    store.expectEffects(listOf(Effect.Run(ProgressEffectPayload.ObserveReadiness)))
    runCurrent()
    store.receive(ProgressAction.ReadinessChanged(OnboardingPage.Welcome, true)) {
      it.copy(readiness = mapOf(OnboardingPage.Welcome to true))
    }

    sink?.invoke(StepReadiness(OnboardingPage.Name, true))
    runCurrent()
    store.receive(ProgressAction.ReadinessChanged(OnboardingPage.Name, true)) {
      it.copy(readiness = mapOf(OnboardingPage.Welcome to true, OnboardingPage.Name to true))
    }

    // The observation is long-lived by design: the host's teardown is what
    // ends it, and the test mirrors that teardown.
    store.teardown()
    runCurrent()
    assertTrue(cancelled, "teardown cancels the subscription")
    assertEquals(1, environment.observeReadinessCallCount)
  }
}
