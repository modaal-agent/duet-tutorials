// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.signin

import dev.modaal.duet.kernel.Effect
import dev.modaal.duet.services.telemetry.TrackedEvent
import dev.modaal.duet.services.telemetry.encodedName
import dev.modaal.duet.services.telemetry.encodedProperties
import dev.modaal.duet.test.TestStore
import dev.modaal.foyer.ports.SignInOutcome
import dev.modaal.foyer.ports.SignInProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

/**
 * Tutorial 9's closing exercise, finished: the sign-in event reaches the
 * sink through the effect handler with the provider kind as its one
 * property, a refusal reaches it with nothing, and the vendor-facing name is
 * the one the dashboard will key on. The recording `signin.email-signs-in`
 * pins the emission; this suite pins the forwarding and the encoding.
 */
class SignInTrackedEventsTest {

  @Test
  fun aCompletedSignInReachesTheSinkWithTheProviderKind() = runTest {
    val environment = SignInEnvironmentMock()
    environment.signInHandler = { _, onOutcome -> onOutcome(SignInOutcome.SignedIn(null, hasOnboarded = true)) }
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
    store.receive(SignInAction.SignInFinished(SignInOutcome.SignedIn(null, hasOnboarded = true))) {
      it.copy(isSigningIn = false, pending = null)
    }
    store.expectEffects(
      listOf(
        Effect.Run(SignInEffectPayload.Track(SignInEvents.signedIn(ann))),
        Effect.Run(SignInEffectPayload.NotifyHost(SignInDelegateEvent.Completed("ann", hasOnboarded = true)))))
    runCurrent()
    store.finish()

    val tracked: List<TrackedEvent> = environment.trackArgs
    assertEquals(1, tracked.size)
    assertEquals("Session Signed In", tracked.single().encodedName())
    // The kind, never the address: the property bag is what a vendor sees.
    assertEquals(mapOf<String, Any>("provider" to "email"), tracked.single().encodedProperties())
  }

  @Test
  fun aRefusedSignInReachesNoSink() = runTest {
    val environment = SignInEnvironmentMock()
    environment.signInHandler = { _, onOutcome -> onOutcome(SignInOutcome.Failed("No account for that address.")) }
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
    store.receive(SignInAction.SignInFinished(SignInOutcome.Failed("No account for that address."))) {
      it.copy(isSigningIn = false, pending = null, failure = "No account for that address.")
    }
    runCurrent()
    store.finish()

    assertEquals(0, environment.trackCallCount)
  }
}
