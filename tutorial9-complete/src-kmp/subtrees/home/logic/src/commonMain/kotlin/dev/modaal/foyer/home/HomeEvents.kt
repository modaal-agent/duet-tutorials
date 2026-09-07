// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.home

import dev.modaal.duet.services.telemetry.TrackedEvent
import dev.modaal.duet.services.telemetry.TrackedVerb

/** The tab's named events: the promo, shown when a locked card is tapped. */
object HomeEvents {
  val promoViewed: TrackedEvent = TrackedEvent("Promo", TrackedVerb.Viewed)
}
