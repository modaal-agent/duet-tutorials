// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import FoyerBridge
import FoyerKit
import XCTest

@testable import ProfileShell

/// The profile tab mounts the account screen from state, the account
/// screen's `Closed` clears it, the plan row climbs its request, a forwarded
/// link mounts the account screen, and a restored path rebuilds the depth.
@MainActor
final class ProfileViewShellSpec: XCTestCase {

  /// The on-device backend over a memory file: a fresh document per test.
  private let backend = LocalBackend(file: MemoryFile(text: nil), scope: mainImmediateStoreScope())

  func testTheAccountScreenMountsAndCloses() async {
    let child = ProfileBuilder(
      dependency: ProfileDependencyMock(account: backend.account, analytics: BridgedAnalyticsWorker(sinks: []), auth: backend.auth)
    ).buildProfile(displayName: "Ann", restoredPath: nil) { _ in }
    let shell = child.shell
    shell.activate()
    defer { shell.deactivate() }

    XCTAssertNil(shell.viewState.account)
    shell.accountTapped()
    guard let account = shell.viewState.account else {
      return XCTFail("the account screen is mounted before send returns")
    }
    XCTAssertEqual(account.shell.viewState.displayName, "Ann")

    account.shell.closeTapped()
    await settle(until: shell.viewState.account == nil, "the account screen was dismissed")
  }

  func testThePlanRowClimbsAndALinkMountsTheAccountScreen() async {
    var events: [ProfileDelegateEvent] = []
    let child = ProfileBuilder(
      dependency: ProfileDependencyMock(account: backend.account, analytics: BridgedAnalyticsWorker(sinks: []), auth: backend.auth)
    ).buildProfile(displayName: "Ann", restoredPath: nil) { events.append($0) }
    let shell = child.shell
    shell.activate()
    defer { shell.deactivate() }

    shell.planTapped()
    await settle(until: events.count == 1, "the host heard the request")
    guard case .upgradeRequested = onEnum(of: events[0]) else {
      return XCTFail("expected UpgradeRequested, got \(events[0])")
    }
    shell.openLink(DeepLinkProfileAccount.shared)
    XCTAssertNotNil(shell.viewState.account, "the link mounts the account screen as the row does")
    XCTAssertTrue(shell.routePath is ProfilePathAccount)
  }

  func testARestoredPathRebuildsTheDepth() {
    let child = ProfileBuilder(
      dependency: ProfileDependencyMock(account: backend.account, analytics: BridgedAnalyticsWorker(sinks: []), auth: backend.auth)
    ).buildProfile(displayName: "Ann", restoredPath: ProfilePathEditName.shared) { _ in }
    let shell = child.shell
    shell.activate()
    defer { shell.deactivate() }

    guard let account = shell.viewState.account else { return XCTFail("the account screen is mounted") }
    XCTAssertNotNil(account.shell.viewState.editor, "the editor is mounted over it")
    XCTAssertTrue(shell.routePath is ProfilePathEditName)
  }
}
