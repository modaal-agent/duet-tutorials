// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.editname

import dev.modaal.duet.services.telemetry.TrackedEvent
import dev.modaal.duet.services.telemetry.TrackedVerb

/**
 * The editor's named events. No param: the name is the user's content, and
 * the grammar's rule is that content never rides an event.
 */
object EditNameEvents {
  val nameEdited: TrackedEvent = TrackedEvent("Name", TrackedVerb.Edited)
}
