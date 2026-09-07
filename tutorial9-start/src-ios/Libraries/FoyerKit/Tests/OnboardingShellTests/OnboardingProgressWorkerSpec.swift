// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import DuetTesting
import FoyerKit
import XCTest

@testable import OnboardingShell

/// The readiness seam through the harness: the value is sticky, so a late
/// subscriber sees each step's current value first; every change follows;
/// a cancelled subscription hears nothing more; the worker settles at finish.
@MainActor
final class OnboardingProgressWorkerSpec: XCTestCase {

  func testALateSubscriberSeesTheCurrentValueThenEveryChange() async {
    let worker = OnboardingProgressWorker()
    let seam = worker.seam
    let tester = WorkerTester(worker)
    tester.start()

    // Published before anyone observes.
    seam.updateReadiness(step: .welcome, ready: true)
    seam.updateReadiness(step: .name, ready: false)

    var received: [StepReadiness] = []
    let subscription = seam.observeReadiness { received.append($0) }
    XCTAssertEqual(received.count, 2, "the current value per step, first")
    XCTAssertEqual(received.first { $0.step == .welcome }?.ready, true)
    XCTAssertEqual(received.first { $0.step == .name }?.ready, false)

    seam.updateReadiness(step: .name, ready: true)
    XCTAssertEqual(received.count, 3)
    XCTAssertEqual(received.last?.step, .name)
    XCTAssertEqual(received.last?.ready, true)

    // The same value again is no change: nothing is delivered.
    seam.updateReadiness(step: .name, ready: true)
    XCTAssertEqual(received.count, 3)

    subscription.cancel()
    seam.updateReadiness(step: .preferences, ready: true)
    XCTAssertEqual(received.count, 3, "nothing after the subscription ends")

    await tester.finish()
    XCTAssertTrue(tester.isFinished)
  }
}
