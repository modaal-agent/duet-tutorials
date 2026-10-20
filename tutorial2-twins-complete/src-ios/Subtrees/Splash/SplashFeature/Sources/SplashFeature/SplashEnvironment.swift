// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import Duet

/// The feature's door to the platform: a clock to wait on and a sink for the
/// delegate event. The app implements it; tests use the generated
/// `SplashEnvironmentMock`.
/// sourcery: CreateMock
public protocol SplashEnvironment {
  /// The clock the safety net waits on. The effect handler calls `sleep` here
  /// rather than `Task.sleep` directly, so a test can run the wait on virtual
  /// time.
  var clock: any KernelClock { get }

  /// Hand a delegate event to the host.
  func notifyHost(_ event: SplashDelegateEvent)
}
