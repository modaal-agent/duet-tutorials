// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import FoyerKit
import FoyerServices
import XCTest

@testable import HomeShell

/// The list round trip over the generated `HomeDependencyMock` and the mock
/// items service: the port's callback re-enters across the boundary.
@MainActor
final class HomeViewShellSpec: XCTestCase {

  func testTheListLoadsOnceOnAppear() async {
    let child = HomeBuilder(dependency: HomeDependencyMock(items: MockItems())).buildHome()
    let shell = child.shell
    shell.activate()
    defer { shell.deactivate() }

    shell.appeared()
    await settle(until: shell.viewState.items.count == 12, "the twelve rows arrived")
    XCTAssertFalse(shell.viewState.isLoading)

    // A repeat appearance (a tab switch, a rotation) reloads nothing.
    shell.appeared()
    XCTAssertFalse(shell.viewState.isLoading)
  }
}
