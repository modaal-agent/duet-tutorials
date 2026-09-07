// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.onboarding

import dev.modaal.duet.services.telemetry.TrackedEvent
import dev.modaal.duet.services.telemetry.TrackedParam
import dev.modaal.duet.services.telemetry.TrackedVerb

/**
 * The level's named events. The count of preferences is a number about the
 * choice; the preferences themselves, and the name, stay out of the event.
 */
object OnboardingEvents {
  fun completed(preferenceCount: Int): TrackedEvent =
    TrackedEvent(
      "Onboarding",
      TrackedVerb.Completed,
      listOf(TrackedParam.int("preference_count", preferenceCount)))
}
