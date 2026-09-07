// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.root

import dev.modaal.duet.kernel.Effect
import dev.modaal.duet.kernel.Reduced
import dev.modaal.foyer.main.MainDelegateEvent
import dev.modaal.foyer.onboarding.OnboardingDelegateEvent
import dev.modaal.foyer.ports.DeepLink
import dev.modaal.foyer.ports.Entitlement
import dev.modaal.foyer.signin.SignInDelegateEvent
import dev.modaal.foyer.splash.SplashDelegateEvent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// The root level, the app's spine: a phase that mounts exactly one child, an
// auth snapshot, the entitlement, a latch for a splash that finishes before
// auth is known, and a held deep link. Every arrow into this level is a
// child's delegate event received as an action, a worker's report of a
// stream value, or a link the shell parsed. Feature spec:
// parity/feature-specs/root.md. Recordings: parity/fixtures/root.*.

// MARK: - State

/** Which child the root mounts. Two gates sit between the splash and main. */
@Serializable(with = RootPhaseSerializer::class)
sealed interface RootPhase {
  @Serializable @SerialName("splash") data object Splash : RootPhase

  @Serializable @SerialName("signIn") data object SignIn : RootPhase

  @Serializable @SerialName("onboarding") data object Onboarding : RootPhase

  @Serializable @SerialName("main") data object Main : RootPhase
}

/** What the root knows about the session. The session worker reports every change. */
@Serializable(with = AuthSnapshotSerializer::class)
sealed interface AuthSnapshot {
  @Serializable @SerialName("unknown") data object Unknown : AuthSnapshot

  @Serializable @SerialName("signedOut") data object SignedOut : AuthSnapshot

  /** `hasOnboarded` is the backend's word on this account; the onboarding gate reads it. */
  @Serializable
  @SerialName("signedIn")
  data class SignedIn(val displayName: String, val hasOnboarded: Boolean) : AuthSnapshot
}

@Serializable
data class RootState(
  val phase: RootPhase = RootPhase.Splash,
  val auth: AuthSnapshot = AuthSnapshot.Unknown,
  /** The splash finished before auth was known; the phase moves on `AuthChanged`. */
  val awaitingAuth: Boolean = false,
  /** The paid check's one value. The entitlement worker is its only writer; `home` and `profile` read it as a slice. */
  val entitlement: Entitlement = Entitlement.Free,
  /** A link that arrived before main was up. Released as one `ForwardLink` when main mounts. */
  val pendingLink: DeepLink? = null,
)

// MARK: - Actions

@Serializable(with = RootActionSerializer::class)
sealed interface RootAction {
  /** The splash's delegate events, received as this level's actions. */
  @Serializable @SerialName("splash") data class Splash(val event: SplashDelegateEvent) : RootAction

  /** The sign-in gate's delegate events. */
  @Serializable @SerialName("signIn") data class SignIn(val event: SignInDelegateEvent) : RootAction

  /** The onboarding gate's delegate events. */
  @Serializable
  @SerialName("onboarding")
  data class Onboarding(val event: OnboardingDelegateEvent) : RootAction

  /** The main level's delegate events. */
  @Serializable @SerialName("main") data class Main(val event: MainDelegateEvent) : RootAction

  /** The session worker's report: the auth port's stream emitted. */
  @Serializable @SerialName("authChanged") data class AuthChanged(val auth: AuthSnapshot) : RootAction

  /** The entitlement worker's report: the purchases port's stream emitted. */
  @Serializable
  @SerialName("entitlementChanged")
  data class EntitlementChanged(val entitlement: Entitlement) : RootAction

  /** Shell report: the operating system handed the app a link it answers. */
  @Serializable @SerialName("deepLink") data class DeepLink(val link: dev.modaal.foyer.ports.DeepLink) : RootAction
}

// MARK: - Effect payloads

@Serializable(with = RootEffectPayloadSerializer::class)
sealed interface RootEffectPayload {
  /** Hand a link down to main; the shell bridges it into `Main.OpenLink`. */
  @Serializable @SerialName("forwardLink") data class ForwardLink(val link: DeepLink) : RootEffectPayload

  /** Persist the onboarding answers through the account port. Void: the session stream carries the result. */
  @Serializable
  @SerialName("completeOnboarding")
  data class CompleteOnboarding(val name: String, val preferences: List<String>) : RootEffectPayload
}

