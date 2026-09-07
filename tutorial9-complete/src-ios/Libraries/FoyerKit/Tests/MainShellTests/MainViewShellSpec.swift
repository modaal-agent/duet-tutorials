// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import FoyerBridge
import FoyerKit
import UpgradeShell
import XCTest

@testable import MainShell

/// Both tabs are built with the level and bracketed by it; the tab intent
/// crosses the boundary and projects back; the profile tree's sign-out
/// request climbs out through the delegate; either tab's request mounts the
/// flow in the sheet slot; a forwarded link sets the tab, the sheet or the
/// profile tree's depth; a restored spine rebuilds every sliver.
@MainActor
final class MainViewShellSpec: XCTestCase {

  /// The on-device backend over a memory file: a fresh document per test.
  private let backend = LocalBackend(file: MemoryFile(text: nil), scope: mainImmediateStoreScope())

  func testBothTabsAreMountedAndTheTabProjects() {
    let child = MainBuilder(
      dependency: MainDependencyMock(
        account: backend.account, analytics: BridgedAnalyticsWorker(sinks: []), auth: backend.auth,
        items: backend.items, purchases: backend.purchases)
    ).buildMain(displayName: "Ann", entitlement: Projected(EntitlementFree.shared), restored: nil) { _ in }
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
        account: backend.account, analytics: BridgedAnalyticsWorker(sinks: []), auth: backend.auth,
        items: backend.items, purchases: backend.purchases)
    ).buildMain(displayName: "Ann", entitlement: entitlement, restored: nil) { _ in }
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
        account: backend.account, analytics: BridgedAnalyticsWorker(sinks: []), auth: backend.auth,
        items: backend.items, purchases: backend.purchases)
    ).buildMain(displayName: "Ann", entitlement: Projected(EntitlementFree.shared), restored: nil) { events.append($0) }
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

  private func build(
    restored: RouteSpine? = nil, onDelegate: @escaping (MainDelegateEvent) -> Void = { _ in }
  ) -> MainChild {
    MainBuilder(
      dependency: MainDependencyMock(
        account: backend.account, analytics: BridgedAnalyticsWorker(sinks: []), auth: backend.auth,
        items: backend.items, purchases: backend.purchases)
    ).buildMain(
      displayName: "Ann", entitlement: Projected(EntitlementFree.shared), restored: restored,
      onDelegate: onDelegate)
  }

  func testEitherTabMountsTheFlowAndTheFlowsCompletedClearsIt() async {
    let shell = build().shell
    shell.activate()
    defer { shell.deactivate() }

    guard let home = shell.home else { return XCTFail("the home tab is mounted") }
    home.shell.insightsTapped()
    home.shell.upgradeTapped()
    await settle(until: shell.viewState.upgrade != nil, "the sheet slot mounted the flow")
    guard let upgrade = shell.viewState.upgrade else { return }
    upgrade.shell.appeared()
    await settle(until: upgrade.shell.viewState.cards.count == 2, "the plans arrived")
    upgrade.shell.selectPlan(PlanMonthly.shared)
    upgrade.shell.confirm()
    await settle(until: upgrade.shell.viewState.step == .done, "the purchase answered after its delay")
    // The flow moved its step; the card stays locked until the root's slice says otherwise.
    XCTAssertTrue(home.shell.viewState.isInsightsLocked)
    upgrade.shell.done()
    await settle(until: shell.viewState.upgrade == nil, "the sheet cleared on Completed")

    guard let profile = shell.profile else { return XCTFail("the profile tab is mounted") }
    profile.shell.planTapped()
    await settle(until: shell.viewState.upgrade != nil, "the plan row mounted the same slot")
    shell.viewState.upgrade?.shell.dismissed()
    await settle(until: shell.viewState.upgrade == nil, "the sheet cleared on Dismissed")
  }

  func testAForwardedLinkSetsTheTabTheSheetOrTheDepth() async {
    let shell = build().shell
    shell.activate()
    defer { shell.deactivate() }

    shell.openLink(DeepLinkUpgrade.shared)
    XCTAssertNotNil(shell.viewState.upgrade, "the upgrade link mounts the flow")
    shell.viewState.upgrade?.shell.dismissed()
    await settle(until: shell.viewState.upgrade == nil, "dismissed")

    shell.openLink(DeepLinkProfileAccount.shared)
    XCTAssertEqual(shell.viewState.activeTab, .profile)
    await settle(until: shell.profile?.shell.viewState.account != nil, "the link travelled on to the profile tab")
    XCTAssertTrue(shell.routeSpine().profilePath is ProfilePathAccount)
  }

  func testARestoredSpineRebuildsEverySliver() {
    let spine = RouteSpine(
      phase: RootPhaseMain.shared,
      activeTab: MainTabProfile.shared,
      profilePath: ProfilePathEditName.shared,
      homePresented: HomePresentationInsights.shared,
      upgradeStep: UpgradeStepConfirm(plan: PlanYearly.shared),
      onboardingPage: nil)
    let shell = build(restored: spine).shell
    shell.activate()
    defer { shell.deactivate() }

    XCTAssertEqual(shell.viewState.activeTab, .profile)
    XCTAssertEqual(shell.home?.shell.viewState.presented, .insights)
    XCTAssertNotNil(shell.profile?.shell.viewState.account?.shell.viewState.editor)
    guard case .confirm(let planName, _) = shell.viewState.upgrade?.shell.viewState.step else {
      return XCTFail("the flow is mounted on its restored step")
    }
    XCTAssertEqual(planName, "Yearly")
    XCTAssertEqual(encodeRouteSpine(spine: shell.routeSpine()), encodeRouteSpine(spine: spine))
  }
}
