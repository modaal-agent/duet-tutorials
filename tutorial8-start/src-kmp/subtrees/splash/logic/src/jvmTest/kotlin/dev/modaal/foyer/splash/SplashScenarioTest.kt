// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.splash

import dev.modaal.duet.kernel.Effect
import dev.modaal.duet.test.*
import kotlin.test.Test

/**
 * The scenario the splash recordings are compiled from. One given (a fresh
 * store that has just appeared) and four branches: the two ways the splash
 * can end, the case where both paths fire, and the arming guard.
 * `tools/duet record` runs this test with regeneration on and writes one
 * fixture per branch under parity/fixtures/.
 */
class SplashScenarioTest {
  @Test
  fun splashScenario() {
    val s =
      scenario<SplashState, SplashAction, SplashEffectPayload>(
        feature = "splash",
        description =
          "The splash arms a safety net when it appears and completes by whichever " +
            "path gets there first: the animation ending, or the net firing. Both " +
            "paths notify the host every time; a repeat Appeared is inert.",
        source =
          "src-kmp/subtrees/splash/logic/src/jvmTest/kotlin/" +
            "dev/modaal/foyer/splash/SplashScenarioTest.kt",
      ) {
        given(SplashState())

        whenAction("the splash appears", SplashAction.Appeared)
        then("the safety net is armed") { it.isArmed }
        thenEffects("exactly the keyed safety net, carrying its duration") {
          it ==
            effectsOf<SplashEffectPayload>(
              Effect.Run(
                SplashEffectPayload.ArmSafetyNet(SplashConfig.SAFETY_NET_MILLIS),
                id = SplashEffectIds.SAFETY_NET))
        }

        branch("ceremony completes") {
          whenAction("the animation reaches its end", SplashAction.CeremonyFinished)
          then("the arming latch is untouched") { it.isArmed }
          thenEffects("the host is notified, by the ceremony path") {
            it == notified(SplashCompletionPath.Ceremony)
          }
        }

        branch("safety net fires") {
          whenAction("the armed delay elapses instead", SplashAction.SafetyNetElapsed)
          then("the arming latch is untouched") { it.isArmed }
          thenEffects("the host is notified, by the safety-net path") {
            it == notified(SplashCompletionPath.SafetyNet)
          }
        }

        branch("both paths notify twice") {
          whenAction("the animation reaches its end", SplashAction.CeremonyFinished)
          thenEffects("the host is notified, by the ceremony path") {
            it == notified(SplashCompletionPath.Ceremony)
          }
          whenAction("the armed delay elapses on top of it", SplashAction.SafetyNetElapsed)
          thenEffects("the host is notified again, by the safety-net path") {
            it == notified(SplashCompletionPath.SafetyNet)
          }
        }

        branch("repeat appear inert") {
          whenAction("the splash reports appearing a second time", SplashAction.Appeared)
          then("still armed, unchanged") { it.isArmed }
          thenEffects("nothing: the net is not re-armed") { it.isEmpty() }
        }
      }

    ScenarioRunner.verifyOrRecord(
      s,
      SplashState.serializer(),
      SplashActionSerializer,
      SplashEffectPayloadSerializer,
      ::splashReducer)
  }
}

/** The one delegate effect, by path: what every completion branch expects. */
private fun notified(path: SplashCompletionPath): List<Effect<SplashEffectPayload>> =
  effectsOf(
    Effect.Run(SplashEffectPayload.NotifyHost(SplashDelegateEvent.Completed(path))))
