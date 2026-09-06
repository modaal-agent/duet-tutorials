// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.root

import dev.modaal.duet.kernel.Reduced
import dev.modaal.foyer.main.MainDelegateEvent
import dev.modaal.foyer.signin.SignInDelegateEvent
import dev.modaal.foyer.splash.SplashDelegateEvent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// The root level, the app's spine: a phase that mounts exactly one child, an
// auth snapshot, and a latch for a splash that finishes before auth is
// known. Every arrow into this level is a child's delegate event received as
// an action. Feature spec: parity/feature-specs/root.md. Recordings:
// parity/fixtures/root.*.

// MARK: - State

/** Which child the root mounts. Tutorial 5 adds `Onboarding`. */
@Serializable(with = RootPhaseSerializer::class)
sealed interface RootPhase {
  @Serializable @SerialName("splash") data object Splash : RootPhase

  @Serializable @SerialName("signIn") data object SignIn : RootPhase

  @Serializable @SerialName("main") data object Main : RootPhase
}

/** What the root knows about the session. Seeded by the host at mount. */
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

  /** Host report: the session changed. Tutorial 4's session worker sends this. */
  @Serializable @SerialName("authChanged") data class AuthChanged(val auth: AuthSnapshot) : RootAction
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
 * main raises the gate.
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
      if (next.awaitingAuth && action.auth != AuthSnapshot.Unknown) {
        Reduced(next.copy(phase = phaseAfterSplash(action.auth), awaitingAuth = false))
      } else {
        Reduced(next)
      }
    }

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
