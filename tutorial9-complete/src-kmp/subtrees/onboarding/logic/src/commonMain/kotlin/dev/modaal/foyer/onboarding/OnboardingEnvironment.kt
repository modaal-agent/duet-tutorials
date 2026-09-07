// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.onboarding

import dev.modaal.duet.services.telemetry.TrackedEvent

/** The level's door to the platform: the delegate sink only. */
interface OnboardingEnvironment {
  fun notifyHost(event: OnboardingDelegateEvent)

  /** Forward a `Track` effect's event to the app's one sink (fire-and-forget). */
  fun track(event: TrackedEvent)
}
