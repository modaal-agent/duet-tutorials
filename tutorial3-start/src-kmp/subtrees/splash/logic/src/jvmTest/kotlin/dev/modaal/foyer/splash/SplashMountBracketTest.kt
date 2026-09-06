// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.splash

import dev.modaal.duet.kernel.Effect
import dev.modaal.duet.test.TestStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

/**
 * The mount bracket: a shell builds a store when the splash is mounted and
 * tears it down when the splash leaves; a second mount builds a second store.
 * The arming latch is per store, so the second mount arms a fresh net, and
 * the torn-down store's net never fires.
 */
class SplashMountBracketTest {
  @Test
  fun aSecondMountArmsAFreshNet() = runTest {
    val environment = RecordingSplashEnvironment()
    val first =
      TestStore(
        initialState = SplashState(),
        reducer = ::splashReducer,
        handler = splashEffectHandler(environment),
        scope = this,
      )
    first.send(SplashAction.Appeared) { it.copy(isArmed = true) }
    first.expectEffects(
      listOf(
        Effect.Run(
          SplashEffectPayload.ArmSafetyNet(SplashConfig.SAFETY_NET_MILLIS),
          id = SplashEffectIds.SAFETY_NET)))
    first.teardown()

    // The second mount, over the same environment: a fresh store, a fresh latch.
    val second =
      TestStore(
        initialState = SplashState(),
        reducer = ::splashReducer,
        handler = splashEffectHandler(environment),
        scope = this,
      )
    second.send(SplashAction.Appeared) { it.copy(isArmed = true) }
    second.expectEffects(
      listOf(
        Effect.Run(
          SplashEffectPayload.ArmSafetyNet(SplashConfig.SAFETY_NET_MILLIS),
          id = SplashEffectIds.SAFETY_NET)))

    advanceTimeBy(SplashConfig.SAFETY_NET_MILLIS)
    runCurrent()

    // One net fired: the second store's. The first store's was cancelled at
    // teardown, and `finish()` on the first store fails on any action that
    // arrived there.
    second.receive(SplashAction.SafetyNetElapsed)
    second.expectEffects(
      listOf(
        Effect.Run(
          SplashEffectPayload.NotifyHost(
            SplashDelegateEvent.Completed(SplashCompletionPath.SafetyNet)))))
    runCurrent()
    first.finish()
    second.finish()
    assertEquals(
      listOf<SplashDelegateEvent>(
        SplashDelegateEvent.Completed(SplashCompletionPath.SafetyNet)),
      environment.notified)
  }
}
