// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.onboarding

import dev.modaal.duet.kernel.Effect
import dev.modaal.duet.test.*
import dev.modaal.foyer.name.NameDelegateEvent
import dev.modaal.foyer.ports.OnboardingPage
import dev.modaal.foyer.preferences.PreferencesDelegateEvent
import dev.modaal.foyer.welcome.WelcomeDelegateEvent
import kotlin.test.Test

/** The scenario the onboarding recordings are compiled from. */
class OnboardingScenarioTest {
  @Test
  fun onboardingScenario() {
    val s =
      scenario<OnboardingState, OnboardingAction, OnboardingEffectPayload>(
        feature = "onboarding",
        description =
          "The onboarding level moves its page forward on each step's Continued, " +
            "keeps the name and the preferences the steps hand up, completes on the " +
            "last one, and walks the page back on Back, which is inert on the first page.",
        source =
          "src-kmp/subtrees/onboarding/logic/src/jvmTest/kotlin/" +
            "dev/modaal/foyer/onboarding/OnboardingScenarioTest.kt",
      ) {
        given(OnboardingState())

        branch("steps advance to completion") {
          whenAction("the welcome step continues", OnboardingAction.Welcome(WelcomeDelegateEvent.Continued))
          then("the name step") { it.page == OnboardingPage.Name }
          thenEffects("nothing: mounting is the shell's job") { it.isEmpty() }
          whenAction(
            "the name step continues with a name",
            OnboardingAction.Name(NameDelegateEvent.Continued("Ann")))
          then("the preferences step, the name kept") {
            it.page == OnboardingPage.Preferences && it.name == "Ann"
          }
          whenAction(
            "the preferences step continues with a selection",
            OnboardingAction.Preferences(PreferencesDelegateEvent.Continued(listOf("digest"))))
          then("the selection kept, the page stays until the host moves on") {
            it.preferences == listOf("digest") && it.page == OnboardingPage.Preferences
          }
          thenEffects("exactly the Completed delegate with both answers") {
            it ==
              effectsOf<OnboardingEffectPayload>(
                Effect.Run(
                  OnboardingEffectPayload.NotifyHost(
                    OnboardingDelegateEvent.Completed("Ann", listOf("digest")))))
          }
        }

        branch("back walks the pages") {
          whenAction("the welcome step continues", OnboardingAction.Welcome(WelcomeDelegateEvent.Continued))
          whenAction(
            "the name step continues",
            OnboardingAction.Name(NameDelegateEvent.Continued("Ann")))
          whenAction("Back on the preferences step", OnboardingAction.Back)
          then("the name step again, the name kept") {
            it.page == OnboardingPage.Name && it.name == "Ann"
          }
          thenEffects("nothing: Back is a state change") { it.isEmpty() }
          whenAction("Back on the name step", OnboardingAction.Back)
          then("the welcome step") { it.page == OnboardingPage.Welcome }
        }

        branch("back on welcome inert") {
          whenAction("Back on the first page", OnboardingAction.Back)
          then("nothing changed") { it.page == OnboardingPage.Welcome }
          thenEffects("nothing: the gate is back-inert") { it.isEmpty() }
          whenAction(
            "a name step event while the welcome step is mounted",
            OnboardingAction.Name(NameDelegateEvent.Continued("stray")))
          then("inert: the page decides which step's events count") {
            it.page == OnboardingPage.Welcome && it.name == null
          }
        }
      }

    ScenarioRunner.verifyOrRecord(
      s,
      OnboardingState.serializer(),
      OnboardingActionSerializer,
      OnboardingEffectPayloadSerializer,
      ::onboardingReducer)
  }
}
