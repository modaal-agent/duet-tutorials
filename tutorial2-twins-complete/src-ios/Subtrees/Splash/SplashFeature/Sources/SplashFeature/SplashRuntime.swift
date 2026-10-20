// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import Duet

// The feature's runtime half: the effect handler, which is the only impure
// code in the package, and the factory the iPhone shell calls. The Android
// app builds its store from the Kotlin twin's `makeSplashStore`.

/// Turns each effect payload into the actions it produces. `.armSafetyNet`
/// sleeps on the environment's clock and emits `.safetyNetElapsed`; when the
/// store cancels the effect (a re-arm under the same id, or teardown) the
/// sleep throws and the stream ends without emitting. `.notifyListener` calls
/// the environment and emits nothing.
public func splashEffectHandler(
  environment: SplashEnvironment
) -> (SplashEffectPayload) -> AsyncStream<SplashAction> {
  { payload in
    switch payload {
    case let .armSafetyNet(afterMillis):
      let clock = environment.clock
      return AsyncStream { continuation in
        let task = Task {
          do {
            try await clock.sleep(nanoseconds: UInt64(afterMillis) * nanosPerMilli)
            continuation.yield(.safetyNetElapsed)
          } catch {
            // Cancelled: the store re-armed under the same id or tore down.
          }
          continuation.finish()
        }
        continuation.onTermination = { _ in task.cancel() }
      }

    case let .notifyListener(event):
      return AsyncStream { continuation in
        environment.notifyHost(event)
        continuation.finish()
      }
    }
  }
}

private let nanosPerMilli: UInt64 = 1_000_000

/// The store the iPhone shell hosts: the reducer and the handler, wired.
@MainActor
public func makeSplashStore(
  environment: SplashEnvironment
) -> Store<SplashState, SplashAction, SplashEffectPayload> {
  Store(
    initialState: SplashState(),
    reducer: splashReducer,
    handler: splashEffectHandler(environment: environment))
}
