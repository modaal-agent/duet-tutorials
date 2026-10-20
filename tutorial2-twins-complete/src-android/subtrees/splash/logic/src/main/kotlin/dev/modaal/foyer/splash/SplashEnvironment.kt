// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.splash

import dev.modaal.duet.kernel.KernelClock

/**
 * The feature's door to the platform: a clock to wait on and a sink for the
 * delegate event. The Android app implements it; the Swift twin declares
 * its own protocol for the iPhone app.
 */
interface SplashEnvironment {
  /**
   * The clock the safety net waits on. The effect handler calls `sleep` here
   * rather than `delay` directly, so a test can run the wait on virtual time.
   */
  val clock: KernelClock

  /** Hand a delegate event to the host. */
  fun notifyHost(event: SplashDelegateEvent)
}
