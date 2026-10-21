// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import Duet
import DuetTesting
import SplashFeature
import XCTest

@testable import SplashNode

// The shells lane's rows for the splash: that an intent reaches the reducer
// and projects back before `send` returns, that the effect loop dispatches
// into the environment, and that `deactivate()` stops the effect loop. The
// reducer's behavior is not tested here; the recordings pin it on both lanes.
//
// Virtual time: the store is Duet's Swift `Store`, so a `TestClock` stands
// where the builder puts the wall clock.

@MainActor
final class SplashViewShellSpec: XCTestCase {

  /// The builder's wiring, end to end: the intent reaches the reducer and the
  /// projection reads back before `send` returns.
  func testTheBuilderComposesTheRuntime() {
    let child = SplashBuilder().buildSplash(onDelegate: { _ in })
    let shell = child.shell
    shell.activate()
    defer { shell.deactivate() }

    XCTAssertFalse(shell.viewState.isArmed)
    shell.appeared()
    XCTAssertTrue(shell.viewState.isArmed, "the projection reads back before send returns")
  }

  /// The effect loop dispatches back out: `ceremonyFinished()` reaches the
  /// reducer, whose `.notifyListener` effect calls the environment with the
  /// ceremony path.
  func testTheCeremonyPathNotifiesTheHost() {
    let environment = SplashEnvironmentMock(clock: TestClock())
    let shell = SplashViewShell(store: makeSplashStore(environment: environment))
    shell.activate()
    defer { shell.deactivate() }

    shell.appeared()
    shell.ceremonyFinished()

    XCTAssertEqual(environment.notifyHostArgs, [.completed(path: .ceremony)])
  }

  /// The safety net, through the shell: three seconds of virtual time after
  /// `appeared()`, the host hears the safety-net path.
  func testTheSafetyNetNotifiesTheHostOnVirtualTime() async {
    let clock = TestClock()
    let environment = SplashEnvironmentMock(clock: clock)
    let shell = SplashViewShell(store: makeSplashStore(environment: environment))
    shell.activate()
    defer { shell.deactivate() }

    shell.appeared()
    await clock.advance(byNanoseconds: 2_999_000_000)
    XCTAssertEqual(environment.notifyHostArgs, [], "the net waits its full duration")

    await clock.advance(byNanoseconds: 1_000_000)
    await Task.megaYield()
    XCTAssertEqual(environment.notifyHostArgs, [.completed(path: .safetyNet)])
  }

  /// Teardown: after `deactivate()` the armed net never fires.
  func testDeactivateStopsTheEffectLoop() async {
    let clock = TestClock()
    let environment = SplashEnvironmentMock(clock: clock)
    let shell = SplashViewShell(store: makeSplashStore(environment: environment))
    shell.activate()
    shell.appeared()
    XCTAssertTrue(shell.viewState.isArmed)

    shell.deactivate()
    await clock.advance(byNanoseconds: 10_000_000_000)
    await Task.megaYield()

    XCTAssertEqual(environment.notifyHostArgs, [], "an effect ran after deactivate")
  }
}
