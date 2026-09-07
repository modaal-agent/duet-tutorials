// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.preferences

import dev.modaal.duet.kernel.Effect
import dev.modaal.duet.test.*
import dev.modaal.foyer.ports.OnboardingPage
import kotlin.test.Test

/** The scenario the preferences recordings are compiled from. */
class PreferencesScenarioTest {
  @Test
  fun preferencesScenario() {
    val s =
      scenario<PreferencesState, PreferencesAction, PreferencesEffectPayload>(
        feature = "preferences",
        description =
          "The preferences step flips one key per toggle, is ready with at least " +
            "one key on, publishes its readiness on the lateral seam when it " +
            "changes, and climbs the selection on Continue; Continue with nothing " +
            "on is inert.",
        source =
          "src-kmp/subtrees/preferences/logic/src/jvmTest/kotlin/" +
            "dev/modaal/foyer/preferences/PreferencesScenarioTest.kt",
      ) {
        given(PreferencesState())

        branch("toggle publishes readiness") {
          whenAction("the tips toggle", PreferencesAction.PreferenceToggled("tips"))
          then("one key on, ready") { it.selected == listOf("tips") && it.isReady }
          thenEffects("exactly the readiness publish, true") { it == published(true) }
          whenAction("the digest toggle", PreferencesAction.PreferenceToggled("digest"))
          then("two keys, in the screen's order") { it.selected == listOf("digest", "tips") }
          thenEffects("nothing: still ready") { it.isEmpty() }
          whenAction("tips off again", PreferencesAction.PreferenceToggled("tips"))
          whenAction("digest off again", PreferencesAction.PreferenceToggled("digest"))
          then("nothing on, not ready") { it.selected.isEmpty() && !it.isReady }
          thenEffects("exactly the readiness publish, false") { it == published(false) }
        }

        branch("continue needs one") {
          whenAction("Continue with nothing on", PreferencesAction.ContinueTapped)
          then("state is untouched") { !it.isReady }
          thenEffects("nothing climbs") { it.isEmpty() }
          whenAction("an unknown key", PreferencesAction.PreferenceToggled("unknown"))
          thenEffects("nothing: the key is not on the screen") { it.isEmpty() }
        }

        branch("continue climbs") {
          whenAction("the reminders toggle", PreferencesAction.PreferenceToggled("reminders"))
          whenAction("Continue", PreferencesAction.ContinueTapped)
          thenEffects("exactly the Continued delegate with the selection") {
            it ==
              effectsOf<PreferencesEffectPayload>(
                Effect.Run(
                  PreferencesEffectPayload.NotifyHost(
                    PreferencesDelegateEvent.Continued(listOf("reminders")))))
          }
        }
      }

    ScenarioRunner.verifyOrRecord(
      s,
      PreferencesState.serializer(),
      PreferencesActionSerializer,
      PreferencesEffectPayloadSerializer,
      ::preferencesReducer)
  }
}

private fun published(ready: Boolean): List<Effect<PreferencesEffectPayload>> =
  effectsOf(
    Effect.Run(PreferencesEffectPayload.PublishReadiness(OnboardingPage.Preferences, ready)))
