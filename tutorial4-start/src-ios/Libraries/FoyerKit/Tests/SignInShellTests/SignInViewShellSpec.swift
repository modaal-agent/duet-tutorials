// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import FoyerKit
import FoyerServices
import XCTest

@testable import SignInShell

/// The shells lane's rows for the gate, composed over the generated
/// `SignInDependencyMock` and the mock auth service: the Builder's wiring
/// end to end, and the port's callback re-entering across the boundary.
@MainActor
final class SignInViewShellSpec: XCTestCase {

  func testAnEmailSignInCompletesThroughThePort() async {
    var completed: [SignInDelegateEvent] = []
    let child = SignInBuilder(dependency: SignInDependencyMock(auth: MockAuth()))
      .buildSignIn { completed.append($0) }
    let shell = child.shell
    shell.activate()
    defer { shell.deactivate() }

    // The mock answers inside the call, so the in-flight flag is never
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
    let child = SignInBuilder(dependency: SignInDependencyMock(auth: MockAuth()))
      .buildSignIn { _ in XCTFail("the host must not be notified") }
    child.shell.activate()
    defer { child.shell.deactivate() }

    child.shell.continueWithEmail("")
    XCTAssertEqual(child.shell.viewState.failure, "Enter an email address.")
    XCTAssertFalse(child.shell.viewState.isSigningIn)
  }
}
