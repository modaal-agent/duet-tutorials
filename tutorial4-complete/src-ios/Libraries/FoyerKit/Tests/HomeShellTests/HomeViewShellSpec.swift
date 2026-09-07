// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import FoyerKit
import XCTest

@testable import HomeShell

/// The list and purchase round trips over the generated `HomeDependencyMock`
/// and the on-device backend, and the slice arriving as an action.
@MainActor
final class HomeViewShellSpec: XCTestCase {

  /// The on-device backend over a memory file: a fresh document per test.
  private let backend = LocalBackend(file: MemoryFile(text: nil), scope: mainImmediateStoreScope())

  private func build() -> HomeChild {
    HomeBuilder(dependency: HomeDependencyMock(items: backend.items, purchases: backend.purchases))
      .buildHome()
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

  func testThePurchaseAnswersThroughThePortAndUnlocksNothingByItself() async {
    let shell = build().shell
    shell.activate()
    defer { shell.deactivate() }

    shell.insightsTapped()
    shell.purchaseTapped()
    XCTAssertTrue(shell.viewState.isPurchasing)
    // The backend answers after its delay on the wall clock here; the
    // promo closes on the answer, and the card stays locked because the
    // entitlement reaches this tab only as the root's slice.
    await settle(until: shell.viewState.presented == nil, "the promo closed")
    XCTAssertFalse(shell.viewState.isPurchasing)
    XCTAssertTrue(shell.viewState.isInsightsLocked)
    guard case .premium = onEnum(of: entitlementsFlow(purchases: backend.purchases).value) else {
      return XCTFail("the stream carries the purchase")
    }
  }
}
