// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import FoyerKit
import XCTest

@testable import ProfileShell

/// The profile tab mounts the account screen from state, and the account
/// screen's `Closed` clears it.
@MainActor
final class ProfileViewShellSpec: XCTestCase {

  /// The on-device backend over a memory file: a fresh document per test.
  private let backend = LocalBackend(file: MemoryFile(text: nil), scope: mainImmediateStoreScope())

  func testTheAccountScreenMountsAndCloses() async {
    let child = ProfileBuilder(
      dependency: ProfileDependencyMock(account: backend.account, auth: backend.auth)
    ).buildProfile(displayName: "Ann") { _ in }
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
}
