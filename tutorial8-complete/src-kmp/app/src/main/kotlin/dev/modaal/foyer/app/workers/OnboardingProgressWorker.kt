// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.app.workers

import dev.modaal.duet.shells.Working
import dev.modaal.duet.shells.untilCancelled
import dev.modaal.foyer.ports.OnboardingPage
import dev.modaal.foyer.ports.ReadinessObserving
import dev.modaal.foyer.ports.ReadinessSubscription
import dev.modaal.foyer.ports.ReadinessUpdating
import dev.modaal.foyer.ports.StepReadiness
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * The lateral seam between the onboarding steps and the progress row: one
 * sticky value per step behind a `MutableStateFlow`. The steps write through
 * `updateReadiness`, a void call; the row reads through `observeReadiness`,
 * which delivers each step's current value first and every change after,
 * on the level's scope. `run()` parks: the worker holds state and no loop,
 * and the host's cancellation ends it. No recording exists for this class;
 * `OnboardingProgressWorkerTest` pins the sticky delivery.
 */
class OnboardingProgressWorker(private val scope: CoroutineScope) :
  Working, ReadinessUpdating, ReadinessObserving {
  private val readiness = MutableStateFlow<Map<OnboardingPage, Boolean>>(emptyMap())

  override fun updateReadiness(step: OnboardingPage, ready: Boolean) {
    readiness.value = readiness.value + (step to ready)
  }

  override fun observeReadiness(onChange: (StepReadiness) -> Unit): ReadinessSubscription {
    val delivered = mutableMapOf<OnboardingPage, Boolean>()
    val job =
      scope.launch {
        readiness.collect { current ->
          for ((step, ready) in current) {
            if (delivered[step] == ready) continue
            delivered[step] = ready
            onChange(StepReadiness(step, ready))
          }
        }
      }
    return ReadinessSubscription { job.cancel() }
  }

  override suspend fun run() = untilCancelled()
}
