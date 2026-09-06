// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.splash

import dev.modaal.duet.kernel.Effect
import dev.modaal.duet.test.TestStore
import kotlin.test.Test
import kotlin.test.fail
import kotlinx.coroutines.test.runTest

/**
 * Tutorial 2's closing exercise: the mount bracket. A shell builds a store
 * when the splash is mounted and tears it down when the splash leaves; a
 * second mount builds a second store. This test fails until you finish it.
 *
 * The given is written: one store, armed, then torn down. Finish the test so
 * it pins that a second store over the same environment arms a fresh net, and
 * that after advancing the clock past the duration the host is notified
 * exactly once — the torn-down store's net never fires.
 */
class Tutorial2ExerciseMountBracketTest {
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

    fail("Tutorial 2 exercise: mount a second store and pin the fresh net")
  }
}
