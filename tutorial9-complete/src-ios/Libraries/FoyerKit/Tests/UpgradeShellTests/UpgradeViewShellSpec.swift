// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import FoyerBridge
import FoyerKit
import XCTest

@testable import UpgradeShell

/// The flow over the generated `UpgradeDependencyMock` and the on-device
/// backend: the plans arrive, the steps walk as route state, the purchase
/// answers through the port and moves the step, and the delegate climbs.
@MainActor
final class UpgradeViewShellSpec: XCTestCase {

  /// The on-device backend over a memory file: a fresh document per test.
  private let backend = LocalBackend(file: MemoryFile(text: nil), scope: mainImmediateStoreScope())

  private func build(
    step: UpgradeStep = UpgradeStepPlans.shared,
    onDelegate: @escaping (UpgradeDelegateEvent) -> Void = { _ in }
  ) -> UpgradeChild {
    UpgradeBuilder(dependency: UpgradeDependencyMock(analytics: BridgedAnalyticsWorker(sinks: []), purchases: backend.purchases))
      .buildUpgrade(step: step, onDelegate: onDelegate)
  }

  func testThePlansArriveAndTheStepsWalkToDone() async {
    var events: [UpgradeDelegateEvent] = []
    let shell = build { events.append($0) }.shell
    shell.activate()
    defer { shell.deactivate() }

    shell.appeared()
    await settle(until: shell.viewState.cards.count == 2, "the two plans arrived")
    XCTAssertEqual(shell.viewState.cards.map(\.price), ["$4.99", "$39.99"])
    shell.selectPlan(PlanYearly.shared)
    XCTAssertEqual(shell.viewState.step, .confirm(planName: "Yearly", price: "$39.99"))
    shell.back()
    XCTAssertEqual(shell.viewState.step, .plans)
    shell.selectPlan(PlanMonthly.shared)
    shell.confirm()
    XCTAssertTrue(shell.viewState.isPurchasing)
    // The backend answers after its delay on the wall clock here.
    await settle(until: shell.viewState.step == .done, "the purchase moved the step")
    XCTAssertFalse(shell.viewState.isPurchasing)
    guard case .premium = onEnum(of: entitlementsFlow(purchases: backend.purchases).value) else {
      return XCTFail("the stream carries the purchase; the flow never did")
    }
    shell.done()
    await settle(until: events.count == 1, "the host heard Completed")
    guard case .completed = onEnum(of: events[0]) else {
      return XCTFail("expected Completed, got \(events[0])")
    }
  }

  func testBackOnTheFirstStepAndADismissalClimbDismissed() async {
    var events: [UpgradeDelegateEvent] = []
    let shell = build { events.append($0) }.shell
    shell.activate()
    defer { shell.deactivate() }

    shell.back()
    await settle(until: events.count == 1, "the host heard the first Dismissed")
    shell.dismissed()
    await settle(until: events.count == 2, "the host heard the second Dismissed")
    for event in events {
      guard case .dismissed = onEnum(of: event) else { return XCTFail("expected Dismissed, got \(event)") }
    }
  }

  func testARestoredStepIsTheFirstStateShown() {
    let shell = build(step: UpgradeStepConfirm(plan: PlanMonthly.shared)).shell
    shell.activate()
    defer { shell.deactivate() }
    XCTAssertEqual(shell.viewState.step, .confirm(planName: "Monthly", price: ""))
  }
}
