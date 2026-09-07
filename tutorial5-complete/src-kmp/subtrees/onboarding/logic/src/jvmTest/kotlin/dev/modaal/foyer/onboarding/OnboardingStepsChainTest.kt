// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.onboarding

import dev.modaal.duet.kernel.Effect
import dev.modaal.duet.test.*
import dev.modaal.foyer.name.NameAction
import dev.modaal.foyer.name.NameActionSerializer
import dev.modaal.foyer.name.NameDelegateEvent
import dev.modaal.foyer.name.NameDelegateEventSerializer
import dev.modaal.foyer.name.NameEffectPayload
import dev.modaal.foyer.name.NameEffectPayloadSerializer
import dev.modaal.foyer.name.NameState
import dev.modaal.foyer.name.nameReducer
import dev.modaal.foyer.ports.OnboardingPage
import kotlin.test.Test

/**
 * The chain scenario for `chain-onboarding-name`: the name step's Continued
 * crosses into the level as `Name(event)`, which keeps the name and moves the
 * page. The level's other seam, the readiness publish, has no chain: a
 * lateral seam carries no delegate hop.
 */
class OnboardingStepsChainTest {

  private val name =
    ChainNode(
      "name",
      NameState(draft = "Ann", isReady = true),
      NameState.serializer(),
      NameActionSerializer,
      NameEffectPayloadSerializer,
      ::nameReducer)

  private val onboarding =
    ChainNode(
      "onboarding",
      OnboardingState(page = OnboardingPage.Name),
      OnboardingState.serializer(),
      OnboardingActionSerializer,
      OnboardingEffectPayloadSerializer,
      ::onboardingReducer)

  @Test
  fun theNameStepsContinuedMovesTheLevel() {
    val chain =
      chainScenario(
        chain = "onboarding-name",
        fixture = "chain-onboarding-name",
        description =
          "The name step's Continued crosses into the onboarding level as Name(event); " +
            "the level keeps the name and mounts the preferences step.",
        source =
          "src-kmp/subtrees/onboarding/logic/src/jvmTest/kotlin/" +
            "dev/modaal/foyer/onboarding/OnboardingStepsChainTest.kt",
      ) {
        whenAction(name, "Continue on a valid name", NameAction.ContinueTapped)
        thenEffects(name, "exactly the Continued delegate") {
          it ==
            effectsOf<NameEffectPayload>(
              Effect.Run(NameEffectPayload.NotifyHost(NameDelegateEvent.Continued("Ann"))))
        }
        hop(
          "the step's delegate is the level's action",
          from = name,
          to = onboarding,
          delegateSerializer = NameDelegateEventSerializer,
        ) { event ->
          OnboardingAction.Name(event)
        }
        then(onboarding, "the preferences step, the name kept") {
          it.page == OnboardingPage.Preferences && it.name == "Ann"
        }
        thenEffects(onboarding, "nothing climbs yet") { it.isEmpty() }
      }

    ChainScenarioRunner.verifyOrRecord(chain)
  }
}
