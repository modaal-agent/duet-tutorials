// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import FoyerKit
import XCTest

@testable import HomeShell

/// The list round trip over the generated `HomeDependencyMock` and the
/// on-device backend, the slice arriving as an action, and the promo's
/// hand-off to the host.
@MainActor
final class HomeViewShellSpec: XCTestCase {

  /// The on-device backend over a memory file: a fresh document per test.
  private let backend = LocalBackend(file: MemoryFile(text: nil), scope: mainImmediateStoreScope())

  private func build(onDelegate: @escaping (HomeDelegateEvent) -> Void = { _ in }) -> HomeChild {
    HomeBuilder(dependency: HomeDependencyMock(items: backend.items))
      .buildHome(presented: nil, onDelegate: onDelegate)
  }

  func testTheListLoadsOnceOnAppear() async {
    let shell = build().shell
    shell.activate()
    defer { shell.deactivate() }

    shell.appeared()
    await settle(until: shell.viewState.items.count == 12, "the twelve rows arrived")
    XCTAssertFalse(shell.viewState.isLoading)

    // A repeat appearance (a tab switch, a rotation) reloads nothing.
    shell.appeared()
    XCTAssertFalse(shell.viewState.isLoading)
  }

  func testTheCardOpensThePromoWhileLockedAndTheSummaryOnceUnlocked() {
    let shell = build().shell
    shell.activate()
    defer { shell.deactivate() }

    XCTAssertTrue(shell.viewState.isInsightsLocked)
    shell.insightsTapped()
    XCTAssertEqual(shell.viewState.presented, .promo)
    shell.dismissed()
    XCTAssertNil(shell.viewState.presented)

    shell.entitlementChanged(EntitlementPremium(plan: PlanMonthly.shared))
    XCTAssertFalse(shell.viewState.isInsightsLocked)
    shell.insightsTapped()
    XCTAssertEqual(shell.viewState.presented, .insights)
  }

  func testThePromoHandsOffToTheHostAndUnlocksNothingByItself() async {
    var events: [HomeDelegateEvent] = []
    let shell = build { events.append($0) }.shell
    shell.activate()
    defer { shell.deactivate() }

    shell.insightsTapped()
    shell.upgradeTapped()
    XCTAssertNil(shell.viewState.presented, "the promo closes on the tap")
    await settle(until: events.count == 1, "the host heard the request")
    guard case .upgradeRequested = onEnum(of: events[0]) else {
      return XCTFail("expected UpgradeRequested, got \(events[0])")
    }
    // The card stays locked: the entitlement reaches this tab only as the root's slice.
    XCTAssertTrue(shell.viewState.isInsightsLocked)
  }
}
