// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import FoyerKit
import OnboardingShell
import XCTest

@testable import RootShell

/// The composition root's receipt: the whole tree builds over the generated
/// `RootDependencyMock` and the on-device backend in a memory file, mounts
/// each child from the phase, and tears down the one that left. The auth
/// seed and every session change arrive through the session worker, the
/// entitlement through the entitlement worker; the onboarding gate feeds
/// its progress row from both routes; a deep link waits for main; a saved
/// spine rebuilds the tree. Across the boundary on real time.
@MainActor
final class RootCompositionSpec: XCTestCase {

  func testTheTreeMountsFromThePhaseAndTheSignOutClimbsToTheGate() async {
    let root = RootBuilder(dependency: RootDependencyMock(storage: MemoryFile(text: nil))).buildRoot(restored: nil)
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
    // A new account: the onboarding gate, then main with the name it entered.
    await settle(until: isOnboarding(shell.viewState.child), "the onboarding gate came up")
    await onboard(shell, name: "Ann")
    await settle(until: isMain(shell.viewState.child), "main came up")

    guard case .main(let main) = shell.viewState.child,
      let profile = main.shell.profile
    else { return }
    XCTAssertEqual(profile.shell.viewState.displayName, "Ann")
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
    let root = RootBuilder(dependency: RootDependencyMock(storage: file)).buildRoot(restored: nil)
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
    await settle(until: isOnboarding(shell.viewState.child), "the onboarding gate came up")
    await onboard(shell, name: "Ann")
    await settle(until: isMain(shell.viewState.child), "main came up")
    guard case .main(let main) = shell.viewState.child, let home = main.shell.home else { return }

    // The promo hands off; the flow buys; the stream unlocks the card.
    home.shell.insightsTapped()
    home.shell.upgradeTapped()
    await settle(until: main.shell.viewState.upgrade != nil, "the sheet slot mounted the flow")
    guard let upgrade = main.shell.viewState.upgrade else { return }
    upgrade.shell.appeared()
    await settle(until: upgrade.shell.viewState.cards.count == 2, "the plans arrived")
    upgrade.shell.selectPlan(PlanMonthly.shared)
    upgrade.shell.confirm()
    await settle(until: upgrade.shell.viewState.step == .done, "the purchase answered")
    await settle(until: isPremium(shell.store.state.entitlement), "the root heard the stream")
    await settle(until: !home.shell.viewState.isInsightsLocked, "the card unlocked from the stream")
    XCTAssertEqual(main.shell.profile?.shell.viewState.planLabel, "Premium · Monthly")
    upgrade.shell.done()
    await settle(until: main.shell.viewState.upgrade == nil, "the sheet cleared")
    shell.deactivate()
    await settle(until: shell.host.liveWorkerCount == 0, "both workers settled")

    // The relaunch: the persisted session skips the gate, the persisted
    // plan arrives with the first report of the entitlement stream.
    let relaunched = RootBuilder(dependency: RootDependencyMock(storage: file)).buildRoot(restored: nil)
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

  func testTheOnboardingGateFeedsTheProgressRowFromBothRoutes() async {
    let root = RootBuilder(dependency: RootDependencyMock(storage: MemoryFile(text: nil))).buildRoot(restored: nil)
    let shell = root.shell
    shell.activate()
    defer { shell.deactivate() }

    await settle(until: isSignedOut(shell.store.state.auth), "the auth seed arrived")
    guard case .splash(let splash) = shell.viewState.child else { return XCTFail("the splash is mounted first") }
    splash.shell.ceremonyFinished()
    await settle(until: isSignIn(shell.viewState.child), "the gate came up")
    guard case .signIn(let gate) = shell.viewState.child else { return }
    gate.shell.continueAsGuest()
    await settle(until: isOnboarding(shell.viewState.child), "the onboarding gate came up")
    guard case .onboarding(let onboarding) = shell.viewState.child else { return }
    let progress = onboarding.shell.progress.shell
    progress.appeared()
    // The page, projected down: step 1 of 3. The readiness, laterally.
    XCTAssertEqual(progress.viewState.stepNumber, 1)
    guard case .welcome(let welcome) = onboarding.shell.viewState.step else { return XCTFail("the welcome step") }
    welcome.shell.appeared()
    await settle(until: progress.viewState.readySteps.contains(1), "the welcome step's readiness arrived laterally")
    welcome.shell.continueTapped()
    await settle(until: progress.viewState.stepNumber == 2, "the page moved and the row followed")
    guard case .name(let name) = onboarding.shell.viewState.step else { return XCTFail("the name step") }
    name.shell.draftChanged("A")
    await settle(until: progress.viewState.readySteps.contains(2), "the name step's readiness arrived laterally")
    name.shell.draftChanged("")
    await settle(until: !progress.viewState.readySteps.contains(2), "and its change followed")
    onboarding.shell.back()
    await settle(until: progress.viewState.stepNumber == 1, "Back moved the page back")
    XCTAssertEqual(shell.host.liveWorkerCount, 2)
    XCTAssertEqual(onboarding.shell.host.liveWorkerCount, 1, "the seam's worker is the level's")
  }

  func testADeepLinkDuringTheSplashOpensTheFlowWhenMainMounts() async {
    let file = MemoryFile(text: nil)
    let first = RootBuilder(dependency: RootDependencyMock(storage: file)).buildRoot(restored: nil)
    first.shell.activate()
    await settle(until: isSignedOut(first.shell.store.state.auth), "the auth seed arrived")
    guard case .splash(let splash) = first.shell.viewState.child else { return XCTFail("the splash is mounted first") }
    splash.shell.ceremonyFinished()
    await settle(until: isSignIn(first.shell.viewState.child), "the gate came up")
    guard case .signIn(let gate) = first.shell.viewState.child else { return }
    gate.shell.continueAsGuest()
    await settle(until: isOnboarding(first.shell.viewState.child), "the onboarding gate came up")
    await onboard(first.shell, name: "Guest")
    await settle(until: isMain(first.shell.viewState.child), "main came up")
    first.shell.deactivate()
    await settle(until: first.shell.host.liveWorkerCount == 0, "both workers settled")

    let root = RootBuilder(dependency: RootDependencyMock(storage: file)).buildRoot(restored: nil)
    let shell = root.shell
    shell.activate()
    defer { shell.deactivate() }
    shell.openLink(DeepLinkUpgrade.shared)
    XCTAssertNotNil(shell.store.state.pendingLink, "held during the splash")
    await settle(until: isSignedIn(shell.store.state.auth), "the session was restored")
    guard case .splash(let splashAgain) = shell.viewState.child else { return XCTFail("the splash is mounted first") }
    splashAgain.shell.ceremonyFinished()
    await settle(until: isMain(shell.viewState.child), "straight to main")
    guard case .main(let main) = shell.viewState.child else { return }
    await settle(until: main.shell.viewState.upgrade != nil, "the held link opened the flow as main mounted")
    XCTAssertNil(shell.store.state.pendingLink)

    // A link under main reaches the profile tree at once.
    main.shell.viewState.upgrade?.shell.dismissed()
    await settle(until: main.shell.viewState.upgrade == nil, "dismissed")
    shell.openLink(DeepLinkProfileAccount.shared)
    await settle(until: main.shell.profile?.shell.viewState.account != nil, "the account link reached the profile tree")
    XCTAssertEqual(main.shell.viewState.activeTab, .profile)
  }

  func testTheTreeRestoresFromASavedSpine() async {
    let file = MemoryFile(text: nil)
    let first = RootBuilder(dependency: RootDependencyMock(storage: file)).buildRoot(restored: nil)
    first.shell.activate()
    await settle(until: isSignedOut(first.shell.store.state.auth), "the auth seed arrived")
    guard case .splash(let splash) = first.shell.viewState.child else { return XCTFail("the splash is mounted first") }
    splash.shell.ceremonyFinished()
    await settle(until: isSignIn(first.shell.viewState.child), "the gate came up")
    guard case .signIn(let gate) = first.shell.viewState.child else { return }
    gate.shell.continueWithEmail("ann@example.com")
    await settle(until: isOnboarding(first.shell.viewState.child), "the onboarding gate came up")
    await onboard(first.shell, name: "Ann")
    await settle(until: isMain(first.shell.viewState.child), "main came up")
    guard case .main(let main) = first.shell.viewState.child, let profile = main.shell.profile else { return }
    main.shell.selectTab(.profile)
    profile.shell.accountTapped()
    profile.shell.viewState.account?.shell.editNameTapped()
    // What the scene saves on the way out.
    let saved = encodeRouteSpine(spine: first.shell.routeSpine())
    first.shell.deactivate()
    await settle(until: first.shell.host.liveWorkerCount == 0, "both workers settled")

    // The rebuild: the splash replays, then main mounts with every sliver.
    let restored = RootBuilder(dependency: RootDependencyMock(storage: file))
      .buildRoot(restored: decodeRouteSpine(text: saved))
    restored.shell.activate()
    defer { restored.shell.deactivate() }
    await settle(until: isSignedIn(restored.shell.store.state.auth), "the session was restored")
    guard case .splash(let splashAgain) = restored.shell.viewState.child else { return XCTFail("the splash is mounted first") }
    splashAgain.shell.ceremonyFinished()
    await settle(until: isMain(restored.shell.viewState.child), "straight to main")
    guard case .main(let mainAgain) = restored.shell.viewState.child else { return }
    XCTAssertEqual(mainAgain.shell.viewState.activeTab, .profile)
    let account = mainAgain.shell.profile?.shell.viewState.account
    XCTAssertEqual(account?.shell.viewState.displayName, "Ann")
    XCTAssertNotNil(account?.shell.viewState.editor, "the editor is mounted over the account screen")
    XCTAssertNil(decodeRouteSpine(text: "not a spine"), "a stale payload restores nothing")
  }

  /// The three onboarding steps, as a new account walks them.
  private func onboard(_ shell: RootViewShell, name: String) async {
    guard case .onboarding(let onboarding) = shell.viewState.child else { return XCTFail("the onboarding gate") }
    guard case .welcome(let welcome) = onboarding.shell.viewState.step else { return XCTFail("the welcome step") }
    welcome.shell.continueTapped()
    await settle(until: isNameStep(onboarding.shell.viewState.step), "the name step came up")
    guard case .name(let nameStep) = onboarding.shell.viewState.step else { return }
    nameStep.shell.draftChanged(name)
    nameStep.shell.continueTapped()
    await settle(until: isPreferencesStep(onboarding.shell.viewState.step), "the preferences step came up")
    guard case .preferences(let preferences) = onboarding.shell.viewState.step else { return }
    preferences.shell.toggle("digest")
    preferences.shell.continueTapped()
  }

  private func isNameStep(_ step: OnboardingStepMount?) -> Bool {
    if case .name = step { return true }
    return false
  }

  private func isPreferencesStep(_ step: OnboardingStepMount?) -> Bool {
    if case .preferences = step { return true }
    return false
  }

  private func isOnboarding(_ child: RootChildMount?) -> Bool {
    if case .onboarding = child { return true }
    return false
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
