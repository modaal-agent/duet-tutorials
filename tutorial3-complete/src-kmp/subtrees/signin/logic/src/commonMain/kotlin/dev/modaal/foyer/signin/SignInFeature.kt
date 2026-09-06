// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.signin

import dev.modaal.duet.kernel.Effect
import dev.modaal.duet.kernel.Reduced
import dev.modaal.foyer.ports.SignInOutcome
import dev.modaal.foyer.ports.SignInProvider
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// The sign-in gate: state, actions, effect payloads and the reducer. Feature
// spec: parity/feature-specs/signin.md. Recordings: parity/fixtures/signin.*.

object SignInMessages {
  const val EMPTY_ADDRESS = "Enter an email address."
}

// MARK: - State

@Serializable
data class SignInState(
  /** The in-flight latch: one sign-in at a time. */
  val isSigningIn: Boolean = false,
  /** The last failure to show, cleared by the next attempt. */
  val failure: String? = null,
  /** The provider in flight, so a nameless outcome still gets a display name. */
  val pending: SignInProvider? = null,
)

// MARK: - Actions

@Serializable(with = SignInActionSerializer::class)
sealed interface SignInAction {
  /** Shell report: the user chose a provider and tapped Continue. */
  @Serializable
  @SerialName("continueTapped")
  data class ContinueTapped(val provider: SignInProvider) : SignInAction

  /** Environment report: the auth port answered. */
  @Serializable
  @SerialName("signInFinished")
  data class SignInFinished(val outcome: SignInOutcome) : SignInAction
}

// MARK: - Delegate events

/** What the gate tells its host: who signed in. */
@Serializable(with = SignInDelegateEventSerializer::class)
sealed interface SignInDelegateEvent {
  @Serializable
  @SerialName("completed")
  data class Completed(val displayName: String) : SignInDelegateEvent
}

// MARK: - Effect payloads

@Serializable(with = SignInEffectPayloadSerializer::class)
sealed interface SignInEffectPayload {
  /** Ask the auth port to sign in; the answer re-enters as `SignInFinished`. */
  @Serializable @SerialName("signIn") data class SignIn(val provider: SignInProvider) : SignInEffectPayload

  /**
   * Hand a delegate event to the host. The serial name `notifyListener` is
   * the one the chain runner looks for when a hop crosses this seam.
   */
  @Serializable
  @SerialName("notifyListener")
  data class NotifyHost(val event: SignInDelegateEvent) : SignInEffectPayload
}

// MARK: - Reducer

/**
 * An empty address never reaches the port; a second tap while one sign-in is
 * in flight is inert; a success completes the gate with a display name, the
 * account's saved one when the outcome carries it and a derived one otherwise.
 */
fun signInReducer(
  state: SignInState,
  action: SignInAction,
): Reduced<SignInState, SignInEffectPayload> =
  when (action) {
    is SignInAction.ContinueTapped ->
      when {
        state.isSigningIn -> Reduced(state)
        action.provider.isEmptyAddress ->
          Reduced(state.copy(failure = SignInMessages.EMPTY_ADDRESS))
        else ->
          Reduced(
            state.copy(isSigningIn = true, failure = null, pending = action.provider),
            listOf(Effect.Run(SignInEffectPayload.SignIn(action.provider))))
      }

    is SignInAction.SignInFinished ->
      when (val outcome = action.outcome) {
        is SignInOutcome.SignedIn ->
          Reduced(
            state.copy(isSigningIn = false, pending = null),
            listOf(
              Effect.Run(
                SignInEffectPayload.NotifyHost(
                  SignInDelegateEvent.Completed(
                    outcome.displayName ?: defaultDisplayName(state.pending))))))
        is SignInOutcome.Failed ->
          Reduced(state.copy(isSigningIn = false, pending = null, failure = outcome.reason))
      }
  }

private val SignInProvider.isEmptyAddress: Boolean
  get() = this is SignInProvider.Email && address.isBlank()

/** The name the app shows when the account has none saved. */
fun defaultDisplayName(provider: SignInProvider?): String =
  when (provider) {
    is SignInProvider.Email -> provider.address.substringBefore('@')
    SignInProvider.Guest, null -> "Guest"
  }
