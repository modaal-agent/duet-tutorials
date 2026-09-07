// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.root

import dev.modaal.duet.kernel.Reduced
import dev.modaal.foyer.main.MainDelegateEvent
import dev.modaal.foyer.ports.Entitlement
import dev.modaal.foyer.signin.SignInDelegateEvent
import dev.modaal.foyer.splash.SplashDelegateEvent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// The root level, the app's spine: a phase that mounts exactly one child, an
// auth snapshot, the entitlement, and a latch for a splash that finishes
// before auth is known. Every arrow into this level is a child's delegate
// event received as an action, or a worker's report of a stream value.
// Feature spec: parity/feature-specs/root.md. Recordings:
// parity/fixtures/root.*.

// MARK: - State

/** Which child the root mounts. Tutorial 5 adds `Onboarding`. */
@Serializable(with = RootPhaseSerializer::class)
sealed interface RootPhase {
  @Serializable @SerialName("splash") data object Splash : RootPhase

  @Serializable @SerialName("signIn") data object SignIn : RootPhase

  @Serializable @SerialName("main") data object Main : RootPhase
}

/** What the root knows about the session. The session worker reports every change. */
@Serializable(with = AuthSnapshotSerializer::class)
sealed interface AuthSnapshot {
  @Serializable @SerialName("unknown") data object Unknown : AuthSnapshot

  @Serializable @SerialName("signedOut") data object SignedOut : AuthSnapshot

  @Serializable @SerialName("signedIn") data class SignedIn(val displayName: String) : AuthSnapshot
}

@Serializable
data class RootState(
  val phase: RootPhase = RootPhase.Splash,
  val auth: AuthSnapshot = AuthSnapshot.Unknown,
  /** The splash finished before auth was known; the phase moves on `AuthChanged`. */
  val awaitingAuth: Boolean = false,
  /** The paid check's one value. The entitlement worker is its only writer; `home` and `profile` read it as a slice. */
  val entitlement: Entitlement = Entitlement.Free,
)

// MARK: - Actions

@Serializable(with = RootActionSerializer::class)
sealed interface RootAction {
  /** The splash's delegate events, received as this level's actions. */
  @Serializable @SerialName("splash") data class Splash(val event: SplashDelegateEvent) : RootAction

  /** The sign-in gate's delegate events. */
  @Serializable @SerialName("signIn") data class SignIn(val event: SignInDelegateEvent) : RootAction

  /** The main level's delegate events. */
  @Serializable @SerialName("main") data class Main(val event: MainDelegateEvent) : RootAction

  /** The session worker's report: the auth port's stream emitted. */
  @Serializable @SerialName("authChanged") data class AuthChanged(val auth: AuthSnapshot) : RootAction

  /** The entitlement worker's report: the purchases port's stream emitted. */
  @Serializable
  @SerialName("entitlementChanged")
  data class EntitlementChanged(val entitlement: Entitlement) : RootAction
}

// MARK: - Effect payloads

/**
 * The root does no work of its own yet: it routes. Tutorial 5 adds the
 * deep-link forward. The type exists so the root has the kernel's full shape
 * and replays like every other feature.
 */
@Serializable(with = RootEffectPayloadSerializer::class) sealed interface RootEffectPayload

// MARK: - Reducer

/**
 * The splash's `Completed` moves to the gate or to main, by the auth snapshot;
 * when auth is still unknown it sets the latch instead, and `AuthChanged`
 * releases it. A `Completed` after the splash phase is inert: both splash
 * paths notify, and only the first one moves the app. The gate's `Completed`
 * signs the session in; a `SignOutRequested` climbing from anywhere under
 * main raises the gate, and so does a session ending while main is up (the
 * guest expiry). `EntitlementChanged` writes the one entitlement value.
 */
fun rootReducer(state: RootState, action: RootAction): Reduced<RootState, RootEffectPayload> =
  when (action) {
    is RootAction.Splash ->
      when {
        state.phase != RootPhase.Splash -> Reduced(state)
        state.auth == AuthSnapshot.Unknown -> Reduced(state.copy(awaitingAuth = true))
        else -> Reduced(state.copy(phase = phaseAfterSplash(state.auth)))
      }

    is RootAction.AuthChanged -> {
      val next = state.copy(auth = action.auth)
      when {
        next.awaitingAuth && action.auth != AuthSnapshot.Unknown ->
          Reduced(next.copy(phase = phaseAfterSplash(action.auth), awaitingAuth = false))
        state.phase == RootPhase.Main && action.auth == AuthSnapshot.SignedOut ->
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
            Reduced(
              state.copy(phase = RootPhase.Main, auth = AuthSnapshot.SignedIn(event.displayName)))
          }
      }

    is RootAction.Main ->
      when (action.event) {
        MainDelegateEvent.SignOutRequested ->
          Reduced(state.copy(phase = RootPhase.SignIn, auth = AuthSnapshot.SignedOut))
      }
  }

/** Where a finished splash goes: the gate when signed out, main when signed in. */
private fun phaseAfterSplash(auth: AuthSnapshot): RootPhase =
  if (auth is AuthSnapshot.SignedIn) RootPhase.Main else RootPhase.SignIn
