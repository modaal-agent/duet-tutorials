// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.signin

import dev.modaal.duet.kernel.Effect
import dev.modaal.duet.test.TestStore
import dev.modaal.foyer.ports.SignInOutcome
import dev.modaal.foyer.ports.SignInProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

/**
 * The effect handler over the generated environment mock. The recordings pin
 * what the reducer emits; this suite pins that the port's callback re-enters
 * as `SignInFinished` and that the delegate reaches the sink.
 * `SignInEnvironmentMock` is generated at test compilation from
 * `SignInEnvironment`; nothing here is committed.
 */
class SignInTestStoreTest {

  @Test
  fun thePortsAnswerReentersAsAnAction() = runTest {
    val environment = SignInEnvironmentMock()
    environment.signInHandler = { _, onOutcome -> onOutcome(SignInOutcome.SignedIn("Ann")) }
    val store =
      TestStore(
        initialState = SignInState(),
        reducer = ::signInReducer,
        handler = signInEffectHandler(environment),
        scope = this,
      )

    val ann = SignInProvider.Email("ann@example.com")
    store.send(SignInAction.ContinueTapped(ann)) { it.copy(isSigningIn = true, pending = ann) }
    store.expectEffects(listOf(Effect.Run(SignInEffectPayload.SignIn(ann))))
    runCurrent()

    store.receive(SignInAction.SignInFinished(SignInOutcome.SignedIn("Ann"))) {
      it.copy(isSigningIn = false, pending = null)
    }
    store.expectEffects(
      listOf(Effect.Run(SignInEffectPayload.NotifyHost(SignInDelegateEvent.Completed("Ann")))))
    runCurrent()
    store.finish()

    assertEquals(listOf<SignInProvider>(ann), environment.signInArgs)
    assertEquals(
      listOf<SignInDelegateEvent>(SignInDelegateEvent.Completed("Ann")),
      environment.notifyHostArgs)
  }

  @Test
  fun aFailureFromThePortLandsInState() = runTest {
    val environment = SignInEnvironmentMock()
    environment.signInHandler = { _, onOutcome -> onOutcome(SignInOutcome.Failed("Try later.")) }
    val store =
      TestStore(
        initialState = SignInState(),
        reducer = ::signInReducer,
        handler = signInEffectHandler(environment),
        scope = this,
      )

    store.send(SignInAction.ContinueTapped(SignInProvider.Guest)) {
      it.copy(isSigningIn = true, pending = SignInProvider.Guest)
    }
    store.expectEffects(listOf(Effect.Run(SignInEffectPayload.SignIn(SignInProvider.Guest))))
    runCurrent()

    store.receive(SignInAction.SignInFinished(SignInOutcome.Failed("Try later."))) {
      it.copy(isSigningIn = false, pending = null, failure = "Try later.")
    }
    runCurrent()
    store.finish()
    assertEquals(0, environment.notifyHostCallCount)
  }
}
