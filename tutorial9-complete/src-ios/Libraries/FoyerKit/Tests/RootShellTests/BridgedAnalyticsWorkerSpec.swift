// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import DuetShells
import DuetTelemetry
import FoyerBridge
import FoyerKit
import XCTest
import os

/// The crossing's receipt: a Kotlin-minted event reaches a Swift sink with
/// its subject, verb token and every param intact, and the consent seed
/// reaches the sink before any event can. The two grammars encode
/// identically by the telemetry package's own twin fixtures; what this file
/// gates is this app's converter — the four-arm param map and the Int32
/// narrowing, which no fixture of theirs reaches.
final class BridgedAnalyticsWorkerSpec: XCTestCase {

  func testABridgedEventReachesTheSinkAsItsSwiftTwin() {
    let sink = RecordingSink()
    let analytics = BridgedAnalyticsWorker(sinks: [sink], isEnabled: true)

    analytics.track(
      event: FoyerKit.TrackedEvent(
        subject: "Session",
        verb: FoyerKit.TrackedVerb(rendered: "Signed In"),
        params: [
          TrackedParamStringParam(key: "provider", value: "email"),
          TrackedParamIntParam(key: "attempt", value: 2),
          TrackedParamBoolParam(key: "cached", value: true),
          TrackedParamDoubleParam(key: "elapsed_s", value: 1.5),
        ]))

    XCTAssertEqual(sink.recorded.count, 1)
    // The vendor-facing name is the grammar's one encoding rule, and the verb
    // token crosses verbatim — a converter that re-derived it would spell the
    // event differently on the two platforms.
    XCTAssertEqual(sink.recorded.first?.encodedName(), "Session Signed In")
    XCTAssertEqual(
      sink.recorded.first?.params,
      [
        .string(key: "provider", value: "email"),
        .int(key: "attempt", value: 2),
        .bool(key: "cached", value: true),
        .double(key: "elapsed_s", value: 1.5),
      ])
  }

  func testTheSeedReachesEverySinkBeforeAnyEventCan() {
    let sink = RecordingSink()
    let analytics = BridgedAnalyticsWorker(sinks: [sink], isEnabled: false)

    XCTAssertFalse(sink.isEnabled)
    XCTAssertFalse(analytics.isEnabled)

    analytics.setEnabled(enabled: true)
    XCTAssertTrue(sink.isEnabled)
    XCTAssertTrue(analytics.isEnabled)
  }
}

/// Records what the fan-out hands it — the shape a vendor sink takes, minus
/// the SDK.
private final class RecordingSink: AnalyticsTrackingWorking {
  private struct State {
    var events: [DuetTelemetry.TrackedEvent] = []
    var enabled = true
  }

  private let state = OSAllocatedUnfairLock(initialState: State())

  var recorded: [DuetTelemetry.TrackedEvent] { state.withLock { $0.events } }

  var isEnabled: Bool { state.withLock { $0.enabled } }

  func setEnabled(_ enabled: Bool) {
    state.withLock { $0.enabled = enabled }
  }

  func track(event: DuetTelemetry.TrackedEvent) {
    state.withLock { $0.events.append(event) }
  }

  func identify(uid: String) {}

  func reset() {}

  func run() async { await untilCancelled() }
}
