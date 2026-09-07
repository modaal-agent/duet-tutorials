// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import DuetTelemetry
import DuetTesting
import XCTest

@testable import RootShell

/// The sink worker through the harness: start, hand it events, read the
/// console, finish. What is pinned is the sink's own duty — the encoded name
/// and the bag on one line, nothing while disabled — and the seed the
/// fan-out pushes into it before any event can reach it.
@MainActor
final class ConsoleAnalyticsSinkSpec: XCTestCase {

  private let signedIn = TrackedEvent(
    subject: "Session", verb: TrackedVerb("Signed In"), params: [.string(key: "provider", value: "email")])

  func testAnEventPrintsItsVendorFacingNameAndBag() async {
    let lines = Recorder()
    let sink = ConsoleAnalyticsSink { lines.add($0) }
    let tester = WorkerTester(sink)
    tester.start()

    sink.track(event: signedIn)
    sink.track(event: TrackedEvent(subject: "Name", verb: .edited))
    XCTAssertEqual(
      lines.all, ["analytics: Session Signed In {provider=email}", "analytics: Name Edited"])

    await tester.finish()
    XCTAssertTrue(tester.isFinished)
  }

  func testADisabledSinkPrintsNothingUntilEnabledAgain() {
    let lines = Recorder()
    let sink = ConsoleAnalyticsSink { lines.add($0) }

    sink.setEnabled(false)
    sink.track(event: signedIn)
    sink.identify(uid: "uid-1")
    XCTAssertTrue(lines.all.isEmpty, "a disabled sink egresses nothing")

    sink.setEnabled(true)
    sink.track(event: signedIn)
    XCTAssertEqual(lines.all, ["analytics: Session Signed In {provider=email}"])
  }

  func testTheFanOutSeedsTheSinkBeforeAnyEventReachesIt() {
    let lines = Recorder()
    let sink = ConsoleAnalyticsSink { lines.add($0) }
    let analytics = AnalyticsTrackingWorker(sinks: [sink], isEnabled: false)

    // The seed landed in the initializer: the sink reports it, and drops the event.
    XCTAssertFalse(sink.isEnabled)
    analytics.track(event: signedIn)
    XCTAssertTrue(lines.all.isEmpty)

    analytics.setEnabled(true)
    analytics.track(event: signedIn)
    XCTAssertEqual(lines.all, ["analytics: Session Signed In {provider=email}"])
  }
}

/// The console, as a list. `@unchecked`: the lock is the whole state.
private final class Recorder: @unchecked Sendable {
  private let lock = NSLock()
  private var lines: [String] = []

  func add(_ line: String) {
    lock.withLock { lines.append(line) }
  }

  var all: [String] { lock.withLock { lines } }
}
