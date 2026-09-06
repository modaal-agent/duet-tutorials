// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.splash

import dev.modaal.duet.kernel.Effect
import dev.modaal.duet.test.TestStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

/**
 * The safety net's timing: it fires after its duration and not before,
 * exactly once, and not at all after teardown. The recordings pin what the
 * reducer emits; this suite drives the effect handler through the real store
 * on the test scheduler's virtual clock, so no test waits on wall time.
 */
class SplashTestStoreTest {

  @Test
  fun safetyNetDoesNotFireBeforeItsDuration() = runTest {
    val environment = RecordingSplashEnvironment()
    val store = armedStore(environment)

    advanceTimeBy(SplashConfig.SAFETY_NET_MILLIS - 1)
    runCurrent()

    // Teardown first: a net still sleeping at `finish()` would count as an
    // effect left in flight. Then `finish()` fails on any action that arrived
    // and was never received.
    store.teardown()
    store.finish()
    assertEquals(emptyList(), environment.notified)
  }

  @Test
  fun safetyNetFiresAfterItsDuration() = runTest {
    val environment = RecordingSplashEnvironment()
    val store = armedStore(environment)

    advanceTimeBy(SplashConfig.SAFETY_NET_MILLIS)
    runCurrent()

    // `receive`, not `send`: the handler emitted this action, so the store
    // already has it and the test states what arrived.
    store.receive(SplashAction.SafetyNetElapsed)
    store.expectEffects(listOf(notifySafetyNet))
    runCurrent()
    store.finish()
    assertEquals(
      listOf<SplashDelegateEvent>(
        SplashDelegateEvent.Completed(SplashCompletionPath.SafetyNet)),
      environment.notified)
  }

  @Test
  fun safetyNetFiresExactlyOnce() = runTest {
    val environment = RecordingSplashEnvironment()
    val store = armedStore(environment)

    // Two advances across the deadline: a periodic wiring would emit a second
    // `SafetyNetElapsed`, which `finish()` reports as an unreceived action.
    advanceTimeBy(2_000)
    advanceTimeBy(3_000)
    runCurrent()

    store.receive(SplashAction.SafetyNetElapsed)
    store.expectEffects(listOf(notifySafetyNet))
    runCurrent()
    store.finish()
    assertEquals(1, environment.notified.size)
  }

  @Test
  fun teardownCancelsTheArmedNet() = runTest {
    val environment = RecordingSplashEnvironment()
    val store = armedStore(environment)

    store.teardown()
    advanceTimeBy(SplashConfig.SAFETY_NET_MILLIS * 3)
    runCurrent()

    store.finish()
    assertEquals(emptyList(), environment.notified)
  }

  /** Every test's given: a store whose splash has appeared, so the net is armed. */
  private fun TestScope.armedStore(
    environment: SplashEnvironment,
  ): TestStore<SplashState, SplashAction, SplashEffectPayload> {
    val store =
      TestStore(
        initialState = SplashState(),
        reducer = ::splashReducer,
        handler = splashEffectHandler(environment),
        scope = this,
      )
    store.send(SplashAction.Appeared) { it.copy(isArmed = true) }
    store.expectEffects(
      listOf(
        Effect.Run(
          SplashEffectPayload.ArmSafetyNet(SplashConfig.SAFETY_NET_MILLIS),
          id = SplashEffectIds.SAFETY_NET)))
    return store
  }
}

private val notifySafetyNet: Effect<SplashEffectPayload> =
  Effect.Run(
    SplashEffectPayload.NotifyHost(
      SplashDelegateEvent.Completed(SplashCompletionPath.SafetyNet)))
