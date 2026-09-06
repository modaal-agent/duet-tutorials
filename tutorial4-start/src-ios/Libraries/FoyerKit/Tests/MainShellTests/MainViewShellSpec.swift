// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import FoyerKit
import FoyerServices
import XCTest

@testable import MainShell

/// Both tabs are built with the level and bracketed by it; the tab intent
/// crosses the boundary and projects back; the profile tree's sign-out
/// request climbs out through the delegate.
@MainActor
final class MainViewShellSpec: XCTestCase {

  func testBothTabsAreMountedAndTheTabProjects() {
    let child = MainBuilder(
      dependency: MainDependencyMock(account: MockAccount(), auth: MockAuth(), items: MockItems())
    ).buildMain(displayName: "Ann") { _ in }
    let shell = child.shell
    shell.activate()
    defer { shell.deactivate() }

    XCTAssertNotNil(shell.home)
    XCTAssertEqual(shell.profile?.shell.viewState.displayName, "Ann")
    XCTAssertEqual(shell.viewState.activeTab, .home)
    shell.selectTab(.profile)
    XCTAssertEqual(shell.viewState.activeTab, .profile)
  }

  func testTheSignOutClimbsThroughTheProfileTree() async {
    var events: [MainDelegateEvent] = []
    let child = MainBuilder(
      dependency: MainDependencyMock(account: MockAccount(), auth: MockAuth(), items: MockItems())
    ).buildMain(displayName: "Ann") { events.append($0) }
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
