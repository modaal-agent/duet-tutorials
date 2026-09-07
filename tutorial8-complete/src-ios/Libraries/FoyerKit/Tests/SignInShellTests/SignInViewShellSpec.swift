// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import FoyerKit
import XCTest

@testable import SignInShell

/// The shells lane's rows for the gate, composed over the generated
/// `SignInDependencyMock` and the on-device backend: the Builder's wiring
/// end to end, and the port's callback re-entering across the boundary.
@MainActor
final class SignInViewShellSpec: XCTestCase {

  /// The on-device backend over a memory file: a fresh document per test.
  private let backend = LocalBackend(file: MemoryFile(text: nil), scope: mainImmediateStoreScope())

  func testAnEmailSignInCompletesThroughThePort() async {
    var completed: [SignInDelegateEvent] = []
    let child = SignInBuilder(dependency: SignInDependencyMock(auth: backend.auth))
      .buildSignIn { completed.append($0) }
    let shell = child.shell
    shell.activate()
    defer { shell.deactivate() }

    // The backend answers inside the call, so the in-flight flag is never
    // observable here; the recordings pin it. What this row pins is the
    // round trip: the port's callback re-enters as an action, and the
    // delegate leaves through the Swift environment.
    shell.continueWithEmail("ann@example.com")
    await settle(until: completed.count == 1, "the host was notified")

    XCTAssertFalse(shell.viewState.isSigningIn)
    guard case .completed(let event) = onEnum(of: completed[0]) else {
      return XCTFail("expected Completed, got \(completed[0])")
    }
    XCTAssertEqual(event.displayName, "ann")
  }

  func testAnEmptyAddressNeverReachesThePort() {
    let child = SignInBuilder(dependency: SignInDependencyMock(auth: backend.auth))
      .buildSignIn { _ in XCTFail("the host must not be notified") }
    child.shell.activate()
    defer { child.shell.deactivate() }

    child.shell.continueWithEmail("")
    XCTAssertTrue(child.shell.viewState.failure is SignInFailureEmptyAddress)
    XCTAssertFalse(child.shell.viewState.isSigningIn)
  }
}
