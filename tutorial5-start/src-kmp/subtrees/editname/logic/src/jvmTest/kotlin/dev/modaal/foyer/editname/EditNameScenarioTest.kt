// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.editname

import dev.modaal.duet.kernel.Effect
import dev.modaal.duet.test.*
import kotlin.test.Test

/** The scenario the editor's recordings are compiled from. */
class EditNameScenarioTest {
  @Test
  fun editNameScenario() {
    val s =
      scenario<EditNameState, EditNameAction, EditNameEffectPayload>(
        feature = "editname",
        description =
          "The editor validates the trimmed draft at Save: empty and over-long " +
            "names never reach the account port; a valid one saves once and climbs " +
            "to the parent as Saved; Cancel climbs as Closed.",
        source =
          "src-kmp/subtrees/editname/logic/src/jvmTest/kotlin/" +
            "dev/modaal/foyer/editname/EditNameScenarioTest.kt",
      ) {
        given(EditNameState(draft = "Ann"))

        branch("valid name saves") {
          whenAction("the user types a new name", EditNameAction.DraftChanged(" Ann B "))
          then("the draft holds the raw text") { it.draft == " Ann B " && it.validation == null }
          whenAction("save", EditNameAction.SaveTapped)
          then("saving, no message") { it.isSaving && it.validation == null }
          thenEffects("exactly the save, trimmed") {
            it ==
              effectsOf<EditNameEffectPayload>(Effect.Run(EditNameEffectPayload.SaveName("Ann B")))
          }
          whenAction("a second save while in flight", EditNameAction.SaveTapped)
          thenEffects("nothing: one save at a time") { it.isEmpty() }
          whenAction("the port confirms", EditNameAction.SaveFinished)
          then("the latch is released") { !it.isSaving }
          thenEffects("the parent hears the saved name") {
            it == notified(EditNameDelegateEvent.Saved("Ann B"))
          }
        }

        branch("empty name rejected") {
          whenAction("the user clears the field", EditNameAction.DraftChanged("   "))
          whenAction("save", EditNameAction.SaveTapped)
          then("the empty message, nothing in flight") {
            it.validation == EditNameMessages.EMPTY && !it.isSaving
          }
          thenEffects("nothing: the port is never called") { it.isEmpty() }
        }

        branch("long name rejected") {
          whenAction(
            "the user types 41 characters",
            EditNameAction.DraftChanged("A".repeat(EditNameConfig.MAX_LENGTH + 1)))
          whenAction("save", EditNameAction.SaveTapped)
          then("the length message") { it.validation == EditNameMessages.TOO_LONG }
          thenEffects("nothing") { it.isEmpty() }
          whenAction("one character removed", EditNameAction.DraftChanged("A".repeat(40)))
          then("the message clears on edit") { it.validation == null }
        }

        branch("cancel closes") {
          whenAction("cancel", EditNameAction.CancelTapped)
          then("the draft is untouched") { it.draft == "Ann" }
          thenEffects("the parent hears Closed") { it == notified(EditNameDelegateEvent.Closed) }
        }
      }

    ScenarioRunner.verifyOrRecord(
      s,
      EditNameState.serializer(),
      EditNameActionSerializer,
      EditNameEffectPayloadSerializer,
      ::editNameReducer)
  }
}

private fun notified(event: EditNameDelegateEvent): List<Effect<EditNameEffectPayload>> =
  effectsOf(Effect.Run(EditNameEffectPayload.NotifyHost(event)))
