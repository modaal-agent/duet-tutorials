// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.welcome

import dev.modaal.duet.kernel.Effect
import dev.modaal.duet.test.TestStore
import dev.modaal.foyer.ports.OnboardingPage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

/** The readiness publish reaches the seam's producer port, and nothing re-enters. */
class WelcomeTestStoreTest {
  @Test
  fun appearingPublishesReadinessAndNothingComesBack() = runTest {
    val environment = WelcomeEnvironmentMock()
    val store =
      TestStore(
        initialState = WelcomeState(),
        reducer = ::welcomeReducer,
        handler = welcomeEffectHandler(environment),
        scope = this,
      )

    store.send(WelcomeAction.Appeared) { it }
    store.expectEffects(
      listOf(Effect.Run(WelcomeEffectPayload.PublishReadiness(OnboardingPage.Welcome, true))))
    runCurrent()
    store.finish()
    assertEquals(1, environment.updateReadinessCallCount)
    assertEquals(OnboardingPage.Welcome, environment.updateReadinessArgs.single().step)
    assertEquals(true, environment.updateReadinessArgs.single().ready)
  }
}
