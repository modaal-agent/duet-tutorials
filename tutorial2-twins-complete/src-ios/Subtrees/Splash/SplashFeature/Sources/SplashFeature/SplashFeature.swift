// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import Duet

// The splash feature: state, actions, effect payloads and the reducer, in one
// file, in the same order as its Kotlin twin. Feature spec:
// parity/feature-specs/splash.md. Recordings: parity/fixtures/splash.*.

// MARK: - Configuration

public enum SplashConfig {
  /// How long the safety net waits before it completes the splash on the
  /// ceremony's behalf. The reducer carries the value out in the effect
  /// payload, so the recordings pin it.
  public static let safetyNetMillis: Int64 = 3_000
}

public enum SplashEffectIds {
  /// The safety net's effect id. One net is in flight per store: a second
  /// `.run` under the same id cancels the first, and the store's teardown
  /// cancels it outright.
  public static let safetyNet: EffectID = "splash.safetyNet"
}

// MARK: - State

public struct SplashState: Equatable, Codable, Sendable {
  /// The arming latch: the safety net is armed once per mount.
  public var isArmed: Bool

  public init(isArmed: Bool = false) {
    self.isArmed = isArmed
  }
}

// MARK: - Actions

public enum SplashAction: Equatable, Sendable, CanonicalSumCodable {
  /// Shell report: the splash is on screen and the safety net should arm.
  case appeared
  /// Shell report: the splash animation reached its end.
  case ceremonyFinished
  /// Environment report: the armed delay elapsed.
  case safetyNetElapsed
}

// MARK: - Delegate events

/// Which of the two paths completed the splash.
public enum SplashCompletionPath: Equatable, Sendable, CanonicalSumCodable {
  case ceremony
  case safetyNet
}

/// What the splash tells its host. The host decides what happens next.
public enum SplashDelegateEvent: Equatable, Sendable, CanonicalSumCodable {
  case completed(path: SplashCompletionPath)
}

// MARK: - Effect payloads

public enum SplashEffectPayload: Equatable, Sendable, CanonicalSumCodable {
  /// Wait `afterMillis` on the environment's clock, then report
  /// `.safetyNetElapsed`. Runs under `SplashEffectIds.safetyNet`.
  case armSafetyNet(afterMillis: Int64)
  /// Hand a delegate event to the host.
  case notifyListener(SplashDelegateEvent)
}

// MARK: - Reducer

/// The whole behavior. `.appeared` arms the safety net once; each completion
/// path notifies the host every time it fires, including after the other path
/// already has. The host treats a repeat `.completed` as a no-op, so the
/// reducer keeps no completion latch and `.ceremonyFinished` does not cancel
/// the net.
public func splashReducer(
  state: inout SplashState,
  action: SplashAction
) -> [Effect<SplashEffectPayload>] {
  switch action {
  case .appeared:
    // The arming guard: a repeat `.appeared` writes nothing and emits nothing.
    guard !state.isArmed else { return [] }
    state.isArmed = true
    return [
      .run(
        .armSafetyNet(afterMillis: SplashConfig.safetyNetMillis),
        id: SplashEffectIds.safetyNet)
    ]

  case .ceremonyFinished:
    return [.run(.notifyListener(.completed(path: .ceremony)))]

  case .safetyNetElapsed:
    return [.run(.notifyListener(.completed(path: .safetyNet)))]
  }
}
