// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.account

import dev.modaal.duet.services.telemetry.TrackedEvent
import dev.modaal.duet.services.telemetry.TrackedVerb

/** The account screen's named events: the session ended, on the starter verb. */
object AccountEvents {
  val signedOut: TrackedEvent = TrackedEvent("Session", TrackedVerb.SignedOut)
}
