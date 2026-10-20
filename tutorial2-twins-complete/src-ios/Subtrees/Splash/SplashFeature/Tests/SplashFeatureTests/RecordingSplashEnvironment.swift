// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import Duet
import SplashFeature

/// A test double for the environment: it records every delegate event the
/// handler hands to the host, and it waits on the clock the test passes in —
/// a `TestClock`, so the safety net runs on virtual time. Test sources only;
/// the iPhone app implements `SplashEnvironment` itself.
final class RecordingSplashEnvironment: SplashEnvironment {
  let clock: any KernelClock
  private(set) var notified: [SplashDelegateEvent] = []

  init(clock: any KernelClock) {
    self.clock = clock
  }

  func notifyHost(_ event: SplashDelegateEvent) {
    notified.append(event)
  }
}