// MARK: - Reducer

/**
 * The splash's `Completed` moves to a gate or to main, by the auth snapshot;
 * when auth is still unknown it sets the latch instead, and `AuthChanged`
 * releases it. A `Completed` after the splash phase is inert. The sign-in
 * gate's `Completed` signs the session in and mounts main or the onboarding
 * gate; the onboarding gate's `Completed` persists the answers and mounts
 * main. A `SignOutRequested` climbing from anywhere under main raises the
 * sign-in gate, and so does a session ending while a signed-in phase is up.
 * `EntitlementChanged` writes the one entitlement value. A deep link is
 * forwarded when main is up and held until main mounts otherwise.
 */
fun rootReducer(state: RootState, action: RootAction): Reduced<RootState, RootEffectPayload> =
  when (action) {
    is RootAction.Splash ->
      when {
        state.phase != RootPhase.Splash -> Reduced(state)
        state.auth == AuthSnapshot.Unknown -> Reduced(state.copy(awaitingAuth = true))
        else -> enter(state, phaseAfterSplash(state.auth))
      }

    is RootAction.AuthChanged -> {
      val next = state.copy(auth = action.auth)
      when {
        next.awaitingAuth && action.auth != AuthSnapshot.Unknown ->
          enter(next.copy(awaitingAuth = false), phaseAfterSplash(action.auth))
        state.phase.isSignedIn && action.auth == AuthSnapshot.SignedOut ->
          Reduced(next.copy(phase = RootPhase.SignIn))
        else -> Reduced(next)
      }
    }

    is RootAction.EntitlementChanged -> Reduced(state.copy(entitlement = action.entitlement))

    is RootAction.SignIn ->
      when (val event = action.event) {
        is SignInDelegateEvent.Completed ->
          if (state.phase != RootPhase.SignIn) {
            Reduced(state)
          } else {
            val auth = AuthSnapshot.SignedIn(event.displayName, event.hasOnboarded)
            enter(state.copy(auth = auth), phaseAfterSplash(auth))
          }
      }

    is RootAction.Onboarding ->
      when (val event = action.event) {
        is OnboardingDelegateEvent.Completed ->
          if (state.phase != RootPhase.Onboarding) {
            Reduced(state)
          } else {
            val entered =
              enter(
                state.copy(auth = AuthSnapshot.SignedIn(event.name, hasOnboarded = true)),
                RootPhase.Main)
            Reduced(
              entered.state,
              listOf(Effect.Run(RootEffectPayload.CompleteOnboarding(event.name, event.preferences))) +
                entered.effects)
          }
      }

    is RootAction.Main ->
      when (action.event) {
        MainDelegateEvent.SignOutRequested ->
          Reduced(state.copy(phase = RootPhase.SignIn, auth = AuthSnapshot.SignedOut))
      }

    is RootAction.DeepLink ->
      if (state.phase == RootPhase.Main) {
        Reduced(state, listOf(Effect.Run(RootEffectPayload.ForwardLink(action.link))))
      } else {
        Reduced(state.copy(pendingLink = action.link))
      }
  }

/** Where a finished splash goes: the sign-in gate, the onboarding gate, or main. */
private fun phaseAfterSplash(auth: AuthSnapshot): RootPhase =
  when (auth) {
    is AuthSnapshot.SignedIn -> if (auth.hasOnboarded) RootPhase.Main else RootPhase.Onboarding
    AuthSnapshot.SignedOut,
    AuthSnapshot.Unknown -> RootPhase.SignIn
  }

/** Move to a phase; entering main releases a held link as one forward. */
private fun enter(state: RootState, phase: RootPhase): Reduced<RootState, RootEffectPayload> {
  val link = state.pendingLink
  return if (phase == RootPhase.Main && link != null) {
    Reduced(
      state.copy(phase = phase, pendingLink = null),
      listOf(Effect.Run(RootEffectPayload.ForwardLink(link))))
  } else {
    Reduced(state.copy(phase = phase))
  }
}

/** The phases a session must be signed in for; a session ending under them raises the gate. */
private val RootPhase.isSignedIn: Boolean
  get() = this == RootPhase.Main || this == RootPhase.Onboarding
