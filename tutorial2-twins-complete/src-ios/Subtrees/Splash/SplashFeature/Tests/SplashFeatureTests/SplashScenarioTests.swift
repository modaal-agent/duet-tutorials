// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import Duet
import DuetTesting
import SplashFeature
import XCTest

/// The splash's behavior as a Swift scenario: the same Given, the same steps
/// and the same four branches as the Kotlin twin's scenario, with the same
/// labels, so both name the same recordings. This scenario is the writer:
/// `tools/duet record` runs it with regeneration on and it writes one fixture
/// per branch under parity/fixtures/. Every other run checks the committed
/// fixtures against it.
private let splashScenario = Scenario<SplashState, SplashAction, SplashEffectPayload>(
  feature: "splash",
  description:
    "The splash arms a safety net when it appears and completes by whichever "
    + "path gets there first: the animation ending, or the net firing. Both "
    + "paths notify the host every time; a repeat Appeared is inert."
) {
  Given(SplashState())

  When("the splash appears", .appeared)
  Then("the safety net is armed") { $0.isArmed }
  ThenEffects("exactly the keyed safety net, carrying its duration") {
    $0 == [
      .run(
        .armSafetyNet(afterMillis: SplashConfig.safetyNetMillis),
        id: SplashEffectIds.safetyNet)
    ]
  }

  Branch("ceremony completes") {
    When("the animation reaches its end", .ceremonyFinished)
    Then("the arming latch is untouched") { $0.isArmed }
    ThenEffects("the host is notified, by the ceremony path") { $0 == notified(.ceremony) }
  }

  Branch("safety net fires") {
    When("the armed delay elapses instead", .safetyNetElapsed)
    Then("the arming latch is untouched") { $0.isArmed }
    ThenEffects("the host is notified, by the safety-net path") { $0 == notified(.safetyNet) }
  }

  Branch("both paths notify twice") {
    When("the animation reaches its end", .ceremonyFinished)
    ThenEffects("the host is notified, by the ceremony path") { $0 == notified(.ceremony) }
    When("the armed delay elapses on top of it", .safetyNetElapsed)
    ThenEffects("the host is notified again, by the safety-net path") {
      $0 == notified(.safetyNet)
    }
  }

  Branch("repeat appear inert") {
    When("the splash reports appearing a second time", .appeared)
    Then("still armed, unchanged") { $0.isArmed }
    ThenEffects("nothing: the net is not re-armed") { $0.isEmpty }
  }
}

/// The one delegate effect, by path: what every completion branch expects.
private func notified(_ path: SplashCompletionPath) -> [Effect<SplashEffectPayload>] {
  [.run(.notifyListener(.completed(path: path)))]
}

final class SplashScenarioTests: XCTestCase {
  func testSplashScenario() throws {
    try ScenarioRunner.verifyOrRecord(splashScenario, reducer: splashReducer)
  }
}
