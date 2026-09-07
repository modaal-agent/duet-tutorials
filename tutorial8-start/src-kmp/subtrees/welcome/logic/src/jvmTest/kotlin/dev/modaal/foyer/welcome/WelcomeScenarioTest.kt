// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.welcome

import dev.modaal.duet.kernel.Effect
import dev.modaal.duet.test.*
import dev.modaal.foyer.ports.OnboardingPage
import kotlin.test.Test

/** The scenario the welcome recordings are compiled from. */
class WelcomeScenarioTest {
  @Test
  fun welcomeScenario() {
    val s =
      scenario<WelcomeState, WelcomeAction, WelcomeEffectPayload>(
        feature = "welcome",
        description =
          "The welcome step is ready at mount and publishes that readiness on the " +
            "lateral seam when it appears; Continue climbs to the onboarding level.",
        source =
          "src-kmp/subtrees/welcome/logic/src/jvmTest/kotlin/" +
            "dev/modaal/foyer/welcome/WelcomeScenarioTest.kt",
      ) {
        given(WelcomeState())

        branch("appearance publishes readiness") {
          whenAction("the step appears", WelcomeAction.Appeared)
          then("ready, as it always is") { it.isReady }
          thenEffects("exactly the readiness publish, true") {
            it ==
              effectsOf<WelcomeEffectPayload>(
                Effect.Run(WelcomeEffectPayload.PublishReadiness(OnboardingPage.Welcome, true)))
          }
        }

        branch("continue climbs") {
          whenAction("Continue", WelcomeAction.ContinueTapped)
          thenEffects("exactly the Continued delegate") {
            it ==
              effectsOf<WelcomeEffectPayload>(
                Effect.Run(WelcomeEffectPayload.NotifyHost(WelcomeDelegateEvent.Continued)))
          }
        }
      }

    ScenarioRunner.verifyOrRecord(
      s,
      WelcomeState.serializer(),
      WelcomeActionSerializer,
      WelcomeEffectPayloadSerializer,
      ::welcomeReducer)
  }
}
