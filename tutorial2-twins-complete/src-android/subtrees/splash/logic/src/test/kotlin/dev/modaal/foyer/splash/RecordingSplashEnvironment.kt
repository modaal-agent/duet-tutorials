// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.splash

import dev.modaal.duet.kernel.KernelClock
import dev.modaal.duet.kernel.LiveClock

/**
 * A test double for the environment: it records every delegate event the
 * handler hands to the host, and it waits on `LiveClock`, which under
 * `runTest` suspends on the test scheduler's virtual time rather than on a
 * wall clock. Test sources only; the apps implement `SplashEnvironment`
 * themselves.
 */
class RecordingSplashEnvironment(
  override val clock: KernelClock = LiveClock,
) : SplashEnvironment {
  val notified = mutableListOf<SplashDelegateEvent>()

  override fun notifyHost(event: SplashDelegateEvent) {
    notified += event
  }
}
