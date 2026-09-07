// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.app.workers

import dev.modaal.duet.services.telemetry.AnalyticsTrackingWorker
import dev.modaal.duet.services.telemetry.TrackedEvent
import dev.modaal.duet.services.telemetry.TrackedParam
import dev.modaal.duet.services.telemetry.TrackedVerb
import dev.modaal.duet.test.WorkerTester
import dev.modaal.foyer.telemetry.AppVerbs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

/**
 * The sink worker through the harness: start, hand it events, read the
 * console, finish. What is pinned is the sink's own duty — the encoded name
 * and the bag on one line, nothing while disabled — and the seed the fan-out
 * pushes into it before any event can reach it.
 */
class ConsoleAnalyticsSinkTest {

  private val signedIn =
    TrackedEvent("Session", AppVerbs.SignedIn, listOf(TrackedParam.string("provider", "email")))

  @Test
  fun anEventPrintsItsVendorFacingNameAndBag() = runTest {
    val lines = mutableListOf<String>()
    val sink = ConsoleAnalyticsSink(lines::add)
    val tester = WorkerTester(sink)
    tester.start(backgroundScope)
    runCurrent()

    sink.track(signedIn)
    sink.track(TrackedEvent("Name", TrackedVerb.Edited))
    assertEquals(
      listOf("analytics: Session Signed In {provider=email}", "analytics: Name Edited"), lines)

    tester.finish()
    assertTrue(tester.isFinished)
  }

  @Test
  fun aDisabledSinkPrintsNothingUntilEnabledAgain() = runTest {
    val lines = mutableListOf<String>()
    val sink = ConsoleAnalyticsSink(lines::add)

    sink.setEnabled(false)
    sink.track(signedIn)
    sink.identify("uid-1")
    assertTrue(lines.isEmpty(), "a disabled sink egresses nothing")

    sink.setEnabled(true)
    sink.track(signedIn)
    assertEquals(listOf("analytics: Session Signed In {provider=email}"), lines)
  }

  @Test
  fun theFanOutSeedsTheSinkBeforeAnyEventReachesIt() = runTest {
    val lines = mutableListOf<String>()
    val sink = ConsoleAnalyticsSink(lines::add)
    val analytics = AnalyticsTrackingWorker(sinks = listOf(sink), isEnabled = false)

    // The seed landed in the constructor: the sink reports it, and drops the event.
    assertFalse(sink.isEnabled)
    analytics.track(signedIn)
    assertTrue(lines.isEmpty())

    analytics.setEnabled(true)
    analytics.track(signedIn)
    assertEquals(listOf("analytics: Session Signed In {provider=email}"), lines)
  }
}
