// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.splash

import dev.modaal.duet.kernel.Effect
import dev.modaal.duet.kernel.EffectId
import dev.modaal.duet.kernel.Reduced
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// The splash feature: state, actions, effect payloads and the reducer, in one
// file. Feature spec: parity/feature-specs/splash.md. Recordings:
// parity/fixtures/splash.*.

// MARK: - Configuration

object SplashConfig {
  /**
   * How long the safety net waits before it completes the splash on the
   * ceremony's behalf. The reducer carries the value out in the effect
   * payload, so the recordings pin it.
   */
  const val SAFETY_NET_MILLIS: Long = 3_000L
}

object SplashEffectIds {
  /**
   * The safety net's effect id. One net is in flight per store: a second
   * `Run` under the same id would cancel the first, and the store's teardown
   * cancels it outright.
   */
  const val SAFETY_NET: EffectId = "splash.safetyNet"
}

// MARK: - State

@Serializable
data class SplashState(
  /** The arming latch: the safety net is armed once per mount. */
  val isArmed: Boolean = false,
)

// MARK: - Actions

@Serializable(with = SplashActionSerializer::class)
sealed interface SplashAction {
  /** Shell report: the splash is on screen and the safety net should arm. */
  @Serializable @SerialName("appeared") data object Appeared : SplashAction

  /** Shell report: the splash animation reached its end. */
  @Serializable
  @SerialName("ceremonyFinished")
  data object CeremonyFinished : SplashAction

  /** Environment report: the armed delay elapsed. */
  @Serializable
  @SerialName("safetyNetElapsed")
  data object SafetyNetElapsed : SplashAction
}

// MARK: - Delegate events

/** Which of the two paths completed the splash. */
@Serializable(with = SplashCompletionPathSerializer::class)
sealed interface SplashCompletionPath {
  @Serializable @SerialName("ceremony") data object Ceremony : SplashCompletionPath

  @Serializable @SerialName("safetyNet") data object SafetyNet : SplashCompletionPath
}

/** What the splash tells its host. The host decides what happens next. */
@Serializable(with = SplashDelegateEventSerializer::class)
sealed interface SplashDelegateEvent {
  @Serializable
  @SerialName("completed")
  data class Completed(val path: SplashCompletionPath) : SplashDelegateEvent
}

// MARK: - Effect payloads

@Serializable(with = SplashEffectPayloadSerializer::class)
sealed interface SplashEffectPayload {
  /**
   * Wait `afterMillis` on the environment's clock, then report
   * `SafetyNetElapsed`. Runs under `SplashEffectIds.SAFETY_NET`.
   */
  @Serializable
  @SerialName("armSafetyNet")
  data class ArmSafetyNet(val afterMillis: Long) : SplashEffectPayload

  /** Hand a delegate event to the host. */
  @Serializable
  @SerialName("notifyListener")
  data class NotifyHost(val event: SplashDelegateEvent) : SplashEffectPayload
}

// MARK: - Reducer

/**
 * The whole behavior. `Appeared` arms the safety net once; each completion
 * path notifies the host every time it fires, including after the other path
 * already has. The host treats a repeat `Completed` as a no-op, so the reducer
 * keeps no completion latch and `CeremonyFinished` does not cancel the net.
 */
fun splashReducer(
  state: SplashState,
  action: SplashAction,
): Reduced<SplashState, SplashEffectPayload> =
  when (action) {
    SplashAction.Appeared ->
      if (state.isArmed) {
        // The arming guard: a repeat `Appeared` writes nothing and emits nothing.
        Reduced(state)
      } else {
        Reduced(
          state.copy(isArmed = true),
          listOf(
            Effect.Run(
              SplashEffectPayload.ArmSafetyNet(SplashConfig.SAFETY_NET_MILLIS),
              id = SplashEffectIds.SAFETY_NET)))
      }

    SplashAction.CeremonyFinished ->
      Reduced(
        state,
        listOf(
          Effect.Run(
            SplashEffectPayload.NotifyHost(
              SplashDelegateEvent.Completed(SplashCompletionPath.Ceremony)))))

    SplashAction.SafetyNetElapsed ->
      Reduced(
        state,
        listOf(
          Effect.Run(
            SplashEffectPayload.NotifyHost(
              SplashDelegateEvent.Completed(SplashCompletionPath.SafetyNet)))))
  }
