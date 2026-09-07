// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.telemetry

import dev.modaal.duet.services.telemetry.TrackedEvent
import dev.modaal.duet.services.telemetry.TrackedVerb
import dev.modaal.duet.services.telemetry.encodedName
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The app grammar's pin. The encoding rule itself (`"Subject Verb"`, the
 * property bag's order, the wire form) belongs to the artifact and is gated
 * by its suite — what is pinned here is that this app's verb reads the way
 * the dashboard expects when it goes through it, beside the starter verb it
 * pairs with.
 */
class AppVerbsTest {

  @Test
  fun theAppsVerbRendersAsAuthored() {
    assertEquals("Session Signed In", TrackedEvent("Session", AppVerbs.SignedIn).encodedName())
  }

  @Test
  fun theStarterVerbItPairsWithSpellsTheSameWay() {
    assertEquals("Session Signed Out", TrackedEvent("Session", TrackedVerb.SignedOut).encodedName())
  }
}
