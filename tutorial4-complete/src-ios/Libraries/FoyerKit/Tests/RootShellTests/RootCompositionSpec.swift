// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import FoyerKit
import XCTest

@testable import RootShell

/// The composition root's receipt: the whole tree builds over the generated
/// `RootDependencyMock` and the on-device backend in a memory file, mounts
/// each child from the phase, and tears down the one that left. The auth
/// seed and every session change arrive through the session worker, the
/// entitlement through the entitlement worker. Across the boundary on real
/// time.
@MainActor
final class RootCompositionSpec: XCTestCase {

  func testTheTreeMountsFromThePhaseAndTheSignOutClimbsToTheGate() async {
    let root = RootBuilder(dependency: RootDependencyMock(storage: MemoryFile(text: nil))).buildRoot()
    let shell = root.shell
    shell.activate()
    defer { shell.deactivate() }

    guard case .splash(let splash) = shell.viewState.child else {
      return XCTFail("the splash is mounted first")
    }
    // The session worker's first report: the sticky stream's current value.
    await settle(until: isSignedOut(shell.store.state.auth), "the auth seed arrived")
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
    XCTAssertEqual(profile.shell.viewState.planLabel, "Free")

    profile.shell.accountTapped()
    guard let account = profile.shell.viewState.account else {
      return XCTFail("the account screen is mounted")
    }
    account.shell.signOutTapped()
    await settle(until: isSignIn(shell.viewState.child), "the gate came back up")
    XCTAssertEqual(shell.host.liveWorkerCount, 2)
  }

  func testAPurchaseUnlocksTheCardThroughTheStreamAndSurvivesARelaunch() async {
    let file = MemoryFile(text: nil)
    let root = RootBuilder(dependency: RootDependencyMock(storage: file)).buildRoot()
    let shell = root.shell
    shell.activate()

    guard case .splash(let splash) = shell.viewState.child else {
      return XCTFail("the splash is mounted first")
    }
    await settle(until: isSignedOut(shell.store.state.auth), "the auth seed arrived")
    splash.shell.ceremonyFinished()
    await settle(until: isSignIn(shell.viewState.child), "the gate came up")
    guard case .signIn(let gate) = shell.viewState.child else { return }
    gate.shell.continueWithEmail("ann@example.com")
    await settle(until: isMain(shell.viewState.child), "main came up")
    guard case .main(let main) = shell.viewState.child, let home = main.shell.home else { return }

    home.shell.insightsTapped()
    home.shell.purchaseTapped()
    await settle(until: home.shell.viewState.presented == nil, "the promo closed on the answer")
    await settle(until: isPremium(shell.store.state.entitlement), "the root heard the stream")
    await settle(until: !home.shell.viewState.isInsightsLocked, "the card unlocked from the stream")
    XCTAssertEqual(main.shell.profile?.shell.viewState.planLabel, "Premium · Monthly")
    shell.deactivate()
    await settle(until: shell.host.liveWorkerCount == 0, "both workers settled")

    // The relaunch: the persisted session skips the gate, the persisted
    // plan arrives with the first report of the entitlement stream.
    let relaunched = RootBuilder(dependency: RootDependencyMock(storage: file)).buildRoot()
    relaunched.shell.activate()
    defer { relaunched.shell.deactivate() }
    await settle(until: isSignedIn(relaunched.shell.store.state.auth), "the session was restored")
    guard case .splash(let splashAgain) = relaunched.shell.viewState.child else {
      return XCTFail("the splash is mounted first")
    }
    splashAgain.shell.ceremonyFinished()
    await settle(until: isMain(relaunched.shell.viewState.child), "straight to main")
    guard case .main(let mainAgain) = relaunched.shell.viewState.child else { return }
    XCTAssertEqual(mainAgain.shell.home?.shell.viewState.isInsightsLocked, false)
  }

  private func isSignIn(_ child: RootChildMount?) -> Bool {
    if case .signIn = child { return true }
    return false
  }

  private func isMain(_ child: RootChildMount?) -> Bool {
    if case .main = child { return true }
    return false
  }

  private func isPremium(_ entitlement: Entitlement) -> Bool {
    if case .premium = onEnum(of: entitlement) { return true }
    return false
  }

  private func isSignedOut(_ auth: AuthSnapshot) -> Bool {
    if case .signedOut = onEnum(of: auth) { return true }
    return false
  }

  private func isSignedIn(_ auth: AuthSnapshot) -> Bool {
    if case .signedIn = onEnum(of: auth) { return true }
    return false
  }
}
