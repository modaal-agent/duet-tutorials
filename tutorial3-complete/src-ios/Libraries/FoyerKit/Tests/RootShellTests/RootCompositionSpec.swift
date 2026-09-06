// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import FoyerKit
import XCTest

@testable import RootShell

/// The composition root's receipt: the whole tree builds over the generated
/// `RootDependencyMock`, mounts each child from the phase, and tears down
/// the one that left. One walk from the splash through the gate into main
/// and back to the gate on a sign-out, across the boundary on real time.
@MainActor
final class RootCompositionSpec: XCTestCase {

  func testTheTreeMountsFromThePhaseAndTheSignOutClimbsToTheGate() async {
    let root = RootBuilder(dependency: RootDependencyMock()).buildRoot()
    let shell = root.shell
    shell.activate()
    defer { shell.deactivate() }

    guard case .splash(let splash) = shell.viewState.child else {
      return XCTFail("the splash is mounted first")
    }
    splash.shell.appeared()
    splash.shell.ceremonyFinished()
    await settle(until: isSignIn(shell.viewState.child), "the gate came up")

    guard case .signIn(let gate) = shell.viewState.child else { return }
    gate.shell.continueWithEmail("ann@example.com")
    await settle(until: isMain(shell.viewState.child), "main came up")

    guard case .main(let main) = shell.viewState.child,
      let profile = main.shell.profile
    else { return }
    XCTAssertEqual(profile.shell.viewState.displayName, "ann")

    profile.shell.accountTapped()
    guard let account = profile.shell.viewState.account else {
      return XCTFail("the account screen is mounted")
    }
    account.shell.signOutTapped()
    await settle(until: isSignIn(shell.viewState.child), "the gate came back up")
  }

  private func isSignIn(_ child: RootChildMount?) -> Bool {
    if case .signIn = child { return true }
    return false
  }

  private func isMain(_ child: RootChildMount?) -> Bool {
    if case .main = child { return true }
    return false
  }
}
