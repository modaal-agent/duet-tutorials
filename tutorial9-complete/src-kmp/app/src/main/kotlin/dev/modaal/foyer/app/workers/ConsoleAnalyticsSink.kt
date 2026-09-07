// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.app.workers

import dev.modaal.duet.services.telemetry.AnalyticsTrackingWorking
import dev.modaal.duet.services.telemetry.TrackedEvent
import dev.modaal.duet.services.telemetry.encodedName
import dev.modaal.duet.services.telemetry.encodedProperties
import dev.modaal.duet.shells.untilCancelled

/**
 * The vendor sink, minus the vendor: one line per event on the console, in
 * the shape a vendor SDK receives — the encoded name and the property bag.
 * A sink is a worker: the composition root adopts it, and `run()` parks for
 * the mount's lifetime. The enabled flag is the sink's own, where a vendor
 * SDK's opt-out switch would be, and a disabled sink prints nothing.
 * `line` is the console; a test passes a list.
 */
class ConsoleAnalyticsSink(
  private val line: (String) -> Unit = ::println,
) : AnalyticsTrackingWorking {
  @Volatile private var enabled = true

  override val isEnabled: Boolean
    get() = enabled

  override fun setEnabled(enabled: Boolean) {
    this.enabled = enabled
  }

  override fun track(event: TrackedEvent) {
    if (!enabled) return
    val bag = event.encodedProperties()
    val properties =
      if (bag.isEmpty()) "" else bag.entries.joinToString(", ", " {", "}") { "${it.key}=${it.value}" }
    line("analytics: ${event.encodedName()}$properties")
  }

  override fun identify(uid: String) {
    if (enabled) line("analytics: identify $uid")
  }

  override fun reset() {
    if (enabled) line("analytics: reset")
  }

  /** Parks until the host cancels: a console has nothing to flush. */
  override suspend fun run() = untilCancelled()
}
