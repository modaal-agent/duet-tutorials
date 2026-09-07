// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.account

import dev.modaal.duet.kernel.Effect
import dev.modaal.duet.kernel.Reduced
import dev.modaal.foyer.editname.EditNameDelegateEvent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// The account screen, level 2 of the profile tree: the display name, the
// editor it mounts, and the sign-out. Feature spec:
// parity/feature-specs/account.md. Recordings: parity/fixtures/account.*.

// MARK: - State

/** The one child this level can mount. */
@Serializable(with = AccountRouteSerializer::class)
sealed interface AccountRoute {
  @Serializable @SerialName("editName") data object EditName : AccountRoute
}

@Serializable
data class AccountState(
  val displayName: String = "",
  /** The mounted child, or none. The shell mounts from this value. */
  val child: AccountRoute? = null,
  /** The in-flight latch: one sign-out at a time. */
  val isSigningOut: Boolean = false,
)

// MARK: - Actions

@Serializable(with = AccountActionSerializer::class)
sealed interface AccountAction {
  @Serializable @SerialName("editNameTapped") data object EditNameTapped : AccountAction

  @Serializable @SerialName("signOutTapped") data object SignOutTapped : AccountAction

  /** Environment report: the auth port ended the session. */
  @Serializable @SerialName("signedOut") data object SignedOut : AccountAction

  @Serializable @SerialName("closeTapped") data object CloseTapped : AccountAction

  /** The child's delegate events, received as this level's actions. */
  @Serializable
  @SerialName("editName")
  data class EditName(val event: EditNameDelegateEvent) : AccountAction
}

// MARK: - Delegate events

@Serializable(with = AccountDelegateEventSerializer::class)
sealed interface AccountDelegateEvent {
  /** The user left the account screen. */
  @Serializable @SerialName("closed") data object Closed : AccountDelegateEvent

  /** The name changed; every parent that shows it updates. */
  @Serializable @SerialName("nameChanged") data class NameChanged(val name: String) : AccountDelegateEvent

  /** The session ended; this climbs to the root, which raises the gate. */
  @Serializable @SerialName("signOutRequested") data object SignOutRequested : AccountDelegateEvent
}

// MARK: - Effect payloads

@Serializable(with = AccountEffectPayloadSerializer::class)
sealed interface AccountEffectPayload {
  /** End the session through the auth port; completion re-enters as `SignedOut`. */
  @Serializable @SerialName("signOut") data object SignOut : AccountEffectPayload

  @Serializable
  @SerialName("notifyListener")
  data class NotifyHost(val event: AccountDelegateEvent) : AccountEffectPayload
}

// MARK: - Reducer

/**
 * The child's `Saved` updates this level's name and climbs on as
 * `NameChanged`; its `Closed` clears the child. A sign-out goes to the port
 * once and climbs as `SignOutRequested` when the port confirms.
 */
fun accountReducer(
  state: AccountState,
  action: AccountAction,
): Reduced<AccountState, AccountEffectPayload> =
  when (action) {
    AccountAction.EditNameTapped -> Reduced(state.copy(child = AccountRoute.EditName))

    is AccountAction.EditName ->
      when (val event = action.event) {
        is EditNameDelegateEvent.Saved ->
          Reduced(
            state.copy(displayName = event.name, child = null),
            listOf(
              Effect.Run(
                AccountEffectPayload.NotifyHost(AccountDelegateEvent.NameChanged(event.name)))))
        EditNameDelegateEvent.Closed -> Reduced(state.copy(child = null))
      }

    AccountAction.SignOutTapped ->
      if (state.isSigningOut) {
        Reduced(state)
      } else {
        Reduced(
          state.copy(isSigningOut = true), listOf(Effect.Run(AccountEffectPayload.SignOut)))
      }

    AccountAction.SignedOut ->
      Reduced(
        state.copy(isSigningOut = false),
        listOf(
          Effect.Run(AccountEffectPayload.NotifyHost(AccountDelegateEvent.SignOutRequested))))

    AccountAction.CloseTapped ->
      Reduced(
        state, listOf(Effect.Run(AccountEffectPayload.NotifyHost(AccountDelegateEvent.Closed))))
  }
