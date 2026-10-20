// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import Duet
import DuetTesting
import SplashFeature
import XCTest

/// The mount bracket: a shell builds a store when the splash is mounted and
/// tears it down when the splash leaves; a second mount builds a second store.
/// The arming latch is per store, so the second mount arms a fresh net, and
/// the torn-down store's net never fires. The Swift counterpart of the Kotlin
/// twin's `SplashMountBracketTest`.
@MainActor
final class SplashMountBracketTests: XCTestCase {
  func testASecondMountArmsAFreshNet() async {
    let clock = TestClock()
    let environment = RecordingSplashEnvironment(clock: clock)
    let first = TestStore(
      initialState: SplashState(),
      reducer: splashReducer,
      handler: splashEffectHandler(environment: environment))
    first.send(.appeared) { $0.isArmed = true }
    first.expectEffects([armSafetyNet])
    first.teardown()

    // The second mount, over the same environment: a fresh store, a fresh latch.
    let second = TestStore(
      initialState: SplashState(),
      reducer: splashReducer,
      handler: splashEffectHandler(environment: environment))
    second.send(.appeared) { $0.isArmed = true }
    second.expectEffects([armSafetyNet])

    await clock.advance(byNanoseconds: nanos(SplashConfig.safetyNetMillis))

    // One net fired: the second store's. The first store's was cancelled at
    // teardown, and `finish()` on the first store fails on any action that
    // arrived there.
    await second.receive(.safetyNetElapsed)
    second.expectEffects([.run(.notifyListener(.completed(path: .safetyNet)))])
    await first.finish()
    await second.finish()
    XCTAssertEqual(environment.notified, [.completed(path: .safetyNet)])
  }
}

private let armSafetyNet: Effect<SplashEffectPayload> =
  .run(
    .armSafetyNet(afterMillis: SplashConfig.safetyNetMillis),
    id: SplashEffectIds.safetyNet)
