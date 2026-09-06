// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import FoyerKit
import FoyerServices
import XCTest

@testable import AccountShell

/// The shells lane's rows for a level with a child: the editor mounts from
/// state, its `Saved` climbs into this level's store, and the sign-out
/// climbs out through the delegate.
@MainActor
final class AccountViewShellSpec: XCTestCase {

  private func build(onDelegate: @escaping (AccountDelegateEvent) -> Void = { _ in })
    -> AccountChild
  {
    AccountBuilder(dependency: AccountDependencyMock(account: MockAccount(), auth: MockAuth()))
      .buildAccount(displayName: "Ann", onDelegate: onDelegate)
  }

  func testTheEditorMountsFromStateAndItsSaveClimbs() async {
    var events: [AccountDelegateEvent] = []
    let child = build { events.append($0) }
    let shell = child.shell
    shell.activate()
    defer { shell.deactivate() }

    XCTAssertNil(shell.viewState.editor)
    shell.editNameTapped()
    let editor = try? XCTUnwrap(shell.viewState.editor, "the editor is mounted before send returns")
    guard let editor else { return }

    editor.shell.draftChanged("Ann B")
    editor.shell.save()
    await settle(until: shell.viewState.editor == nil, "the editor was dismissed")

    XCTAssertEqual(shell.viewState.displayName, "Ann B")
    await settle(until: events.count == 1, "the parent heard NameChanged")
    guard case .nameChanged(let changed) = onEnum(of: events[0]) else {
      return XCTFail("expected NameChanged, got \(events[0])")
    }
    XCTAssertEqual(changed.name, "Ann B")
  }

  func testTheSignOutClimbsWhenThePortConfirms() async {
    var events: [AccountDelegateEvent] = []
    let child = build { events.append($0) }
    child.shell.activate()
    defer { child.shell.deactivate() }

    child.shell.signOutTapped()
    await settle(until: events.count == 1, "the parent heard the request")
    guard case .signOutRequested = onEnum(of: events[0]) else {
      return XCTFail("expected SignOutRequested, got \(events[0])")
    }
  }
}
