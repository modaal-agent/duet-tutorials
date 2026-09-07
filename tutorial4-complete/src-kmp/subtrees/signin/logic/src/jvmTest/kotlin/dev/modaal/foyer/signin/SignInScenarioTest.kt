// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.signin

import dev.modaal.duet.kernel.Effect
import dev.modaal.duet.test.*
import dev.modaal.foyer.ports.SignInOutcome
import dev.modaal.foyer.ports.SignInProvider
import kotlin.test.Test

/**
 * The scenario the sign-in recordings are compiled from: one fresh gate and
 * four branches, the two providers, the empty address, and a failure from
 * the port.
 */
class SignInScenarioTest {
  @Test
  fun signInScenario() {
    val s =
      scenario<SignInState, SignInAction, SignInEffectPayload>(
        feature = "signin",
        description =
          "The gate sends one sign-in at a time to the auth port, rejects an empty " +
            "address before it reaches the port, and completes with a display name: " +
            "the account's saved one, or one derived from the provider.",
        source =
          "src-kmp/subtrees/signin/logic/src/jvmTest/kotlin/" +
            "dev/modaal/foyer/signin/SignInScenarioTest.kt",
      ) {
        given(SignInState())

        branch("email signs in") {
          whenAction("continue with an email", SignInAction.ContinueTapped(ann))
          then("signing in, the provider held") { it.isSigningIn && it.pending == ann }
          thenEffects("exactly the sign-in call") {
            it == effectsOf<SignInEffectPayload>(Effect.Run(SignInEffectPayload.SignIn(ann)))
          }
          whenAction("a second tap while in flight", SignInAction.ContinueTapped(ann))
          thenEffects("nothing: one sign-in at a time") { it.isEmpty() }
          whenAction(
            "the port signs the account in with no saved name",
            SignInAction.SignInFinished(SignInOutcome.SignedIn(displayName = null)))
          then("the latch is released") { !it.isSigningIn && it.pending == null }
          thenEffects("the host hears the local part of the address") {
            it == completed("ann")
          }
        }

        branch("guest signs in") {
          whenAction("continue as a guest", SignInAction.ContinueTapped(SignInProvider.Guest))
          thenEffects("exactly the sign-in call") {
            it ==
              effectsOf<SignInEffectPayload>(
                Effect.Run(SignInEffectPayload.SignIn(SignInProvider.Guest)))
          }
          whenAction(
            "the port signs the guest in",
            SignInAction.SignInFinished(SignInOutcome.SignedIn(displayName = null)))
          thenEffects("the host hears the guest name") { it == completed("Guest") }
        }

        branch("empty address fails") {
          whenAction(
            "continue with an empty address",
            SignInAction.ContinueTapped(SignInProvider.Email("")))
          then("the message is set, nothing in flight") {
            it.failure == SignInMessages.EMPTY_ADDRESS && !it.isSigningIn
          }
          thenEffects("nothing: the port is never called") { it.isEmpty() }
        }

        branch("port failure lands") {
          whenAction("continue with an email", SignInAction.ContinueTapped(ann))
          whenAction(
            "the port refuses",
            SignInAction.SignInFinished(SignInOutcome.Failed("No account for that address.")))
          then("the reason is shown, the latch released") {
            it.failure == "No account for that address." && !it.isSigningIn
          }
          thenEffects("nothing: the host is not told") { it.isEmpty() }
        }
      }

    ScenarioRunner.verifyOrRecord(
      s,
      SignInState.serializer(),
      SignInActionSerializer,
      SignInEffectPayloadSerializer,
      ::signInReducer)
  }
}

private val ann = SignInProvider.Email("ann@example.com")

private fun completed(displayName: String): List<Effect<SignInEffectPayload>> =
  effectsOf(
    Effect.Run(SignInEffectPayload.NotifyHost(SignInDelegateEvent.Completed(displayName))))
