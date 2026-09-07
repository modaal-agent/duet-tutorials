// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import DuetShells
import FoyerKit
import XCTest

@testable import SplashShell

// The shells lane's rows for the splash. What belongs here is what only the
// bridge or the composition can break: that an intent reaches the Kotlin
// reducer and projects back before `send` returns, that the Kotlin effect
// loop dispatches into a Swift-implemented environment, and that
// `deactivate()` stops the runtime on the other side of the boundary. The
// reducer's behavior is not tested here; the recordings pin it on the Kotlin
// lane and again across the boundary in the replay suite.
//
// Real time, deliberately: no test dispatcher crosses the boundary.

/// Poll the delivery an assertion depends on, in 20 ms slices against a
/// ceiling. A passing row returns on the first satisfied poll.
@MainActor
private func settle(
  until reached: @autoclosure @MainActor () -> Bool,
  _ what: @autoclosure () -> String,
  withinSeconds seconds: Double = 5,
  file: StaticString = #filePath,
  line: UInt = #line
) async {
  for _ in 0..<Int(seconds * 50) {
    if reached() { return }
    try? await Task.sleep(nanoseconds: 20_000_000)
  }
  XCTFail("settle: \(what()) never happened within \(seconds)s", file: file, line: line)
}

/// A Swift environment that records what the Kotlin effect loop hands it.
private final class SpySplashEnvironment: NSObject, SplashEnvironment {
  private(set) var notified: [SplashDelegateEvent] = []

  var clock: KernelClock { LiveClock.shared }

  func notifyHost(event: SplashDelegateEvent) {
    notified.append(event)
  }
}

@MainActor
final class SplashViewShellSpec: XCTestCase {

  /// The builder's wiring, end to end: the intent crosses into the reducer
  /// and the projection reads back before `send` returns.
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
  /// reducer, whose `NotifyHost` effect calls the Swift environment with the
  /// ceremony path.
  ///
  /// The runtime is built by hand, with the builder's exact wiring, so the
  /// spy can stand where the builder puts the live environment.
  func testTheCeremonyPathNotifiesTheHost() async {
    let environment = SpySplashEnvironment()
    let scope = mainImmediateStoreScope()
    let store = makeSplashStore(environment: environment, scope: scope)
    let shell = SplashViewShell(
      store: SplashKitStore(
        state: splashStateFlow(store: store),
        send: { store.send(action: $0) },
        teardown: {
          store.teardown()
          cancelStoreScope(scope: scope)
        }))
    shell.activate()
    defer { shell.deactivate() }

    shell.appeared()
    shell.ceremonyFinished()
    await settle(until: environment.notified.count == 1, "the host was notified")

    guard case .completed(let completed) = onEnum(of: environment.notified[0]) else {
      return XCTFail("expected a Completed event, got \(environment.notified[0])")
    }
    guard case .ceremony = onEnum(of: completed.path) else {
      return XCTFail("expected the ceremony path, got \(completed.path)")
    }
  }

  /// Teardown across the boundary: after `deactivate()` the store still
  /// reduces, and no effect runs, because the scope the effects launch into
  /// is cancelled.
  func testDeactivateStopsTheEffectLoop() async {
    let environment = SpySplashEnvironment()
    let scope = mainImmediateStoreScope()
    let store = makeSplashStore(environment: environment, scope: scope)
    let kotlinState = splashStateFlow(store: store)
    let shell = SplashViewShell(
      store: SplashKitStore(
        state: kotlinState,
        send: { store.send(action: $0) },
        teardown: {
          store.teardown()
          cancelStoreScope(scope: scope)
        }))
    shell.activate()
    shell.appeared()
    XCTAssertTrue(kotlinState.value.isArmed)

    shell.deactivate()

    // Driven directly: the store object is still alive, only its scope is
    // gone. A notification arriving here would mean teardown did not cross.
    store.send(action: SplashActionCeremonyFinished.shared)
    try? await Task.sleep(nanoseconds: 200_000_000)
    XCTAssertTrue(environment.notified.isEmpty, "an effect ran after deactivate")
  }
}
