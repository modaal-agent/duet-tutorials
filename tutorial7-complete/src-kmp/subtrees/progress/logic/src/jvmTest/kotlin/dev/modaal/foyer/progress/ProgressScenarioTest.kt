// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.progress

import dev.modaal.duet.kernel.Effect
import dev.modaal.duet.test.*
import dev.modaal.foyer.ports.OnboardingPage
import kotlin.test.Test

/** The scenario the progress recordings are compiled from. */
class ProgressScenarioTest {
  @Test
  fun progressScenario() {
    val s =
      scenario<ProgressState, ProgressAction, ProgressEffectPayload>(
        feature = "progress",
        description =
          "The progress row subscribes to the readiness seam once, on its first " +
            "appearance; the page arrives as the level's projection and each " +
            "step's readiness arrives laterally, and the row writes both as they come.",
        source =
          "src-kmp/subtrees/progress/logic/src/jvmTest/kotlin/" +
            "dev/modaal/foyer/progress/ProgressScenarioTest.kt",
      ) {
        given(ProgressState())

        branch("appearance observes once") {
          whenAction("the row appears", ProgressAction.Appeared)
          then("observing") { it.isObserving }
          thenEffects("exactly the subscription") {
            it ==
              effectsOf<ProgressEffectPayload>(Effect.Run(ProgressEffectPayload.ObserveReadiness))
          }
          whenAction("the row appears again", ProgressAction.Appeared)
          thenEffects("nothing: one subscription per mount") { it.isEmpty() }
        }

        branch("page projects down") {
          whenAction(
            "the level moves to the name step",
            ProgressAction.PageChanged(OnboardingPage.Name))
          then("step 2 of 3") { it.page == OnboardingPage.Name && it.page.stepNumber == 2 }
          thenEffects("nothing") { it.isEmpty() }
        }

        branch("readiness arrives laterally") {
          whenAction(
            "the seam reports the welcome step ready",
            ProgressAction.ReadinessChanged(OnboardingPage.Welcome, true))
          then("one tick") { it.readiness == mapOf(OnboardingPage.Welcome to true) }
          whenAction(
            "the seam reports the name step ready",
            ProgressAction.ReadinessChanged(OnboardingPage.Name, true))
          whenAction(
            "the seam reports the name step not ready after all",
            ProgressAction.ReadinessChanged(OnboardingPage.Name, false))
          then("the latest value per step") {
            it.readiness == mapOf(OnboardingPage.Welcome to true, OnboardingPage.Name to false)
          }
          thenEffects("nothing") { it.isEmpty() }
        }
      }

    ScenarioRunner.verifyOrRecord(
      s,
      ProgressState.serializer(),
      ProgressActionSerializer,
      ProgressEffectPayloadSerializer,
      ::progressReducer)
  }
}
