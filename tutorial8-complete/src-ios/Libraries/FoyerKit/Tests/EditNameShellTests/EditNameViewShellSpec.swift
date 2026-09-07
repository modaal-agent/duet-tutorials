// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import FoyerKit
import XCTest

@testable import EditNameShell

/// The save round trip over the generated `EditNameDependencyMock` and the
/// on-device backend: the saved name reaches the session stream.
@MainActor
final class EditNameViewShellSpec: XCTestCase {

  /// The on-device backend over a memory file: a fresh document per test.
  private let backend = LocalBackend(file: MemoryFile(text: nil), scope: mainImmediateStoreScope())

  func testAValidNameSavesAndClimbs() async {
    var events: [EditNameDelegateEvent] = []
    backend.auth.signIn(provider: SignInProviderGuest.shared) { _ in }
    let child = EditNameBuilder(dependency: EditNameDependencyMock(account: backend.account))
      .buildEditName(currentName: "Ann") { events.append($0) }
    let shell = child.shell
    shell.activate()
    defer { shell.deactivate() }

    XCTAssertEqual(shell.viewState.draft, "Ann")
    shell.draftChanged(" Ann B ")
    shell.save()
    await settle(until: events.count == 1, "the parent was notified")

    guard case .signedIn(let session) = onEnum(of: sessionsFlow(auth: backend.auth).value) else {
      return XCTFail("the session carries the saved name")
    }
    XCTAssertEqual(session.displayName, "Ann B")
    guard case .saved(let saved) = onEnum(of: events[0]) else {
      return XCTFail("expected Saved, got \(events[0])")
    }
    XCTAssertEqual(saved.name, "Ann B")
  }

  func testAnEmptyNameIsRejectedBeforeThePort() {
    backend.auth.signIn(provider: SignInProviderGuest.shared) { _ in }
    let child = EditNameBuilder(dependency: EditNameDependencyMock(account: backend.account))
      .buildEditName(currentName: "Ann") { _ in XCTFail("nothing climbs") }
    child.shell.activate()
    defer { child.shell.deactivate() }

    child.shell.draftChanged("   ")
    child.shell.save()
    XCTAssertTrue(child.shell.viewState.validation is NameValidationEmpty)
    guard case .signedIn(let session) = onEnum(of: sessionsFlow(auth: backend.auth).value) else {
      return XCTFail("still signed in")
    }
    XCTAssertEqual(session.displayName, "Guest")
  }
}
