// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.name

import dev.modaal.duet.kernel.Effect
import dev.modaal.duet.test.*
import dev.modaal.foyer.editname.EditNameMessages
import dev.modaal.foyer.ports.OnboardingPage
import kotlin.test.Test

/** The scenario the name recordings are compiled from. */
class NameScenarioTest {
  @Test
  fun nameScenario() {
    val s =
      scenario<NameState, NameAction, NameEffectPayload>(
        feature = "name",
        description =
          "The name step validates every edit with the editor's rule, publishes " +
            "its readiness on the lateral seam when it changes, climbs the trimmed " +
            "name on Continue, and refuses an empty or over-long draft with the " +
            "message the editor shows.",
        source =
          "src-kmp/subtrees/name/logic/src/jvmTest/kotlin/" +
            "dev/modaal/foyer/name/NameScenarioTest.kt",
      ) {
        given(NameState())

        branch("valid draft publishes readiness") {
          whenAction("a letter is typed", NameAction.DraftChanged("A"))
          then("ready") { it.isReady && it.draft == "A" }
          thenEffects("exactly the readiness publish, true") { it == published(true) }
          whenAction("more letters", NameAction.DraftChanged("Ann"))
          thenEffects("nothing: the readiness did not change") { it.isEmpty() }
          whenAction("the field is cleared", NameAction.DraftChanged(""))
          then("not ready") { !it.isReady }
          thenEffects("exactly the readiness publish, false") { it == published(false) }
        }

        branch("empty name rejected") {
          whenAction("Continue on the empty field", NameAction.ContinueTapped)
          then("the editor's message") { it.validation == EditNameMessages.EMPTY && !it.isReady }
          thenEffects("nothing climbs") { it.isEmpty() }
          whenAction("a letter is typed", NameAction.DraftChanged("A"))
          then("the message clears with the edit") { it.validation == null }
        }

        branch("continue climbs") {
          whenAction("a name with spaces around it", NameAction.DraftChanged(" Ann B "))
          whenAction("Continue", NameAction.ContinueTapped)
          then("no message") { it.validation == null }
          thenEffects("exactly the Continued delegate, trimmed") {
            it ==
              effectsOf<NameEffectPayload>(
                Effect.Run(NameEffectPayload.NotifyHost(NameDelegateEvent.Continued("Ann B"))))
          }
        }
      }

    ScenarioRunner.verifyOrRecord(
      s, NameState.serializer(), NameActionSerializer, NameEffectPayloadSerializer, ::nameReducer)
  }
}

private fun published(ready: Boolean): List<Effect<NameEffectPayload>> =
  effectsOf(Effect.Run(NameEffectPayload.PublishReadiness(OnboardingPage.Name, ready)))
