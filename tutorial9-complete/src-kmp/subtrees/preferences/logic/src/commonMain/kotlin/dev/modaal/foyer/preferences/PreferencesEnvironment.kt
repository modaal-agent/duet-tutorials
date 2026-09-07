// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.preferences

import dev.modaal.foyer.ports.OnboardingPage

/** The step's door to the platform: the readiness seam's producer side and the delegate sink. */
interface PreferencesEnvironment {
  fun updateReadiness(step: OnboardingPage, ready: Boolean)

  fun notifyHost(event: PreferencesDelegateEvent)
}
