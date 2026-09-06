// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import FoyerKit
import FoyerServices
import XCTest

@testable import EditNameShell

/// The save round trip over the generated `EditNameDependencyMock` and the
/// mock account service.
@MainActor
final class EditNameViewShellSpec: XCTestCase {

  func testAValidNameSavesAndClimbs() async {
    var events: [EditNameDelegateEvent] = []
    let account = MockAccount()
    let child = EditNameBuilder(dependency: EditNameDependencyMock(account: account))
      .buildEditName(currentName: "Ann") { events.append($0) }
    let shell = child.shell
    shell.activate()
    defer { shell.deactivate() }

    XCTAssertEqual(shell.viewState.draft, "Ann")
    shell.draftChanged(" Ann B ")
    shell.save()
    await settle(until: events.count == 1, "the parent was notified")

    XCTAssertEqual(account.displayName, "Ann B")
    guard case .saved(let saved) = onEnum(of: events[0]) else {
      return XCTFail("expected Saved, got \(events[0])")
    }
    XCTAssertEqual(saved.name, "Ann B")
  }

  func testAnEmptyNameIsRejectedBeforeThePort() {
    let account = MockAccount()
    let child = EditNameBuilder(dependency: EditNameDependencyMock(account: account))
      .buildEditName(currentName: "Ann") { _ in XCTFail("nothing climbs") }
    child.shell.activate()
    defer { child.shell.deactivate() }

    child.shell.draftChanged("   ")
    child.shell.save()
    XCTAssertEqual(child.shell.viewState.validation, "Enter a name.")
    XCTAssertNil(account.displayName)
  }
}
