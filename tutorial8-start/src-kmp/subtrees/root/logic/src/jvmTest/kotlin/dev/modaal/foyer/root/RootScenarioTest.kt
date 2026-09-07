// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.root

import dev.modaal.duet.kernel.Effect
import dev.modaal.duet.test.*
import dev.modaal.foyer.main.MainDelegateEvent
import dev.modaal.foyer.onboarding.OnboardingDelegateEvent
import dev.modaal.foyer.ports.DeepLink
import dev.modaal.foyer.ports.Entitlement
import dev.modaal.foyer.ports.Plan
import dev.modaal.foyer.signin.SignInDelegateEvent
import dev.modaal.foyer.splash.SplashCompletionPath
import dev.modaal.foyer.splash.SplashDelegateEvent
import kotlin.test.Test

/**
 * The scenario the root recordings are compiled from: twelve branches over a
 * fresh root whose auth is unknown.
 */
class RootScenarioTest {
  @Test
  fun rootScenario() {
    val s =
      scenario<RootState, RootAction, RootEffectPayload>(
        feature = "root",
        description =
          "The root mounts one child from its phase. A finished splash goes to the " +
            "sign-in gate, the onboarding gate or main by the auth snapshot, or waits " +
            "for it; the sign-in gate's completion signs the session in and picks " +
            "the onboarding gate for an account that has not onboarded; the " +
            "onboarding gate's completion persists the answers and mounts main; a " +
            "sign-out request from under main raises the sign-in gate, and so does a " +
            "session ending under a signed-in phase; a late splash completion is " +
            "inert, and so is a late sign-in completion; the entitlement stream writes one value; a deep link is " +
            "forwarded under main and held until main mounts otherwise.",
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
          whenAction("the gate completes for an onboarded account", gateCompleted(hasOnboarded = true))
          then("main is up, the session signed in") {
            it.phase == RootPhase.Main && it.auth == onboardedAnn
          }
        }

        branch("gate picks onboarding") {
          whenAction("the host reports a signed-out session", signedOut)
          whenAction("the splash completes", splashCompleted)
          whenAction("the gate completes for a new account", gateCompleted(hasOnboarded = false))
          then("the onboarding gate is up") {
            it.phase == RootPhase.Onboarding && it.auth == AuthSnapshot.SignedIn("ann", false)
          }
          thenEffects("nothing") { it.isEmpty() }
        }

        branch("onboarding completes into main") {
          whenAction(
            "the host reports a session that has not onboarded",
            RootAction.AuthChanged(AuthSnapshot.SignedIn("ann", false)))
          whenAction("the splash completes", splashCompleted)
          then("straight to the onboarding gate") { it.phase == RootPhase.Onboarding }
          whenAction(
            "the onboarding gate completes",
            RootAction.Onboarding(OnboardingDelegateEvent.Completed("Ann", listOf("digest"))))
          then("main is up, the snapshot carries the answers") {
            it.phase == RootPhase.Main && it.auth == AuthSnapshot.SignedIn("Ann", true)
          }
          thenEffects("exactly the account port's onboarding write") {
            it ==
              effectsOf<RootEffectPayload>(
                Effect.Run(RootEffectPayload.CompleteOnboarding("Ann", listOf("digest"))))
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

        branch("late sign-in inert") {
          whenAction("the host reports an onboarded session", RootAction.AuthChanged(onboardedAnn))
          whenAction("the splash completes", splashCompleted)
          then("main is up") { it.phase == RootPhase.Main }
          whenAction(
            "a sign-in completion arrives with the gate long gone",
            RootAction.SignIn(SignInDelegateEvent.Completed("mallory", hasOnboarded = false)))
          then("nothing changed: the phase and the session are as they were") {
            it.phase == RootPhase.Main && it.auth == onboardedAnn
          }
          thenEffects("nothing") { it.isEmpty() }
        }

        branch("signed in skips gate") {
          whenAction("the host reports an onboarded session", RootAction.AuthChanged(onboardedAnn))
          whenAction("the splash completes", splashCompleted)
          then("straight to main") { it.phase == RootPhase.Main }
        }

        branch("sign out returns to gate") {
          whenAction("the host reports an onboarded session", RootAction.AuthChanged(onboardedAnn))
          whenAction("the splash completes", splashCompleted)
          whenAction(
            "a sign-out request climbs from under main",
            RootAction.Main(MainDelegateEvent.SignOutRequested))
          then("the gate is up, the session signed out") {
            it.phase == RootPhase.SignIn && it.auth == AuthSnapshot.SignedOut
          }
        }

        branch("session expiry returns to gate") {
          whenAction(
            "the host reports a signed-in guest",
            RootAction.AuthChanged(AuthSnapshot.SignedIn("Guest", true)))
          whenAction("the splash completes", splashCompleted)
          then("main is up") { it.phase == RootPhase.Main }
          whenAction("the session worker reports the session ended", signedOut)
          then("the gate is up, from wherever the user was") {
            it.phase == RootPhase.SignIn && it.auth == AuthSnapshot.SignedOut
          }
        }

        branch("entitlement changes") {
          whenAction(
            "the entitlement worker reports a purchase",
            RootAction.EntitlementChanged(Entitlement.Premium(Plan.Monthly)))
          then("the value is written, nothing else moves") {
            it.entitlement == Entitlement.Premium(Plan.Monthly) && it.phase == RootPhase.Splash
          }
        }

        branch("link forwards under main") {
          whenAction("the host reports an onboarded session", RootAction.AuthChanged(onboardedAnn))
          whenAction("the splash completes", splashCompleted)
          whenAction("the shell hands over the upgrade link", RootAction.DeepLink(DeepLink.Upgrade))
          then("nothing is held") { it.pendingLink == null && it.phase == RootPhase.Main }
          thenEffects("exactly the forward") {
            it == effectsOf<RootEffectPayload>(Effect.Run(RootEffectPayload.ForwardLink(DeepLink.Upgrade)))
          }
        }

        branch("link waits for main") {
          whenAction(
            "the shell hands over the account link during the splash",
            RootAction.DeepLink(DeepLink.ProfileAccount))
          then("held") { it.pendingLink == DeepLink.ProfileAccount }
          thenEffects("nothing yet") { it.isEmpty() }
          whenAction("the host reports an onboarded session", RootAction.AuthChanged(onboardedAnn))
          whenAction("the splash completes", splashCompleted)
          then("main is up, the link released") {
            it.phase == RootPhase.Main && it.pendingLink == null
          }
          thenEffects("exactly the forward, as main mounts") {
            it ==
              effectsOf<RootEffectPayload>(
                Effect.Run(RootEffectPayload.ForwardLink(DeepLink.ProfileAccount)))
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
private val onboardedAnn = AuthSnapshot.SignedIn("ann", hasOnboarded = true)

private fun gateCompleted(hasOnboarded: Boolean) =
  RootAction.SignIn(SignInDelegateEvent.Completed("ann", hasOnboarded))
