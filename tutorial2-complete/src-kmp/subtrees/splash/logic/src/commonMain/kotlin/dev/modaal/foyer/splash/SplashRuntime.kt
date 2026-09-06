// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.splash

import dev.modaal.duet.kernel.Store
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow

// The feature's runtime half: the effect handler, which is the only impure
// code in the module, and the two factories a shell calls. Both apps build the
// same store from `makeSplashStore`.

/**
 * Turns each effect payload into the actions it produces. `ArmSafetyNet`
 * sleeps on the environment's clock and emits `SafetyNetElapsed`; when the
 * store cancels the effect (a re-arm under the same id, or teardown) the sleep
 * throws and the flow ends without emitting. `NotifyHost` calls the
 * environment and emits nothing.
 */
fun splashEffectHandler(
  environment: SplashEnvironment,
): (SplashEffectPayload) -> Flow<SplashAction> = { payload ->
  flow {
    when (payload) {
      is SplashEffectPayload.ArmSafetyNet -> {
        environment.clock.sleep(payload.afterMillis * NANOS_PER_MILLI)
        emit(SplashAction.SafetyNetElapsed)
      }
      is SplashEffectPayload.NotifyHost -> environment.notifyHost(payload.event)
    }
  }
}

private const val NANOS_PER_MILLI = 1_000_000L

/** The store every shell hosts: the reducer and the handler, wired. */
fun makeSplashStore(
  environment: SplashEnvironment,
  scope: CoroutineScope,
): Store<SplashState, SplashAction, SplashEffectPayload> =
  Store(
    initialState = SplashState(),
    reducer = ::splashReducer,
    handler = splashEffectHandler(environment),
    scope = scope,
  )

/**
 * [Store.state] in a concrete-typed position, so the Apple boundary exports
 * it as a typed `StateFlow<SplashState>` for the SwiftUI shell.
 */
fun splashStateFlow(
  store: Store<SplashState, SplashAction, SplashEffectPayload>,
): StateFlow<SplashState> = store.state
