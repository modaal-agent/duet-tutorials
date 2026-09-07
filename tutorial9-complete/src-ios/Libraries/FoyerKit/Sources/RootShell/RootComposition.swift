// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import FoyerBridge
import FoyerKit
import Foundation
import MainShell
import OnboardingShell
import SignInShell

// The root level's composition triple, and the one place that knows the
// whole tree. Hand-written rather than generated: the root owns objects,
// and a generated Component forwards only.

/// What the root consumes from the platform: the file the backend persists
/// its document in. The app target's `SceneComponent` supplies a file under
/// the app's documents; the composition spec supplies memory.
///
/// `CreateMock` generates `RootDependencyMock` in this package's test
/// target; the composition spec builds the whole tree over it.
/// sourcery: CreateMock
public protocol RootDependency: AnyObject {
  var storage: any KeyValueFile { get }
}

/// The root Component owns what is scoped to the app: the on-device
/// backend, whose four members are the ports, and the analytics sink every
/// feature's `Track` effect reaches. It satisfies each child's Dependency
/// with those members, one conformance per child level; `analytics` is the
/// member every level's Dependency names.
final class RootComponent {
  private let dependency: RootDependency
  private let backend: LocalBackend

  /// The one vendor sink this app ships: the console. Picking a real vendor
  /// is one more class conforming to the telemetry package's
  /// `AnalyticsTrackingWorking` — the only file that imports the SDK — added
  /// to the list below and adopted in its own right.
  let consoleSink = ConsoleAnalyticsSink()

  /// The fan-out over the sink list, wearing the bridged port so a Kotlin
  /// `Track` effect lands on it unchanged; a worker the root adopts. The
  /// seed reaches every sink in the initializer, before any event can.
  let analyticsWorker: BridgedAnalyticsWorker

  init(dependency: RootDependency, scope: any Kotlinx_coroutines_coreCoroutineScope) {
    self.dependency = dependency
    backend = LocalBackend(file: dependency.storage, scope: scope)
    analyticsWorker = BridgedAnalyticsWorker(sinks: [consoleSink], isEnabled: true)
  }

  var auth: any AuthPort { backend.auth }
  var items: any ItemsPort { backend.items }
  var account: any AccountPort { backend.account }
  var purchases: any PurchasesPort { backend.purchases }
  var analytics: any AnalyticsTracking { analyticsWorker }
}

extension RootComponent: SignInDependency {}
extension RootComponent: OnboardingDependency {}
extension RootComponent: MainDependency {}

/// The root's environment: the void calls the reducer's effects make. The
/// link forward lands in the shell, which holds it until main is up; the
/// launch event lands on the app's one sink.
final class LiveRootEnvironment: NSObject, RootEnvironment {
  private let account: any AccountPort
  private let analytics: any AnalyticsTracking
  private let onForward: (DeepLink) -> Void

  init(
    account: any AccountPort,
    analytics: any AnalyticsTracking,
    onForward: @escaping (DeepLink) -> Void
  ) {
    self.account = account
    self.analytics = analytics
    self.onForward = onForward
  }

  func forwardLink(link: DeepLink) {
    onForward(link)
  }

  func completeOnboarding(name: String, preferences: [String]) {
    account.completeOnboarding(name: name, preferences: preferences)
  }

  func track(event: TrackedEvent) {
    analytics.track(event: event)
  }
}
