// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.app.workers

import dev.modaal.duet.test.WorkerTester
import dev.modaal.foyer.ports.OnboardingPage
import dev.modaal.foyer.ports.StepReadiness
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

/**
 * The readiness seam through the harness: the value is sticky, so a late
 * subscriber sees each step's current value first; every change follows;
 * a cancelled subscription hears nothing more; the worker settles at finish.
 */
class OnboardingProgressWorkerTest {

  @Test
  fun aLateSubscriberSeesTheCurrentValueThenEveryChange() = runTest {
    val worker = OnboardingProgressWorker(backgroundScope)
    val tester = WorkerTester(worker)
    tester.start(backgroundScope)

    // Published before anyone observes.
    worker.updateReadiness(OnboardingPage.Welcome, true)
    worker.updateReadiness(OnboardingPage.Name, false)
    runCurrent()

    val received = mutableListOf<StepReadiness>()
    val subscription = worker.observeReadiness { received.add(it) }
    runCurrent()
    assertEquals(
      listOf(StepReadiness(OnboardingPage.Welcome, true), StepReadiness(OnboardingPage.Name, false)),
      received,
      "the current value per step, first")

    worker.updateReadiness(OnboardingPage.Name, true)
    runCurrent()
    assertEquals(StepReadiness(OnboardingPage.Name, true), received.last())
    assertEquals(3, received.size)

    // The same value again is no change: nothing is delivered.
    worker.updateReadiness(OnboardingPage.Name, true)
    runCurrent()
    assertEquals(3, received.size)

    subscription.cancel()
    worker.updateReadiness(OnboardingPage.Preferences, true)
    runCurrent()
    assertEquals(3, received.size, "nothing after the subscription ends")

    tester.finish()
    assertTrue(tester.isFinished)
  }
}
