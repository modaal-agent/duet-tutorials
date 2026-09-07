// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import FoyerKit
import XCTest

@testable import OnboardingShell

/// The level mounts one step from its page and the progress row beside it;
/// the page reaches the row as a projection, each step's readiness reaches
/// it laterally through the worker the level owns; the last step's
/// Continued climbs `Completed` with both answers; Back is the level's.
@MainActor
final class OnboardingViewShellSpec: XCTestCase {

  private final class NoDependency: OnboardingDependency {}

  private func build(
    page: OnboardingPage = .welcome,
    onDelegate: @escaping (OnboardingDelegateEvent) -> Void = { _ in }
  ) -> OnboardingChild {
    OnboardingBuilder(dependency: NoDependency()).buildOnboarding(page: page, onDelegate: onDelegate)
  }

  func testTheStepsWalkAndTheRowHearsBothRoutes() async {
    var events: [OnboardingDelegateEvent] = []
    let shell = build { events.append($0) }.shell
    shell.activate()
    defer { shell.deactivate() }
    let progress = shell.progress.shell
    progress.appeared()
    XCTAssertEqual(progress.viewState.stepNumber, 1)
    XCTAssertFalse(shell.viewState.canGoBack)

    guard case .welcome(let welcome) = shell.viewState.step else { return XCTFail("the welcome step") }
    welcome.shell.appeared()
    await settle(until: progress.viewState.readySteps.contains(1), "the welcome step's readiness, laterally")
    welcome.shell.continueTapped()
    await settle(until: progress.viewState.stepNumber == 2, "the page, projected down")
    XCTAssertTrue(shell.viewState.canGoBack)

    guard case .name(let name) = shell.viewState.step else { return XCTFail("the name step") }
    name.shell.draftChanged("   ")
    name.shell.continueTapped()
    XCTAssertTrue(name.shell.viewState.validation is NameValidationEmpty)
    name.shell.draftChanged("Ann")
    await settle(until: progress.viewState.readySteps.contains(2), "the name step's readiness, laterally")
    name.shell.continueTapped()
    await settle(until: progress.viewState.stepNumber == 3, "the last page")

    guard case .preferences(let preferences) = shell.viewState.step else {
      return XCTFail("the preferences step")
    }
    XCTAssertFalse(preferences.shell.viewState.isReady)
    preferences.shell.toggle("tips")
    await settle(until: progress.viewState.readySteps.contains(3), "the last step's readiness, laterally")
    preferences.shell.continueTapped()
    await settle(until: events.count == 1, "the host heard Completed")
    guard case .completed(let completed) = onEnum(of: events[0]) else {
      return XCTFail("expected Completed, got \(events[0])")
    }
    XCTAssertEqual(completed.name, "Ann")
    XCTAssertEqual(completed.preferences, ["tips"])
    XCTAssertEqual(shell.host.liveWorkerCount, 1, "the seam's worker is the level's")
  }

  func testBackWalksThePagesAndARestoredPageIsTheFirstShown() async {
    let shell = build(page: .preferences).shell
    shell.activate()
    defer { shell.deactivate() }
    guard case .preferences = shell.viewState.step else { return XCTFail("the restored step") }
    XCTAssertEqual(shell.progress.shell.viewState.stepNumber, 3)

    shell.back()
    await settle(until: shell.progress.shell.viewState.stepNumber == 2, "Back moved the page")
    guard case .name = shell.viewState.step else { return XCTFail("the name step") }
    shell.back()
    await settle(until: shell.progress.shell.viewState.stepNumber == 1, "Back moved the page again")
    shell.back()
    XCTAssertEqual(shell.progress.shell.viewState.stepNumber, 1, "Back on the first page is inert")
  }
}
