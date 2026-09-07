// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.upgrade

import dev.modaal.duet.services.telemetry.TrackedEvent
import dev.modaal.duet.services.telemetry.TrackedParam
import dev.modaal.duet.services.telemetry.TrackedVerb
import dev.modaal.foyer.ports.Plan

/** The flow's named events: a purchase that went through, with the plan bought. */
object UpgradeEvents {
  fun completed(plan: Plan): TrackedEvent =
    TrackedEvent(
      "Upgrade",
      TrackedVerb.Completed,
      listOf(
        TrackedParam.string(
          "plan",
          when (plan) {
            Plan.Monthly -> "monthly"
            Plan.Yearly -> "yearly"
          })))
}
