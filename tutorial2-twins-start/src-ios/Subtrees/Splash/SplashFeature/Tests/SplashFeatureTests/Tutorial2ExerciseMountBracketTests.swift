// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import Duet
import DuetTesting
import Foundation
import SplashFeature
import XCTest

/// Tutorial 2's closing exercise: the mount bracket. A shell builds a store
/// when the splash is mounted and tears it down when the splash leaves; a
/// second mount builds a second store. This test fails until you finish it.
///
/// The given is written: one store, armed, then torn down. Finish the test so
/// it pins that a second store over the same environment arms a fresh net, and
/// that after advancing the clock past the duration the host is notified
/// exactly once — the torn-down store's net never fires.
@MainActor
final class Tutorial2ExerciseMountBracketTests: XCTestCase {
  func testASecondMountArmsAFreshNet() async throws {
    // The repository's CI sets TUTORIAL_SKIP_STUBS=1 to leave the exercise
    // out; a bare `tools/duet verify` runs it. Delete this line when you
    // finish the test.
    try XCTSkipIf(
      ProcessInfo.processInfo.environment["TUTORIAL_SKIP_STUBS"] == "1",
      "the closing exercise; unset TUTORIAL_SKIP_STUBS to run it")

    let clock = TestClock()
    let environment = SplashEnvironmentMock(clock: clock)
    let first = TestStore(
      initialState: SplashState(),
      reducer: splashReducer,
      handler: splashEffectHandler(environment: environment))
    first.send(.appeared) { $0.isArmed = true }
    first.expectEffects([
      .run(
        .armSafetyNet(afterMillis: SplashConfig.safetyNetMillis),
        id: SplashEffectIds.safetyNet)
    ])
    first.teardown()

    XCTFail("Tutorial 2 exercise: mount a second store and pin the fresh net")
  }
}
