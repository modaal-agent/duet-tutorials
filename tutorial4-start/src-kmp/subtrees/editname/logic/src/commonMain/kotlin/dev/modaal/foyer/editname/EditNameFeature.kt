// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.editname

import dev.modaal.duet.kernel.Effect
import dev.modaal.duet.kernel.Reduced
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// The name editor, level 3 of the profile tree: a text field with validation,
// a save through the account port, and two ways out. Feature spec:
// parity/feature-specs/editname.md. Recordings: parity/fixtures/editname.*.

object EditNameConfig {
  /** The longest display name the app accepts. */
  const val MAX_LENGTH: Int = 40
}

object EditNameMessages {
  const val EMPTY = "Enter a name."
  const val TOO_LONG = "Keep it to 40 characters or fewer."
}

// MARK: - State

@Serializable
data class EditNameState(
  /** The field's text, seeded with the current name. */
  val draft: String = "",
  /** The in-flight latch: one save at a time. */
  val isSaving: Boolean = false,
  /** The validation message to show, cleared by the next edit. */
  val validation: String? = null,
)

// MARK: - Actions

@Serializable(with = EditNameActionSerializer::class)
sealed interface EditNameAction {
  @Serializable @SerialName("draftChanged") data class DraftChanged(val text: String) : EditNameAction

  @Serializable @SerialName("saveTapped") data object SaveTapped : EditNameAction

  /** Environment report: the account port saved the name. */
  @Serializable @SerialName("saveFinished") data object SaveFinished : EditNameAction

  @Serializable @SerialName("cancelTapped") data object CancelTapped : EditNameAction
}

// MARK: - Delegate events

@Serializable(with = EditNameDelegateEventSerializer::class)
sealed interface EditNameDelegateEvent {
  /** The name was saved; the parent updates its own copy. */
  @Serializable @SerialName("saved") data class Saved(val name: String) : EditNameDelegateEvent

  /** The user left without saving. */
  @Serializable @SerialName("closed") data object Closed : EditNameDelegateEvent
}

// MARK: - Effect payloads

@Serializable(with = EditNameEffectPayloadSerializer::class)
sealed interface EditNameEffectPayload {
  /** Save through the account port; completion re-enters as `SaveFinished`. */
  @Serializable @SerialName("saveName") data class SaveName(val name: String) : EditNameEffectPayload

  @Serializable
  @SerialName("notifyListener")
  data class NotifyHost(val event: EditNameDelegateEvent) : EditNameEffectPayload
}

// MARK: - Reducer

/**
 * The validation guard runs at `SaveTapped`, on the trimmed draft: empty and
 * over-long names never reach the port. A save in flight makes a second tap
 * inert; completion climbs to the parent as `Saved`.
 */
fun editNameReducer(
  state: EditNameState,
  action: EditNameAction,
): Reduced<EditNameState, EditNameEffectPayload> =
  when (action) {
    is EditNameAction.DraftChanged -> Reduced(state.copy(draft = action.text, validation = null))

    EditNameAction.SaveTapped -> {
      val name = state.draft.trim()
      when {
        state.isSaving -> Reduced(state)
        name.isEmpty() -> Reduced(state.copy(validation = EditNameMessages.EMPTY))
        name.length > EditNameConfig.MAX_LENGTH ->
          Reduced(state.copy(validation = EditNameMessages.TOO_LONG))
        else ->
          Reduced(
            state.copy(isSaving = true, validation = null),
            listOf(Effect.Run(EditNameEffectPayload.SaveName(name))))
      }
    }

    EditNameAction.SaveFinished ->
      Reduced(
        state.copy(isSaving = false),
        listOf(
          Effect.Run(
            EditNameEffectPayload.NotifyHost(EditNameDelegateEvent.Saved(state.draft.trim())))))

    EditNameAction.CancelTapped ->
      Reduced(
        state,
        listOf(Effect.Run(EditNameEffectPayload.NotifyHost(EditNameDelegateEvent.Closed))))
  }
