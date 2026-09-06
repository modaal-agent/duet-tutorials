// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.root

import dev.modaal.duet.test.*
import dev.modaal.foyer.main.MainDelegateEvent
import dev.modaal.foyer.signin.SignInDelegateEvent
import dev.modaal.foyer.splash.SplashCompletionPath
import dev.modaal.foyer.splash.SplashDelegateEvent
import kotlin.test.Test

/**
 * The scenario the root recordings are compiled from: five branches over a
 * fresh root whose auth is unknown. The root emits no effects; every branch
 * pins state alone.
 */
class RootScenarioTest {
  @Test
  fun rootScenario() {
    val s =
      scenario<RootState, RootAction, RootEffectPayload>(
        feature = "root",
        description =
          "The root mounts one child from its phase. A finished splash goes to the " +
            "gate or to main by the auth snapshot, or waits for it; the gate's " +
            "completion signs the session in; a sign-out request from under main " +
            "raises the gate; a late splash completion is inert.",
        source =
          "src-kmp/subtrees/root/logic/src/jvmTest/kotlin/" +
            "dev/modaal/foyer/root/RootScenarioTest.kt",
      ) {
        given(RootState())

        branch("splash before auth holds") {
          whenAction("the splash completes while auth is unknown", splashCompleted)
          then("the latch is set, the phase stays") {
            it.awaitingAuth && it.phase == RootPhase.Splash
          }
          whenAction("the host reports a signed-out session", signedOut)
          then("the latch releases into the gate") {
            !it.awaitingAuth && it.phase == RootPhase.SignIn && it.auth == AuthSnapshot.SignedOut
          }
        }

        branch("gate after splash") {
          whenAction("the host reports a signed-out session", signedOut)
          then("still on the splash") { it.phase == RootPhase.Splash && !it.awaitingAuth }
          whenAction("the splash completes", splashCompleted)
          then("the gate is up") { it.phase == RootPhase.SignIn }
          whenAction(
            "the gate completes", RootAction.SignIn(SignInDelegateEvent.Completed("ann")))
          then("main is up, the session signed in") {
            it.phase == RootPhase.Main && it.auth == AuthSnapshot.SignedIn("ann")
          }
        }

        branch("late splash inert") {
          whenAction("the host reports a signed-out session", signedOut)
          whenAction("the ceremony completes the splash", splashCompleted)
          whenAction(
            "the safety net completes it again",
            RootAction.Splash(SplashDelegateEvent.Completed(SplashCompletionPath.SafetyNet)))
          then("nothing changed: the second completion is inert") {
            it.phase == RootPhase.SignIn && !it.awaitingAuth
          }
        }

        branch("signed in skips gate") {
          whenAction(
            "the host reports a signed-in session",
            RootAction.AuthChanged(AuthSnapshot.SignedIn("ann")))
          whenAction("the splash completes", splashCompleted)
          then("straight to main") { it.phase == RootPhase.Main }
        }

        branch("sign out returns to gate") {
          whenAction(
            "the host reports a signed-in session",
            RootAction.AuthChanged(AuthSnapshot.SignedIn("ann")))
          whenAction("the splash completes", splashCompleted)
          whenAction(
            "a sign-out request climbs from under main",
            RootAction.Main(MainDelegateEvent.SignOutRequested))
          then("the gate is up, the session signed out") {
            it.phase == RootPhase.SignIn && it.auth == AuthSnapshot.SignedOut
          }
        }
      }

    ScenarioRunner.verifyOrRecord(
      s, RootState.serializer(), RootActionSerializer, RootEffectPayloadSerializer, ::rootReducer)
  }
}

private val splashCompleted =
  RootAction.Splash(SplashDelegateEvent.Completed(SplashCompletionPath.Ceremony))
private val signedOut = RootAction.AuthChanged(AuthSnapshot.SignedOut)
