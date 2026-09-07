// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import DuetShells
import DuetTesting
import FoyerKit
import XCTest

@testable import RootShell

/// The two workers through the harness: start, drive the backend, read what
/// reached the relay, finish. The workers carry no recordings; `finish()`
/// fails if a `run()` outlives its cancellation.
@MainActor
final class WorkersSpec: XCTestCase {

  private let backend = LocalBackend(file: MemoryFile(text: nil), scope: mainImmediateStoreScope())

  func testTheSessionWorkerSeedsTheRootAndFollowsEveryChange() async {
    let relay = Relay<any RootAction>()
    var reported: [any RootAction] = []
    relay.sink = { reported.append($0) }
    let tester = WorkerTester(SessionWorker(auth: backend.auth, relay: relay))

    tester.start()
    await settle(until: reported.count == 1, "the sticky stream's current value arrived")
    XCTAssertTrue(isAuth(reported[0], signedIn: nil))

    backend.auth.signIn(provider: SignInProviderEmail(address: "ann@example.com")) { _ in }
    await settle(until: reported.count == 2, "the sign-in was reported")
    XCTAssertTrue(isAuth(reported[1], signedIn: "ann"))

    backend.account.saveDisplayName(name: "Ann B") {}
    await settle(until: reported.count == 3, "the saved name was reported")
    XCTAssertTrue(isAuth(reported[2], signedIn: "Ann B"))

    await tester.finish()
    XCTAssertTrue(tester.isFinished)
  }

  func testTheEntitlementWorkerReportsThePurchaseThroughTheStreamAlone() async {
    let relay = Relay<any RootAction>()
    var reported: [any RootAction] = []
    relay.sink = { reported.append($0) }
    let tester = WorkerTester(EntitlementWorker(purchases: backend.purchases, relay: relay))

    tester.start()
    await settle(until: reported.count == 1, "the sticky stream's current value arrived")
    XCTAssertTrue(isEntitlement(reported[0], premium: false))

    backend.purchases.purchase(plan: PlanYearly.shared) { _ in }
    await settle(until: reported.count == 2, "the purchase was reported after its delay")
    XCTAssertTrue(isEntitlement(reported[1], premium: true))

    await tester.finish()
  }

  func testAFinishedWorkerReportsNothingMore() async {
    let relay = Relay<any RootAction>()
    var reported: [any RootAction] = []
    relay.sink = { reported.append($0) }
    let tester = WorkerTester(SessionWorker(auth: backend.auth, relay: relay))
    tester.start()
    await settle(until: reported.count == 1, "the seed arrived")
    await tester.finish()

    backend.auth.signIn(provider: SignInProviderGuest.shared) { _ in }
    await settleTurns()
    XCTAssertEqual(reported.count, 1, "nothing after the bracket closes")
  }

  private func isAuth(_ action: any RootAction, signedIn name: String?) -> Bool {
    guard case .authChanged(let changed) = onEnum(of: action) else { return false }
    switch onEnum(of: changed.auth) {
    case .signedOut: return name == nil
    case .signedIn(let signedIn): return signedIn.displayName == name
    case .unknown: return false
    }
  }

  private func isEntitlement(_ action: any RootAction, premium: Bool) -> Bool {
    guard case .entitlementChanged(let changed) = onEnum(of: action) else { return false }
    if case .premium = onEnum(of: changed.entitlement) { return premium }
    return !premium
  }

  private func settleTurns() async {
    for _ in 0..<20 {
      try? await Task.sleep(nanoseconds: 20_000_000)
    }
  }
}
