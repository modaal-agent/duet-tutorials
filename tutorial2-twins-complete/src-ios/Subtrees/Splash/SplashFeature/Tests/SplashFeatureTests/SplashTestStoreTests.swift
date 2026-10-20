// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import Duet
import DuetTesting
import SplashFeature
import XCTest

/// The safety net's timing: it fires after its duration and not before,
/// exactly once, and not at all after teardown. The recordings pin what the
/// reducer emits; this suite drives the effect handler through the test store
/// on a `TestClock`, so no test waits on wall time.
@MainActor
final class SplashTestStoreTests: XCTestCase {
  func testSafetyNetDoesNotFireBeforeItsDuration() async {
    let clock = TestClock()
    let environment = RecordingSplashEnvironment(clock: clock)
    let store = armedStore(environment)

    await clock.advance(byNanoseconds: nanos(SplashConfig.safetyNetMillis - 1))

    // Teardown first: a net still sleeping at `finish()` would count as an
    // effect left in flight. Then `finish()` fails on any action that arrived
    // and was never received.
    store.teardown()
    await store.finish()
    XCTAssertEqual(environment.notified, [])
  }

  func testSafetyNetFiresAfterItsDuration() async {
    let clock = TestClock()
    let environment = RecordingSplashEnvironment(clock: clock)
    let store = armedStore(environment)

    await clock.advance(byNanoseconds: nanos(SplashConfig.safetyNetMillis))

    // `receive`, not `send`: the handler emitted this action, so the store
    // already has it and the test states what arrived.
    await store.receive(.safetyNetElapsed)
    store.expectEffects([notifySafetyNet])
    await store.finish()
    XCTAssertEqual(environment.notified, [.completed(path: .safetyNet)])
  }

  func testSafetyNetFiresExactlyOnce() async {
    let clock = TestClock()
    let environment = RecordingSplashEnvironment(clock: clock)
    let store = armedStore(environment)

    // Two advances across the deadline: a periodic wiring would emit a second
    // `.safetyNetElapsed`, which `finish()` reports as an unreceived action.
    await clock.advance(byNanoseconds: nanos(2_000))
    await clock.advance(byNanoseconds: nanos(3_000))

    await store.receive(.safetyNetElapsed)
    store.expectEffects([notifySafetyNet])
    await store.finish()
    XCTAssertEqual(environment.notified.count, 1)
  }

  func testTeardownCancelsTheArmedNet() async {
    let clock = TestClock()
    let environment = RecordingSplashEnvironment(clock: clock)
    let store = armedStore(environment)

    store.teardown()
    await clock.advance(byNanoseconds: nanos(SplashConfig.safetyNetMillis * 3))

    await store.finish()
    XCTAssertEqual(environment.notified, [])
  }

  /// Every test's given: a store whose splash has appeared, so the net is armed.
  private func armedStore(
    _ environment: SplashEnvironment
  ) -> TestStore<SplashState, SplashAction, SplashEffectPayload> {
    let store = TestStore(
      initialState: SplashState(),
      reducer: splashReducer,
      handler: splashEffectHandler(environment: environment))
    store.send(.appeared) { $0.isArmed = true }
    store.expectEffects([
      .run(
        .armSafetyNet(afterMillis: SplashConfig.safetyNetMillis),
        id: SplashEffectIds.safetyNet)
    ])
    return store
  }
}

private let notifySafetyNet: Effect<SplashEffectPayload> =
  .run(.notifyListener(.completed(path: .safetyNet)))

/// Milliseconds on the clock's nanosecond scale.
func nanos(_ millis: Int64) -> UInt64 {
  UInt64(millis) * 1_000_000
}
