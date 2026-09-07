// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import FoyerBridge
import FoyerKit
import XCTest

@testable import MainShell

/// Both tabs are built with the level and bracketed by it; the tab intent
/// crosses the boundary and projects back; the profile tree's sign-out
/// request climbs out through the delegate.
@MainActor
final class MainViewShellSpec: XCTestCase {

  /// The on-device backend over a memory file: a fresh document per test.
  private let backend = LocalBackend(file: MemoryFile(text: nil), scope: mainImmediateStoreScope())

  func testBothTabsAreMountedAndTheTabProjects() {
    let child = MainBuilder(
      dependency: MainDependencyMock(
        account: backend.account, auth: backend.auth, items: backend.items, purchases: backend.purchases)
    ).buildMain(displayName: "Ann", entitlement: Projected(EntitlementFree.shared)) { _ in }
    let shell = child.shell
    shell.activate()
    defer { shell.deactivate() }

    XCTAssertNotNil(shell.home)
    XCTAssertEqual(shell.profile?.shell.viewState.displayName, "Ann")
    XCTAssertEqual(shell.viewState.activeTab, .home)
    shell.selectTab(.profile)
    XCTAssertEqual(shell.viewState.activeTab, .profile)
  }

  func testTheSliceReachesBothTabs() async {
    let entitlement = Projected<Entitlement>(EntitlementFree.shared)
    let child = MainBuilder(
      dependency: MainDependencyMock(
        account: backend.account, auth: backend.auth, items: backend.items, purchases: backend.purchases)
    ).buildMain(displayName: "Ann", entitlement: entitlement) { _ in }
    let shell = child.shell
    shell.activate()
    defer { shell.deactivate() }

    XCTAssertEqual(shell.home?.shell.viewState.isInsightsLocked, true)
    XCTAssertEqual(shell.profile?.shell.viewState.planLabel, "Free")
    entitlement.project(EntitlementPremium(plan: PlanYearly.shared))
    XCTAssertEqual(shell.home?.shell.viewState.isInsightsLocked, false)
    XCTAssertEqual(shell.profile?.shell.viewState.planLabel, "Premium · Yearly")
  }

  func testTheSignOutClimbsThroughTheProfileTree() async {
    var events: [MainDelegateEvent] = []
    let child = MainBuilder(
      dependency: MainDependencyMock(
        account: backend.account, auth: backend.auth, items: backend.items, purchases: backend.purchases)
    ).buildMain(displayName: "Ann", entitlement: Projected(EntitlementFree.shared)) { events.append($0) }
    let shell = child.shell
    shell.activate()
    defer { shell.deactivate() }

    guard let profile = shell.profile else { return XCTFail("the profile tab is mounted") }
    profile.shell.accountTapped()
    guard let account = profile.shell.viewState.account else {
      return XCTFail("the account screen is mounted")
    }
    account.shell.signOutTapped()
    await settle(until: events.count == 1, "the host heard the request")
    guard case .signOutRequested = onEnum(of: events[0]) else {
      return XCTFail("expected SignOutRequested, got \(events[0])")
    }
  }
}
